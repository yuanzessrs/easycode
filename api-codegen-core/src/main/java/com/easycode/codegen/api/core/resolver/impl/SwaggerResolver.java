package com.easycode.codegen.api.core.resolver.impl;

import com.easycode.codegen.api.core.constants.HandlerMethodParamTag;
import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.api.core.enums.TypeMapping;
import com.easycode.codegen.api.core.meta.ApiResolveResult;
import com.easycode.codegen.api.core.meta.Dto;
import com.easycode.codegen.api.core.meta.Dto.Field;
import com.easycode.codegen.api.core.meta.HandlerClass;
import com.easycode.codegen.api.core.meta.HandlerMethod;
import com.easycode.codegen.api.core.resolver.IResolver;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import com.easycode.codegen.api.core.util.SpringAnnotations;
import com.easycode.codegen.api.core.util.SwaggerUtils;
import com.easycode.codegen.utils.FormatUtils;
import com.easycode.codegen.utils.Methods;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.ObjectProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.easycode.codegen.api.core.util.SwaggerUtils.*;
import static com.easycode.codegen.api.core.util.SwaggerVendorExtensions.*;

/**
 * @ClassName: SwaggerMetaResolver
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-04-21 20:00
 */
@Slf4j
public class SwaggerResolver implements IResolver {

    private final ResolverContext context;

    public SwaggerResolver(ResolverContext context) {
        this.context = context;
    }

    public ApiResolveResult resolve() {
        File[] swaggerFiles = scanSwaggerFiles(context.getDefinitionFilesDirPath());
        List<ApiResolveResult> swaggerResolveResults = Arrays.stream(swaggerFiles)

//                .map(this::readFile)
//                .map(SwaggerUtils::parseSwagger)
                .map(SwaggerUtils::toSwagger)
                .map(this::resolveSwagger)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // ????????????dto
        checkDuplicatedNameController(swaggerResolveResults);
//        checkDuplicatedNameDto(swaggerResolveResults);
        ApiResolveResult result = new ApiResolveResult();
        result.setClasses(
                swaggerResolveResults.stream()
                        .flatMap(o -> o.getClasses().stream())
                        .collect(Collectors.toList())
        );
        result.setDtos(
                swaggerResolveResults.stream()
                        .flatMap(o -> o.getDtos().stream())
                        .collect(Collectors.toList())
        );
        return result;
    }

