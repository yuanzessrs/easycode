package com.easycode.codegen.api.core.resolver.impl;

import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.api.core.enums.TypeMapping;
import com.easycode.codegen.api.core.meta.ApiResolveResult;
import com.easycode.codegen.api.core.meta.Dto;
import com.easycode.codegen.api.core.meta.HandlerClass;
import com.easycode.codegen.api.core.resolver.IResolver;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import com.easycode.codegen.api.core.util.SpringAnnotations;
import com.easycode.codegen.api.core.util.SwaggerUtils;
import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.ObjectSchema;
import io.swagger.v3.oas.models.media.Schema;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.easycode.codegen.api.core.util.SwaggerUtils.getClassNameFromDefinitionName;
import static com.easycode.codegen.api.core.util.SwaggerUtils.getPropertyDefaultValue;
import static com.easycode.codegen.api.core.util.SwaggerVendorExtensions.*;

/**
 * @class-name: OpenapiResolver
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-07-30 18:23
 */
@Slf4j
public class OpenapiResolver implements IResolver {

    private final ResolverContext context;

    public OpenapiResolver(ResolverContext context) {
        this.context = context;
    }

    @Override
    public ApiResolveResult resolve() {
        List<ApiResolveResult> resolveResults = SwaggerUtils.scan(context.getDefinitionFilesDirPath())
                .stream()
                .map(SwaggerUtils::toOpenAPI)
                // sort OpenAPI
                .map(openAPI -> new SingleOpenapiResolver(context, openAPI))
                .map(SingleOpenapiResolver::resolve)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // controller????????????
        // dto????????????


        return ApiResolveResult.merge(resolveResults);
    }

    @AllArgsConstructor
    static class SingleOpenapiResolver implements IResolver {

        private final ResolverContext context;

        private final OpenAPI openAPI;

        private final Map<String, HandlerClass> controllerMetaMap = new HashMap<>();

        private final Map<String, Dto> dtoMap = new HashMap<>();

        private final ApiResolveResult result = new ApiResolveResult();

        {
            result.setClasses(new ArrayList<>());
            result.setDtos(new ArrayList<>());
        }

        @Override
        public ApiResolveResult resolve() {
            if (isSkip()) {
                // log
                return null;
            }
            parseControllerMap();
            parseDefinitions();


            return result;
        }

        private boolean isSkip() {
            return "true".equalsIgnoreCase(getXFieldVal(openAPI.getExtensions(), "disabled"));
        }

        private void parseControllerMap() {
            Optional.ofNullable(openAPI.getTags()).orElse(Collections.emptyList()).forEach(tag -> {
                HandlerClass handlerClass = new HandlerClass();
                // name
                handlerClass.setName(SwaggerUtils.wrapControllerClassName(tag.getName()));
                handlerClass.setServiceName(SwaggerUtils.wrapControllerServiceClassName(tag.getName()));
                handlerClass.setFeignClientName(SwaggerUtils.wrapFeignClientClassName(tag.getName()));
                // desc
                handlerClass.setDescription(tag.getDescription());
//                        handlerClass.setBasePath();

                handlerClass.setHandlerMethods(new ArrayList<>());

                handlerClass.getAnnotations().add(parseAnnotations(tag.getExtensions()));

                // extra annotation by x-@?
                handlerClass.getControllerAnnotations().add(parseControllerAnnotations(tag.getExtensions()));
                handlerClass.getFeignClientAnnotations().add(parseFeignClientAnnotations(tag.getExtensions()));
                handlerClass.getServiceAnnotations().add(parseServiceAnnotations(tag.getExtensions()));

                // extra import by x-imports
                handlerClass.getImports().add(getImports(tag.getExtensions()));

                // default annotation
                handlerClass.getAnnotations().add(SpringAnnotations.Validated());
                handlerClass.getControllerAnnotations().add(SpringAnnotations.Controller());
                handlerClass.getFeignClientAnnotations().add(SpringAnnotations.FeignClient(context.getApplicationName()));
                handlerClass.getServiceAnnotations().add(Collections.emptyList());

//                        if (!ObjectUtils.isEmpty(handlerClass.getBasePath())) {
//                            handlerClass.getControllerAnnotations()
//                                    .add(SpringAnnotations.RequestMapping(handlerClass.getBasePath()));
//
//                            handlerClass.getFeignClientAnnotations()
//                                    .add(SpringAnnotations.RequestMapping(handlerClass.getBasePath()));
//                        }

                if (controllerMetaMap.containsKey(tag.getName())) {
                    throw new RuntimeException("find duplicate tag. tag: " + tag.getName());
                }

                controllerMetaMap.put(tag.getName(), handlerClass);
            });
        }

