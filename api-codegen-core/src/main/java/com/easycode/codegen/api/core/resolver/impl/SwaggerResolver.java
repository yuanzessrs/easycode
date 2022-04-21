package com.easycode.codegen.api.core.resolver.impl;

import static com.easycode.codegen.api.core.util.SwaggerUtils.getClassNameFromDefinitionName;
import static com.easycode.codegen.api.core.util.SwaggerUtils.getClassNameFromRefPath;
import static com.easycode.codegen.api.core.util.SwaggerUtils.getPropertyDefaultValue;

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
import com.easycode.codegen.api.core.util.SwaggerVendorExtensions;
import io.swagger.models.ArrayModel;
import io.swagger.models.Model;
import io.swagger.models.ModelImpl;
import io.swagger.models.Operation;
import io.swagger.models.RefModel;
import io.swagger.models.Response;
import io.swagger.models.Swagger;
import io.swagger.models.Tag;
import io.swagger.models.parameters.BodyParameter;
import io.swagger.models.parameters.Parameter;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.parameters.QueryParameter;
import io.swagger.models.properties.ArrayProperty;
import io.swagger.models.properties.Property;
import io.swagger.models.properties.RefProperty;
import java.io.File;
import java.io.FileInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

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
                .map(this::readFile)
                .map(SwaggerUtils::parseSwagger)
                .map(this::resolveSwagger)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // 检查重名dto
        checkDuplicatedNameController(swaggerResolveResults);
        checkDuplicatedNameDto(swaggerResolveResults);
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
        // 关闭的swagger文档不处理
        if ("true".equalsIgnoreCase(SwaggerVendorExtensions.getXFieldVal(swagger.getVendorExtensions(), "disabled"))) {
            // log
            return null;
        }
        // 解析需要生成的dto对象
        List<Dto> dtos = parseDefinitions(swagger);
        // 需要生成的controller对象
        Map<String, HandlerClass> controllerMetaMap = parseControllerMap(swagger);
        List<String> globalProduces = Optional.ofNullable(swagger.getProduces()).orElse(Collections.emptyList());
        List<String> globalConsumes = Optional.ofNullable(swagger.getConsumes()).orElse(Collections.emptyList());
        if (globalConsumes.size() > 1 || globalProduces.size() > 1) {
            throw new RuntimeException("consumes or produces only support one element.");
        }
        // request mapping 解析
        swagger.getPaths().forEach((url, path) -> {
            // 处理 每个path，每个path包含 get post delete patch put
            log.info("当前正在处理url:{}", url);
            // tag check
            path.getOperations().forEach(op -> {
                if (CollectionUtils.isEmpty(op.getTags())) {
                    throw new RuntimeException(
                            String.format("当前path【%s】的 operationId【%s】没有tags属性", url, op.getOperationId()));
                }
                if (op.getTags().size() > 1) {
                    throw new RuntimeException(
                            String.format("当前path【%s】的 operationId【%s】, tags存在多个值，目前仅支持一个值", url, op.getOperationId()));
                }
                String tag = op.getTags().get(0);
                if (!controllerMetaMap.containsKey(tag)) {
                    throw new RuntimeException(String.format("当前path【%s】的 operationId【%s】,tags绑定了一个未定义的tag: %s;" +
                            "\ntips: tag不能跨文档绑定，仅支持当前文档定义的 tag", url, op.getOperationId(), tag));
                }
            });
            // 当前path的自定义扩展参数
            Map<String, Object> pathExtParams = path.getVendorExtensions();
            // 处理请求定义
            path.getOperationMap().forEach((opType, op) -> {
                log.info("当前正在处理url:{}, type:{}", url, opType.name());
                // 获取对应的controllerMeta
                HandlerClass handlerClass = controllerMetaMap.get(op.getTags().get(0));
                Map<String, Object> extParams = op.getVendorExtensions();
                // 定义当前请求
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
                handlerMethod
                        .setHandlerMethodParams(getHandlerMethodParams(op.getOperationId(), op.getParameters(), dtos));
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
                // 集成path上的注解, 当前方法的扩展注解
                handlerMethod.getAnnotations().add(SwaggerVendorExtensions.parseAnnotations(pathExtParams));
                handlerMethod.getControllerAnnotations()
                        .add(SwaggerVendorExtensions.parseControllerAnnotations(pathExtParams));
                handlerMethod.getServiceAnnotations()
                        .add(SwaggerVendorExtensions.parseServiceAnnotations(pathExtParams));
                handlerMethod.getFeignClientAnnotations()
                        .add(SwaggerVendorExtensions.parseFeignClientAnnotations(pathExtParams));

                handlerMethod.getAnnotations().add(SwaggerVendorExtensions.parseAnnotations(extParams));
                handlerMethod.getControllerAnnotations()
                        .add(SwaggerVendorExtensions.parseControllerAnnotations(extParams));
                handlerMethod.getServiceAnnotations().add(SwaggerVendorExtensions.parseServiceAnnotations(extParams));
                handlerMethod.getFeignClientAnnotations()
                        .add(SwaggerVendorExtensions.parseFeignClientAnnotations(extParams));
                // 集成path上的import, 当前方法的import
                handlerMethod.getImports().add(SwaggerVendorExtensions.getImports(pathExtParams));
                handlerMethod.getImports().add(SwaggerVendorExtensions.getImports(extParams));
                // 收集 handlerMethod
                handlerClass.getHandlerMethods().add(handlerMethod);
            });
        });
        ApiResolveResult resolveResult = new ApiResolveResult();
        resolveResult.setClasses(new ArrayList<>(controllerMetaMap.values()));
        resolveResult.setDtos(dtos);
        return resolveResult;
    }

    /**
     * 解析当前swagger定义的dto
     *
     * @param swagger swagger文档对象
     * @return 解析出来的dto定义
     */
    private List<Dto> parseDefinitions(Swagger swagger) {
        return Optional.ofNullable(swagger)
                .map(Swagger::getDefinitions)
                .orElse(Collections.emptyMap())
                .entrySet()
                .stream()
                .map(o -> {
                    String definitionName = o.getKey();
                    // 仅支持 type=object的 definition
                    if (!(o.getValue() instanceof ModelImpl)
                            || !SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(((ModelImpl) o.getValue()).getType())) {
                        log.warn("definition 只处理 type=object 的定义，已跳过当前定义:{}!", definitionName);
                        return null;
                    }
                    ModelImpl modelImpl = (ModelImpl) o.getValue();
                    Map<String, String> renameMap = SwaggerVendorExtensions
                            .getRenameMap(modelImpl.getVendorExtensions());
                    // 生成定义
                    Dto dto = new Dto();
                    dto.setName(getClassNameFromDefinitionName(definitionName));
                    dto.setDescription(modelImpl.getDescription());
                    // 默认注解
                    dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
                    // 自定义注解
                    dto.getAnnotations().add(SwaggerVendorExtensions.parseAnnotations(modelImpl.getVendorExtensions()));
                    // 属性
                    AtomicInteger index = new AtomicInteger();
                    List<Field> preProcessedFields = Optional.ofNullable(modelImpl.getProperties())
                            .orElse(Collections.emptyMap())
                            .entrySet()
                            .stream()
                            .map(entry -> propertyConvertDtoField(entry.getKey(), entry.getValue()))
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
                                String aliasNamesJoinString = String.join("、", aliasValues);
                                log.info("处理DTO，以下字段({})映射Java 字段相同，按照定义顺序，以第一个定义详情为准", aliasNamesJoinString);
                                return field;
                            })
                            .peek(field -> {
                                if (ObjectUtils.isEmpty(field.getAliasValues())) {
                                    Optional.ofNullable(renameMap.get(field.getName())).ifPresent(alias->{
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
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    /**
     * definition property 转换为 dto field
     *
     * @param fieldName 字段名
     * @param property  属性实体
     * @return dto field 对象
     */
    private Field propertyConvertDtoField(String fieldName, Property property) {
        Field field = new Field();
        field.setName(fieldName);
        field.setDescription(property.getDescription());
        // 默认拿了 default 值,特殊类型下面在处理格式
        field.setValue(
                Optional.ofNullable(SwaggerVendorExtensions.getXDefault(property.getVendorExtensions()))
                        .orElse(getPropertyDefaultValue(property))
        );
        // rename，当存在多个的时候，序列化名称会以第一个为准
        Optional.ofNullable(SwaggerVendorExtensions.getRenameVal(property.getVendorExtensions()))
                .filter(val -> !ObjectUtils.isEmpty(val))
                .ifPresent(f -> {
                    String originalName = field.getName();
                    field.setName(f.trim());
                    field.setAliasValues(Collections.singletonList(originalName));
                });
        // 需要导入的class，需要指定  x-import: xx.xx.xx,yy.yy.yy,zz.zz.zz
        field.getImports().add(SwaggerVendorExtensions.getImports(property.getVendorExtensions()));
        field.getAnnotations().add(SwaggerVendorExtensions.parseAnnotations(property.getVendorExtensions()));
        field.setReadOnly(Boolean.TRUE.equals(property.getReadOnly()));
        if (property.getRequired()) {
            field.getAnnotations().add(AnnotationUtils.notNull());
        }
        if (property instanceof ArrayProperty) {
            // array 暂不支持默认值
            Field childField = propertyConvertDtoField("childField", ((ArrayProperty) property).getItems());
            field.setType(String.format("%s<%s>", List.class.getSimpleName(), childField.getType()));
            field.getImports().add(List.class.getName());
            field.getImports().add(childField.getImports().get());
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof RefProperty) {
            RefProperty refProperty = (RefProperty) property;
            String className = getClassNameFromRefPath(refProperty.getOriginalRef());
            field.setType(className);
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(property.getType())) {
            String xFormat = SwaggerVendorExtensions.getXFormat(property.getVendorExtensions());
            if (ObjectUtils.isEmpty(xFormat)) {
                // 没有配置 x-Type,则对应 java.lang.Object
                field.setType(Object.class.getSimpleName());
            } else {
                // x-Type: xx ,则对应 xx
                String[] xFormatArr = xFormat.split("\\.");
                String type = xFormatArr[xFormatArr.length - 1];
                field.setType(type);
                if (xFormatArr.length > 1) {
                    field.getImports().add(xFormat);
                }
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
     * 获取当前swagger的controller定义
     *
     * @param swagger swagger文档对象
     * @return controller定义
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
                            .add(SwaggerVendorExtensions.parseAnnotations(tag.getVendorExtensions()));
                    handlerClass.getControllerAnnotations()
                            .add(SwaggerVendorExtensions.parseControllerAnnotations(tag.getVendorExtensions()));
                    handlerClass.getServiceAnnotations()
                            .add(SwaggerVendorExtensions.parseServiceAnnotations(tag.getVendorExtensions()));
                    handlerClass.getFeignClientAnnotations()
                            .add(SwaggerVendorExtensions.parseFeignClientAnnotations(tag.getVendorExtensions()));
                    handlerClass.getImports().add(SwaggerVendorExtensions.getImports(tag.getVendorExtensions()));

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
     * 获取方法参数
     *
     * @param opName 方法名称
     * @param params 当前op参数
     * @return 方法参数列表
     */
    private List<HandlerMethod.Param> getHandlerMethodParams(String opName, List<Parameter> params, List<Dto> dtos) {
        // 所有参数分三类，path直接存放，query参数合并生成对象，body参数也直接存放
        List<Parameter> parameters = Optional.ofNullable(params).orElse(Collections.emptyList());
        List<HandlerMethod.Param> handlerMethodParams = parameters.stream()
                .filter(o -> !(o instanceof QueryParameter))
                .map(parameter -> {
                    HandlerMethod.Param handlerMethodParam = new HandlerMethod.Param();
                    handlerMethodParam.setName(parameter.getName());
                    handlerMethodParam.setDescription(parameter.getDescription());
                    if (parameter.getRequired()) {
                        handlerMethodParam.getControllerAnnotations().add(AnnotationUtils.notNull());
                        handlerMethodParam.getFeignClientAnnotations().add(AnnotationUtils.notNull());
                    }
                    if (parameter instanceof PathParameter) {
                        PathParameter pathParameter = (PathParameter) parameter;
                        TypeMapping mapping = TypeMapping.parse(pathParameter.getType(), pathParameter.getFormat());
                        // 只支持基本类型，直接获取type就行
                        handlerMethodParam.setType(mapping.getType());
                        handlerMethodParam.getImports().add(mapping.getImportValue());
                        handlerMethodParam.setTag(HandlerMethodParamTag.PATH);
                        handlerMethodParam.getControllerAnnotations()
                                .add(SpringAnnotations.PathVariable(handlerMethodParam.getName()));
                        handlerMethodParam.getFeignClientAnnotations()
                                .add(SpringAnnotations.PathVariable(handlerMethodParam.getName()));
                    } else if (parameter instanceof BodyParameter) {
                        BodyParameter bodyParameter = ((BodyParameter) parameter);
                        Model model = bodyParameter.getSchema();
                        if (!(model instanceof RefModel)) {
                            throw new RuntimeException("body类型参数只支持ref引用");
                        }
                        // TODO 同 DTO   arrayModel refModel modelImpl
                        String typeName = SwaggerUtils.getClassNameFromRefPath(model.getReference());
                        handlerMethodParam.setType(typeName);
                        handlerMethodParam.setTag(HandlerMethodParamTag.BODY);
                        handlerMethodParam.getControllerAnnotations().add(SpringAnnotations.Valid());
                    } else {
                        throw new RuntimeException("目前只能处理 query path body 三类参数");
                    }
                    return handlerMethodParam;
                }).collect(Collectors.toList());
        List<QueryParameter> queryParameters = parameters.stream()
                .filter(o -> o instanceof QueryParameter)
                .map(o -> (QueryParameter) o)
                .collect(Collectors.toList());
        // 封装 queryParams 为 QueryParamsDTO
        if (!CollectionUtils.isEmpty(queryParameters)) {
            HandlerMethod.Param handlerMethodParam = new HandlerMethod.Param();
            handlerMethodParam.setTag(HandlerMethodParamTag.QUERY);
            handlerMethodParam.setName("queryParams");
            handlerMethodParam.setDescription("query参数,详情参考dto定义");
            handlerMethodParam.setType(SwaggerUtils.getClassNameFromHandlerMethodName(opName));
            // 追加到definition定义列表中
            handlerMethodParams.add(handlerMethodParam);
            dtos.add(createQueryParamsDto(opName, queryParameters));
            handlerMethodParam.getControllerAnnotations().add(SpringAnnotations.Valid());
        }
        return handlerMethodParams;
    }

    private Dto createQueryParamsDto(String opName, List<QueryParameter> queryParameters) {
        Dto dto = new Dto();
        dto.setIsGetParamsDTO(Boolean.TRUE);
        // 默认注解
        dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
        dto.setName(SwaggerUtils.getClassNameFromHandlerMethodName(opName));
        dto.setDescription(opName + "方法查询参数");
        AtomicInteger index = new AtomicInteger();
        // 参数预处理
        List<Field> preProcessedFields = queryParameters.stream().map(parameter -> {
            Field field = new Field();
            if (parameter.getRequired()) {
                field.getAnnotations().add(AnnotationUtils.notNull());
            }
            field.setIndex(index.incrementAndGet());
            field.setName(parameter.getName());
            //获取别名
            Optional.ofNullable(SwaggerVendorExtensions.getRenameVal(parameter.getVendorExtensions()))
                    .filter(val -> !ObjectUtils.isEmpty(val))
                    .ifPresent(javaFieldName -> {
                        String apiDefName = field.getName();
                        field.setName(javaFieldName.trim());
                        field.setAliasValues(Collections.singletonList(apiDefName));
                    });
            // 默认值
            field.setValue(Optional.ofNullable(parameter.getDefaultValue()).map(Object::toString).orElse(null));
            field.setDescription(parameter.getDescription());
            field.getAnnotations().add(SwaggerVendorExtensions.parseAnnotations(parameter.getVendorExtensions()));
            field.getImports().add(SwaggerVendorExtensions.getImports(parameter.getVendorExtensions()));
            if (SwaggerConstants.TYPE_ARRAY.equals(parameter.getType())) {
                if (null == parameter.getItems()) {
                    throw new RuntimeException("QueryParam array类型参数应该具备子类型!");
                }
                if (parameter.getItems() instanceof RefProperty) {
                    throw new RuntimeException("QueryParam 暂不支持 List<$ref> ");
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
                // 主要是为了合并 具有多个alias的field
                .collect(Collectors.groupingBy(Field::getName))
                .entrySet()
                .stream()
                .map(entry -> {
                    String fieldName = entry.getKey();
                    List<Field> multiFields = entry.getValue();
                    // 单字段
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
                    String aliasNamesJoinString = String.join("、", aliasValues);
                    log.info("针对 Query Param 进行合并，以下字段({})映射Java 字段相同，按照定义顺序，以第一个定义详情为准", aliasNamesJoinString);
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
     * 请求方法返回对象(@Perfect)
     *
     * @param op 请求方法定义
     * @return return对象
     */
    private HandlerMethod.Return getHandlerMethodReturn(Operation op) {
        Response resp = op.getResponses().values()
                .stream()
                .findFirst()
                .orElseThrow(() -> new RuntimeException("接口定义需要有一个唯一的返回声明!"));
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
            // 基础类型
            ModelImpl modelImpl = (ModelImpl) model;
            Map<String, Object> extParams = modelImpl.getVendorExtensions();
            if (SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(modelImpl.getType())) {
                String xFormat = SwaggerVendorExtensions.getXFormat(extParams);
                if (null == xFormat) {
                    // 没有配置 x-Type,则对应 java.lang.Object
                    handlerMethodReturn.setType(Object.class.getSimpleName());
                } else {
                    // x-Type: xx ,则对应 xx
                    handlerMethodReturn.setType(xFormat);
                    handlerMethodReturn.getImports().add(SwaggerVendorExtensions.getImports(extParams));
                }
            } else {
                TypeMapping mapping = TypeMapping.parse(modelImpl.getType(), modelImpl.getFormat());
                handlerMethodReturn.setType(mapping.getType());
                handlerMethodReturn.getImports().add(mapping.getImportValue());
            }
        } else {
            throw new RuntimeException("resp返回值只支持 $ref | type | List<$ref> |List<type> ");
        }
        return handlerMethodReturn;
    }

    /**
     * 扫描swagger文件
     *
     * @param swaggerApiDirPath swagger文件目录
     * @return swagger files
     */
    @SneakyThrows
    private File[] scanSwaggerFiles(String swaggerApiDirPath) {
        File apiResourceDir = new File(swaggerApiDirPath);
        FileUtils.forceMkdir(apiResourceDir);
        File[] swaggerFiles = apiResourceDir.listFiles((file, name) -> name.endsWith(".yaml") || name.endsWith(".yml"));
        Objects.requireNonNull(swaggerFiles, "没有找到swagger定义文档");
        return swaggerFiles;
    }

    /**
     * 检查重名 controller
     *
     * @param resolverApiResolveResults swagger parse results
     */
    private void checkDuplicatedNameController(List<ApiResolveResult> resolverApiResolveResults) {
        // 检查重名dto
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
                    throw new RuntimeException(String.format("存在同名tag :%s，会导致文件覆盖,请检查!", sameName));
                });
    }

    /**
     * 检查重名dto
     *
     * @param resolverApiResolveResults swagger parse results
     */
    private void checkDuplicatedNameDto(List<ApiResolveResult> resolverApiResolveResults) {
        // 检查重名dto
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
                    throw new RuntimeException(String.format("存在同名DTO:%s，会导致文件覆盖,请检查!", sameName));
                });
    }

    @SneakyThrows
    private String readFile(File swaggerFile) {
        return IOUtils.toString(new FileInputStream(swaggerFile), StandardCharsets.UTF_8);
    }

}