    private ApiResolveResult resolveSwagger(Swagger swagger) {
        // ?????????swagger???????????????
        if ("true".equalsIgnoreCase(getXFieldVal(swagger.getVendorExtensions(), "disabled"))) {
            // log
            return null;
        }
        // ?????????????????????dto??????
        List<Dto> dtos = parseDefinitions(swagger);
        // ???????????????controller??????
        Map<String, HandlerClass> controllerMetaMap = parseControllerMap(swagger);
        List<String> globalProduces = Optional.ofNullable(swagger.getProduces()).orElse(Collections.emptyList());
        List<String> globalConsumes = Optional.ofNullable(swagger.getConsumes()).orElse(Collections.emptyList());
        if (globalConsumes.size() > 1 || globalProduces.size() > 1) {
            throw new RuntimeException("consumes or produces only support one element.");
        }
        // request mapping ??????
        swagger.getPaths().forEach((url, path) -> {
            // ?????? ??????path?????????path?????? get post delete patch put
            log.info("??????????????????url:{}", url);
            // tag check
            path.getOperations().forEach(op -> {
                if (CollectionUtils.isEmpty(op.getTags())) {
                    throw new RuntimeException(
                            String.format("??????path???%s?????? operationId???%s?????????tags??????", url, op.getOperationId()));
                }
                if (op.getTags().size() > 1) {
                    throw new RuntimeException(
                            String.format("??????path???%s?????? operationId???%s???, tags??????????????????????????????????????????", url, op.getOperationId()));
                }
                String tag = op.getTags().get(0);
                if (!controllerMetaMap.containsKey(tag)) {
                    throw new RuntimeException(String.format("??????path???%s?????? operationId???%s???,tags???????????????????????????tag: %s;" +
                            "\ntips: tag?????????????????????????????????????????????????????? tag", url, op.getOperationId(), tag));
                }
            });
            // ??????path????????????????????????
            Map<String, Object> pathExtParams = path.getVendorExtensions();
            // ??????????????????
            path.getOperationMap().forEach((opType, op) -> {
                log.info("??????????????????url:{}, type:{}", url, opType.name());
                // ???????????????controllerMeta
                HandlerClass handlerClass = controllerMetaMap.get(op.getTags().get(0));
                Map<String, Object> extParams = op.getVendorExtensions();
                // ??????????????????
                HandlerMethod handlerMethod = new HandlerMethod();
                handlerMethod.setUrl(url);
                handlerMethod.setRequestType(opType.name());
                handlerMethod.setMethodName(op.getOperationId());
                handlerMethod.setSummary(op.getSummary());
                handlerMethod.setDescription(op.getDescription());
                handlerMethod.setConsumes(Optional.ofNullable(op.getConsumes()).orElse(globalConsumes));
                handlerMethod.setProduces(Optional.ofNullable(op.getProduces()).orElse(globalProduces));
                // check
                if (handlerMethod.getConsumes().size() > 1 || handlerMethod.getProduces().size() > 1) {
                    throw new RuntimeException("consumes or produces only support one element.");
                }

                if (handlerMethod.enableResponseBody()) {
                    handlerMethod.getControllerAnnotations().add(SpringAnnotations.ResponseBody());
                    handlerMethod.getFeignClientAnnotations().add(SpringAnnotations.ResponseBody());
                }

                handlerMethod.getControllerAnnotations().add(SpringAnnotations.RequestMapping(
                        handlerMethod.getUrl(),
                        handlerMethod.getConsumes(), handlerMethod.getProduces(),
                        handlerMethod.getRequestType()
                ));

                handlerMethod.getFeignClientAnnotations().add(SpringAnnotations.RequestMapping(
                        handlerMethod.getUrl(),
                        handlerMethod.getConsumes(), handlerMethod.getProduces(),
                        handlerMethod.getRequestType()
                ));

                // setting handlerMethod params
                handlerMethod.setHandlerMethodParams(getHandlerMethodParams(op.getOperationId(),
                        op.getParameters(), dtos, op.getVendorExtensions()));
                if (handlerMethod.enableRequestBody()) {
                    handlerMethod.getHandlerMethodParams()
                            .stream()
                            .filter(p -> p.getTag() == HandlerMethodParamTag.BODY)
                            .forEach(param -> {
                                param.getControllerAnnotations().add(SpringAnnotations.RequestBody());
                                param.getFeignClientAnnotations().add(SpringAnnotations.RequestBody());
                            });
                }
                // setting handlerMethod return def
                handlerMethod.setHandlerMethodReturn(getHandlerMethodReturn(op));
                // ??????path????????????, ???????????????????????????
                handlerMethod.getAnnotations().add(parseAnnotations(pathExtParams));
                handlerMethod.getControllerAnnotations().add(parseControllerAnnotations(pathExtParams));
                handlerMethod.getServiceAnnotations().add(parseServiceAnnotations(pathExtParams));
                handlerMethod.getFeignClientAnnotations().add(parseFeignClientAnnotations(pathExtParams));

                handlerMethod.getAnnotations().add(parseAnnotations(extParams));
                handlerMethod.getControllerAnnotations().add(parseControllerAnnotations(extParams));
                handlerMethod.getServiceAnnotations().add(parseServiceAnnotations(extParams));
                handlerMethod.getFeignClientAnnotations().add(parseFeignClientAnnotations(extParams));
                // ??????path??????import, ???????????????import
                handlerMethod.getImports().add(getImports(pathExtParams));
                handlerMethod.getImports().add(getImports(extParams));
                // ?????? handlerMethod
                handlerClass.getHandlerMethods().add(handlerMethod);
            });
        });
        ApiResolveResult resolveResult = new ApiResolveResult();
        resolveResult.setClasses(new ArrayList<>(controllerMetaMap.values()));
        resolveResult.setDtos(dtos);
        return resolveResult;
    }