        private void parseDefinitions() {
            Optional.ofNullable(openAPI.getComponents())
                    .map(Components::getSchemas)
                    .orElse(Collections.emptyMap())
                    .forEach((definitionName, value) -> {
                        // ????????? type=object??? definition
                        if (!(value instanceof ObjectSchema)
                                || !SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase((value).getType())) {
                            log.warn("definitions|components ????????? type=object ?????????????????????????????????:{}!", definitionName);
                        } else {
                            ObjectSchema modelImpl = (ObjectSchema) value;
                            load(definitionName, modelImpl);
                        }
                    });
        }

        private void load(String definitionName, ObjectSchema modelImpl) {
            Set<String> requiredFields = Optional.ofNullable(modelImpl.getRequired())
                    .map(HashSet::new)
                    .orElse(new HashSet<>());

            Map<String, String> renameMap = getRenameMap(modelImpl.getExtensions());
            // ????????????
            Dto dto = new Dto();
            dto.setName(getClassNameFromDefinitionName(definitionName));
            dto.setDescription(modelImpl.getDescription());
            // ????????????
            dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
            // ???????????????
            dto.getAnnotations().add(parseAnnotations(modelImpl.getExtensions()));
            // ??????
            AtomicInteger index = new AtomicInteger();
            List<Dto.Field> preProcessedFields = Optional.ofNullable(modelImpl.getProperties())
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    // ??????openapi????????????Dto.Filed
                    .map(entry -> toDtoField(entry.getKey(), entry.getValue()))
                    // ??????????????????
                    .peek(field -> {
                        if (field.getAliasValues().stream().anyMatch(requiredFields::contains)) {
                            field.getAnnotations().add(AnnotationUtils.notNull());
                        }
                    })
                    .peek(field -> field.setIndex(index.incrementAndGet()))
                    .collect(Collectors.toList());

            List<Dto.Field> finalFields = preProcessedFields.stream()
                    .collect(Collectors.groupingBy(Dto.Field::getName))
                    .entrySet()
                    .stream()
                    .map(entry -> {
                        List<Dto.Field> multiFields = entry.getValue();
                        if (multiFields.size() == 1) {
                            return multiFields.get(0);
                        }
                        String fieldName = entry.getKey();
                        Dto.Field field = multiFields.stream().sorted(Dto.Field.COMPARATOR).collect(Collectors.toList())
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
                    .sorted(Dto.Field.COMPARATOR)
                    .collect(Collectors.toList());

            dto.setFields(finalFields);

            // collect
            if (dtoMap.containsKey(definitionName)) {
                throw new RuntimeException("find duplicate definition|component: " + definitionName);
            }
            dtoMap.put(definitionName, dto);
        }

        /**
         * definition property ????????? dto field
         *
         * @param fieldName ?????????
         * @param property  ????????????
         * @return dto field ??????
         */
        private Dto.Field toDtoField(String fieldName, Schema<?> property) {
            Dto.Field field = new Dto.Field();
            field.setName(fieldName);
            field.setDescription(property.getDescription());
            // ???????????? default ???,?????????????????????????????????
            field.setValue(getPropertyDefaultValue(property));

            // rename??????????????????????????????????????????????????????????????????
            field.setAliasValues(Collections.emptyList());
            Optional.ofNullable(getRenameVal(property.getExtensions()))
                    .filter(val -> !ObjectUtils.isEmpty(val))
                    .ifPresent(alias -> {
                        String originalName = field.getName();
                        field.setName(alias.trim());
                        field.setAliasValues(Collections.singletonList(originalName));
                    });

            // ???????????????class???????????????  x-import: xx.xx.xx,yy.yy.yy,zz.zz.zz
            field.getImports().add(getImports(property.getExtensions()));
            field.getAnnotations().add(parseAnnotations(property.getExtensions()));
            field.setReadOnly(Boolean.TRUE.equals(property.getReadOnly()));

            if (property instanceof ArraySchema) {
                // array ?????????????????????
                Dto.Field childField = toDtoField("childField", ((ArraySchema) property).getItems());
                field.setType(String.format("%s<%s>", List.class.getSimpleName(), childField.getType()));
                field.getImports().add(List.class.getName());
                field.getImports().add(childField.getImports().get());
                field.getAnnotations().add(AnnotationUtils.valid());
            } else if (property instanceof ObjectSchema) {
                //

//                RefProperty refProperty = (RefProperty) property;
//                String className = getClassNameFromRefPath(refProperty.getOriginalRef());
//                field.setType(className);
//                field.getAnnotations().add(AnnotationUtils.valid());
            } else if (SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(property.getType())
                    || hasXFormat(property.getExtensions())) {
                String xFormat = getXFormat(property.getExtensions());
                if (ObjectUtils.isEmpty(xFormat)) {
                    // ???????????? x-Type,????????? java.lang.Object
                    field.setType(Object.class.getSimpleName());
                } else {
                    // x-Type: xx ,????????? xx
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


    }

}