    /**
     * ????????????swagger?????????dto
     *
     * @param swagger swagger????????????
     * @return ???????????????dto??????
     */
    private List<Dto> parseDefinitions(Swagger swagger) {
        return Optional.ofNullable(swagger)
                .map(Swagger::getDefinitions)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(o -> {
                    String definitionName = o.getKey();
                    // ????????? type=object??? definition
                    if (!(o.getValue() instanceof ModelImpl)
                            || !SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(((ModelImpl) o.getValue()).getType())) {
                        log.warn("definition ????????? type=object ?????????????????????????????????:{}!", definitionName);
                        return null;
                    }
                    ModelImpl modelImpl = (ModelImpl) o.getValue();
                    return toDto(definitionName, modelImpl);
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }


    private Dto toDto(String definitionName, ModelImpl modelImpl) {
        return toDto(definitionName, modelImpl.getDescription(), modelImpl.getProperties(), modelImpl.getVendorExtensions());
    }

    private Dto toDto(String definitionName, ObjectProperty objectProperty) {
        return toDto(definitionName, objectProperty.getDescription(), objectProperty.getProperties(), objectProperty.getVendorExtensions());
    }

    private Dto toDto(String definitionName, String description, Map<String, Property> properties, Map<String, Object> vendorExtensions) {
        Map<String, String> renameMap = getRenameMap(vendorExtensions);
        // ????????????
        Dto dto = new Dto();
        dto.setName(getClassNameFromDefinitionName(definitionName));
        dto.setDescription(description);
        // ????????????
        dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
        // ???????????????
        dto.getAnnotations().add(parseAnnotations(vendorExtensions));
        // ??????
        AtomicInteger index = new AtomicInteger();
        List<Field> preProcessedFields = Optional.ofNullable(properties)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(entry -> toDtoField(entry.getKey(), entry.getValue()))
                .peek(field -> field.setIndex(index.incrementAndGet()))
                .collect(Collectors.toList());

        List<Field> finalFields = preProcessedFields.stream()
                .collect(Collectors.groupingBy(Field::getName))
                .entrySet()
                .stream()
                .map(entry -> {
                    List<Field> multiFields = entry.getValue();
                    if (multiFields.size() == 1) {
                        return multiFields.get(0);
                    }
                    String fieldName = entry.getKey();
                    Field field = multiFields.stream().sorted(Field.COMPARATOR).collect(Collectors.toList())
                            .get(0);
                    List<String> aliasValues = multiFields
                            .stream()
                            .flatMap(f -> Optional.ofNullable(f.getAliasValues()).map(List::stream)
                                    .orElse(Stream.empty()))
                            .filter(f -> !ObjectUtils.isEmpty(f))
                            .filter(alias -> !fieldName.equals(alias))
                            .distinct()
                            .collect(Collectors.toList());
                    field.setAliasValues(aliasValues);
                    String aliasNamesJoinString = String.join("???", aliasValues);
                    log.info("??????DTO???????????????({})??????Java ??????????????????????????????????????????????????????????????????", aliasNamesJoinString);
                    return field;
                })
                .peek(field -> {
                    if (ObjectUtils.isEmpty(field.getAliasValues())) {
                        Optional.ofNullable(renameMap.get(field.getName())).ifPresent(alias -> {
                            field.setAliasValues(Collections.singletonList(field.getName()));
                            field.setName(alias);
                        });
                    }
                })
                .peek(field -> {
                    List<String> aliasValues = field.getAliasValues();
                    if (!ObjectUtils.isEmpty(aliasValues)) {
                        field.getAnnotations().add(AnnotationUtils.jacksonPropertyOrAlias(aliasValues));
                    }
                })
                .sorted(Field.COMPARATOR)
                .collect(Collectors.toList());

        dto.setFields(finalFields);
        return dto;
    }

    /**
     * definition property ????????? dto field
     *
     * @param fieldName ?????????
     * @param property  ????????????
     * @return dto field ??????
     */
    private Field toDtoField(String fieldName, Property property) {
        Field field = new Field();
        field.setName(fieldName);
        field.setDescription(property.getDescription());
        // ???????????? default ???,?????????????????????????????????
        field.setValue(
                Optional.ofNullable(getXDefault(property.getVendorExtensions()))
                        .orElse(getPropertyDefaultValue(property))
        );
        // rename??????????????????????????????????????????????????????????????????
        Optional.ofNullable(getRenameVal(property.getVendorExtensions()))
                .filter(val -> !ObjectUtils.isEmpty(val))
                .ifPresent(f -> {
                    String originalName = field.getName();
                    field.setName(f.trim());
                    field.setAliasValues(Collections.singletonList(originalName));
                });
        // ???????????????class???????????????  x-import: xx.xx.xx,yy.yy.yy,zz.zz.zz
        field.getImports().add(getImports(property.getVendorExtensions()));
        field.getAnnotations().add(parseAnnotations(property.getVendorExtensions()));
        field.setReadOnly(Boolean.TRUE.equals(property.getReadOnly()));
        if (property.getRequired()) {
            field.getAnnotations().add(AnnotationUtils.notNull());
        }

        if (hasXFormat(property.getVendorExtensions())) {
            String xFormat = getXFormat(property.getVendorExtensions());
            // x-Type: xx ,????????? xx
            String[] xFormatArr = xFormat.split("\\.");
            String type = xFormatArr[xFormatArr.length - 1];
            field.setType(type);
            if (xFormatArr.length > 1) {
                field.getImports().add(xFormat);
            }
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof ArrayProperty) {
            // array ?????????????????????
            Field childField = toDtoField("childField", ((ArrayProperty) property).getItems());
            field.setType(String.format("%s<%s>", List.class.getSimpleName(), childField.getType()));
            field.getImports().add(List.class.getName());
            field.getImports().add(childField.getImports().get());
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof RefProperty) {
            RefProperty refProperty = (RefProperty) property;
            String className = getClassNameFromRefPath(refProperty.getOriginalRef());
            field.setType(className);
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof ObjectProperty) {
            ObjectProperty objectProperty = (ObjectProperty) property;
            if (ObjectUtils.isEmpty(objectProperty.getProperties())) {
                // ??????????????????, ????????? java.lang.Object
                field.setType(Object.class.getSimpleName());
            } else {
                // ????????????????????????????????????
                String innerClassName = FormatUtils.snakeToUpperCamel(fieldName);
                field.setDto(toDto(innerClassName, objectProperty));
                field.setType(innerClassName);
            }
            field.getAnnotations().add(AnnotationUtils.valid());
        } else {
            TypeMapping mapping = TypeMapping.parse(property.getType(), property.getFormat());
            field.setType(mapping.getType());
            field.getImports().add(mapping.getImportValue());
        }
        return field;
    }

    /**
     * ????????????swagger???controller??????
     *
     * @param swagger swagger????????????
     * @return controller??????
     */
    private Map<String, HandlerClass> parseControllerMap(Swagger swagger) {
        return Optional.ofNullable(swagger.getTags())
                .orElse(Collections.emptyList())
                .stream().collect(Collectors.toMap(Tag::getName, tag -> {
                    HandlerClass handlerClass = new HandlerClass();
                    handlerClass.setName(SwaggerUtils.wrapControllerClassName(tag.getName()));
                    handlerClass.setServiceName(SwaggerUtils.wrapControllerServiceClassName(tag.getName()));
                    handlerClass.setFeignClientName(SwaggerUtils.wrapFeignClientClassName(tag.getName()));
                    handlerClass.setDescription(tag.getDescription());
                    handlerClass.setBasePath(swagger.getBasePath());
                    handlerClass.setHandlerMethods(new ArrayList<>(16));

                    handlerClass.getAnnotations()
                            .add(parseAnnotations(tag.getVendorExtensions()));
                    handlerClass.getControllerAnnotations()
                            .add(parseControllerAnnotations(tag.getVendorExtensions()));
                    handlerClass.getServiceAnnotations()
                            .add(parseServiceAnnotations(tag.getVendorExtensions()));
                    handlerClass.getFeignClientAnnotations()
                            .add(parseFeignClientAnnotations(tag.getVendorExtensions()));
                    handlerClass.getImports().add(getImports(tag.getVendorExtensions()));

                    handlerClass.getControllerAnnotations().add(
                            SpringAnnotations.Controller(),
                            SpringAnnotations.Validated()
                    );

                    handlerClass.getFeignClientAnnotations().add(
                            SpringAnnotations.FeignClient(context.getApplicationName()),
                            SpringAnnotations.Validated()
                    );

                    if (!ObjectUtils.isEmpty(handlerClass.getBasePath())) {
                        handlerClass.getControllerAnnotations()
                                .add(SpringAnnotations.RequestMapping(handlerClass.getBasePath()));

                        handlerClass.getFeignClientAnnotations()
                                .add(SpringAnnotations.RequestMapping(handlerClass.getBasePath()));
                    }

                    return handlerClass;
                }));
    }

    /**
     * ??????????????????
     *
     * @param opName ????????????
     * @param params ??????op??????
     * @return ??????????????????
     */
    private List<HandlerMethod.Param> getHandlerMethodParams(String opName,
                                                             List<Parameter> params,
                                                             List<Dto> dtos,
                                                             Map<String, Object> vendorExtensions) {
        // ????????????????????????path???????????????query???????????????????????????body?????????????????????
        List<Parameter> parameters = Optional.ofNullable(params).orElse(Collections.emptyList());
        Map<String, String> renameMap = getRenameMap(vendorExtensions);
        List<HandlerMethod.Param> handlerMethodParams = parameters.stream()
                .filter(o -> !(o instanceof QueryParameter))
                .map(parameter -> {
                    HandlerMethod.Param param = new HandlerMethod.Param();
                    String finalName = Methods.or(
                            getRenameVal(parameter.getVendorExtensions()),
                            renameMap.get(parameter.getName()),
                            parameter.getName()
                    );
                    param.setName(finalName);
                    param.setDescription(parameter.getDescription());
                    if (parameter.getRequired()) {
                        param.getControllerAnnotations().add(AnnotationUtils.notNull());
                        param.getFeignClientAnnotations().add(AnnotationUtils.notNull());
                    }
                    if (parameter instanceof PathParameter || parameter instanceof HeaderParameter) {
                        AbstractSerializableParameter<?> pathParameter = (AbstractSerializableParameter<?>) parameter;
                        TypeMapping mapping = TypeMapping.parse(pathParameter.getType(), pathParameter.getFormat());
                        // ????????????????????????????????????type??????
                        param.setType(mapping.getType());
                        param.getImports().add(mapping.getImportValue());
                        if (parameter instanceof PathParameter) {
                            param.setTag(HandlerMethodParamTag.PATH);
                            param.getControllerAnnotations().add(SpringAnnotations.PathVariable(parameter.getName()));
                            param.getFeignClientAnnotations().add(SpringAnnotations.PathVariable(parameter.getName()));
                        } else {
                            param.setTag(HandlerMethodParamTag.HEADER);
                            param.getControllerAnnotations().add(SpringAnnotations.HeaderVariable(parameter.getName()));
                            param.getFeignClientAnnotations().add(SpringAnnotations.HeaderVariable(parameter.getName()));
                        }
                    } else if (parameter instanceof BodyParameter) {
                        BodyParameter bodyParameter = ((BodyParameter) parameter);
                        Model model = bodyParameter.getSchema();
                        if (!(model instanceof RefModel)) {
                            throw new RuntimeException("body?????????????????????ref??????");
                        }
                        // TODO ??? DTO   arrayModel refModel modelImpl
                        String typeName = SwaggerUtils.getClassNameFromRefPath(model.getReference());
                        param.setType(typeName);
                        param.setTag(HandlerMethodParamTag.BODY);
                        param.getControllerAnnotations().add(SpringAnnotations.Valid());
                    } else {
                        throw new RuntimeException("?????????????????? query path body ????????????");
                    }
                    return param;
                }).collect(Collectors.toList());
        List<QueryParameter> queryParameters = parameters.stream()
                .filter(o -> o instanceof QueryParameter)
                .map(o -> (QueryParameter) o)
                .collect(Collectors.toList());
        // ?????? queryParams ??? QueryParamsDTO
        if (!CollectionUtils.isEmpty(queryParameters)) {
            HandlerMethod.Param handlerMethodParam = new HandlerMethod.Param();
            handlerMethodParam.setTag(HandlerMethodParamTag.QUERY);
            handlerMethodParam.setName("queryParams");
            handlerMethodParam.setDescription("query??????,????????????dto??????");
            handlerMethodParam.setType(SwaggerUtils.getClassNameFromHandlerMethodName(opName));
            // ?????????definition???????????????
            handlerMethodParams.add(handlerMethodParam);
            dtos.add(createQueryParamsDto(opName, queryParameters));
            handlerMethodParam.getControllerAnnotations().add(SpringAnnotations.Valid());
        }
        return handlerMethodParams;
    }

    private Dto createQueryParamsDto(String opName, List<QueryParameter> queryParameters) {
        Dto dto = new Dto();
        dto.setIsGetParamsDTO(Boolean.TRUE);
        // ????????????
        dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
        dto.setName(SwaggerUtils.getClassNameFromHandlerMethodName(opName));
        dto.setDescription(opName + "??????????????????");
        AtomicInteger index = new AtomicInteger();
        // ???????????????
        List<Field> preProcessedFields = queryParameters.stream().map(parameter -> {
            Field field = new Field();
            if (parameter.getRequired()) {
                field.getAnnotations().add(AnnotationUtils.notNull());
            }
            field.setIndex(index.incrementAndGet());
            field.setName(parameter.getName());
            //????????????
            Optional.ofNullable(getRenameVal(parameter.getVendorExtensions()))
                    .filter(val -> !ObjectUtils.isEmpty(val))
                    .ifPresent(javaFieldName -> {
                        String apiDefName = field.getName();
                        field.setName(javaFieldName.trim());
                        field.setAliasValues(Collections.singletonList(apiDefName));
                    });
            // ?????????
            field.setValue(Optional.ofNullable(parameter.getDefaultValue()).map(Object::toString).orElse(null));
            field.setDescription(parameter.getDescription());
            field.getAnnotations().add(parseAnnotations(parameter.getVendorExtensions()));
            field.getImports().add(getImports(parameter.getVendorExtensions()));
            if (SwaggerConstants.TYPE_ARRAY.equals(parameter.getType())) {
                if (null == parameter.getItems()) {
                    throw new RuntimeException("QueryParam array?????????????????????????????????!");
                }
                if (parameter.getItems() instanceof RefProperty) {
                    throw new RuntimeException("QueryParam ???????????? List<$ref> ");
                }
                String type = parameter.getItems().getType();
                String format = parameter.getItems().getFormat();
                TypeMapping mapping = TypeMapping.parse(type, format);
                field.setType(String.format("%s<%s>", List.class.getSimpleName(), mapping.getType()));
                Optional.ofNullable(mapping.getImportValue()).ifPresent(field.getImports()::add);
                field.getImports().add(List.class.getName());
            } else {
                TypeMapping mapping = TypeMapping.parse(parameter.getType(), parameter.getFormat());
                field.setType(mapping.getType());
                Optional.ofNullable(mapping.getImportValue()).ifPresent(field.getImports()::add);
            }
            return field;
        }).collect(Collectors.toList());
        List<Field> finalFields = preProcessedFields.stream()
                // ????????????????????? ????????????alias???field
                .collect(Collectors.groupingBy(Field::getName))
                .entrySet()
                .stream()
                .map(entry -> {
                    String fieldName = entry.getKey();
                    List<Field> multiFields = entry.getValue();
                    // ?????????
                    if (multiFields.size() == 1) {
                        return multiFields.get(0);
                    }
                    Field field = multiFields.stream().sorted(Field.COMPARATOR).collect(Collectors.toList()).get(0);
                    List<String> aliasValues = multiFields
                            .stream()
                            .flatMap(f -> Optional.ofNullable(f.getAliasValues()).map(List::stream)
                                    .orElse(Stream.empty()))
                            .filter(f -> !ObjectUtils.isEmpty(f))
                            .filter(alias -> !fieldName.equals(alias))
                            .distinct()
                            .collect(Collectors.toList());
                    field.setAliasValues(aliasValues);
                    String aliasNamesJoinString = String.join("???", aliasValues);
                    log.info("?????? Query Param ???????????????????????????({})??????Java ??????????????????????????????????????????????????????????????????", aliasNamesJoinString);
                    return field;
                })
                .peek(filed -> {
                    // alias
                    if (!ObjectUtils.isEmpty(filed.getAliasValues())) {
                        filed.getAnnotations().add(AnnotationUtils.getParamAlias(filed.getAliasValues()));
                    }
                })
                .sorted(Field.COMPARATOR)
                .collect(Collectors.toList());
        dto.setFields(finalFields);
        return dto;
    }


    /**
     * ????????????????????????(@Perfect)
     *
     * @param op ??????????????????
     * @return return??????
     */
    private HandlerMethod.Return getHandlerMethodReturn(Operation op) {
        Response resp = op.getResponses().values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("????????????????????????????????????????????????!"));
        HandlerMethod.Return handlerMethodReturn = new HandlerMethod.Return();
        handlerMethodReturn.setDescription(resp.getDescription());
        Model model = resp.getResponseSchema();
        if (null == model) {
            handlerMethodReturn.setType(void.class.getSimpleName());
        } else if (model instanceof RefModel) {
            RefModel refModel = (RefModel) model;
            String className = getClassNameFromRefPath(refModel.getOriginalRef());
            handlerMethodReturn.setType(className);
        } else if (model instanceof ArrayModel) {
            Property childProperty = ((ArrayModel) model).getItems();
            String childType;
            if (childProperty instanceof RefProperty) {
                childType = getClassNameFromRefPath(((RefProperty) childProperty).getOriginalRef());
            } else {
                TypeMapping mapping = TypeMapping.parse(childProperty.getType(), childProperty.getFormat());
                childType = mapping.getType();
                handlerMethodReturn.getImports().add(mapping.getImportValue());
            }
            handlerMethodReturn.setType(String.format("%s<%s>", List.class.getSimpleName(), childType));
            handlerMethodReturn.getImports().add(List.class.getName());
        } else if (model instanceof ModelImpl) {
            // ????????????
            ModelImpl modelImpl = (ModelImpl) model;
            Map<String, Object> extParams = modelImpl.getVendorExtensions();
            if (SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(modelImpl.getType())) {
                String xFormat = getXFormat(extParams);
                if (null == xFormat) {
                    // ???????????? x-Type,????????? java.lang.Object
                    handlerMethodReturn.setType(Object.class.getSimpleName());
                } else {
                    // x-Type: xx ,????????? xx
                    handlerMethodReturn.setType(xFormat);
                    handlerMethodReturn.getImports().add(getImports(extParams));
                }
            } else {
                TypeMapping mapping = TypeMapping.parse(modelImpl.getType(), modelImpl.getFormat());
                handlerMethodReturn.setType(mapping.getType());
                handlerMethodReturn.getImports().add(mapping.getImportValue());
            }
        } else {
            throw new RuntimeException("resp?????????????????? $ref | type | List<$ref> |List<type> ");
        }
        return handlerMethodReturn;
    }

    /**
     * ??????swagger??????
     *
     * @param swaggerApiDirPath swagger????????????
     * @return swagger files
     */
    @SneakyThrows
    private File[] scanSwaggerFiles(String swaggerApiDirPath) {
        File apiResourceDir = new File(swaggerApiDirPath);
        FileUtils.forceMkdir(apiResourceDir);
        File[] swaggerFiles = apiResourceDir.listFiles((file, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        Objects.requireNonNull(swaggerFiles, "????????????swagger????????????");
        return swaggerFiles;
    }

    /**
     * ???????????? controller
     *
     * @param resolverApiResolveResults swagger parse results
     */
    private void checkDuplicatedNameController(List<ApiResolveResult> resolverApiResolveResults) {
        // ????????????dto
        resolverApiResolveResults.stream()
                .flatMap(o -> o.getClasses().stream())
                .map(HandlerClass::getName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(sameName -> {
                    throw new RuntimeException(String.format("????????????tag :%s????????????????????????,?????????!", sameName));
                });
    }

    /**
     * ????????????dto
     *
     * @param resolverApiResolveResults swagger parse results
     */
    private void checkDuplicatedNameDto(List<ApiResolveResult> resolverApiResolveResults) {
        // ????????????dto
        resolverApiResolveResults.stream()
                .flatMap(o -> o.getDtos().stream())
                .map(Dto::getName)
                .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()))
                .entrySet()
                .stream()
                .filter(entry -> entry.getValue() > 1)
                .map(Map.Entry::getKey)
                .findFirst()
                .ifPresent(sameName -> {
                    throw new RuntimeException(String.format("????????????DTO:%s????????????????????????,?????????!", sameName));
                });
    }

    @SneakyThrows
    private String readFile(File swaggerFile) {
        return IOUtils.toString(new FileInputStream(swaggerFile), StandardCharsets.UTF_8);
    }

}
