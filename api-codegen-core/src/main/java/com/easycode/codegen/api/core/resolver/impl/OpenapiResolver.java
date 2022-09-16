package com.easycode.codegen.api.core.resolver.impl;

import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.output.HandlerClass;
import com.easycode.codegen.api.core.output.ResolveResult;
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
    public ResolveResult resolve() {
        List<ResolveResult> resolveResults = SwaggerUtils.scan(context.getDefinitionPath())
                .stream()
                .map(SwaggerUtils::toOpenAPI)
                // sort OpenAPI
                .map(openAPI -> new SingleOpenapiResolver(context, openAPI))
                .map(SingleOpenapiResolver::resolve)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        // controller重名校验
        // dto重复校验


        return ResolveResult.merge(resolveResults);
    }

    @AllArgsConstructor
    static class SingleOpenapiResolver implements IResolver {

        private final ResolverContext context;

        private final OpenAPI openAPI;

        private final Map<String, HandlerClass> controllerMetaMap = new HashMap<>();

        private final Map<String, Dto> dtoMap = new HashMap<>();

        private final ResolveResult result = new ResolveResult();

        {
            result.setClasses(new ArrayList<>());
            result.setDtos(new ArrayList<>());
        }

        @Override
        public ResolveResult resolve() {
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
//                handlerClass.getFeignClientAnnotations()
//                        .add(SpringAnnotations.FeignClient(SwaggerVendorExtensions.getFeignClientName(tag.getExtensions())));
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
                        // 仅支持 type=object的 definition
                        if (!(value instanceof ObjectSchema)
                                || !SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase((value).getType())) {
                            log.warn("definitions|components 只处理 type=object 的定义，已跳过当前定义:{}!", definitionName);
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
            // 生成定义
            Dto dto = new Dto();
            dto.setName(getClassNameFromDefinitionName(definitionName));
            dto.setDescription(modelImpl.getDescription());
            // 默认注解
            dto.getAnnotations().add(AnnotationUtils.jsonInclude(), AnnotationUtils.jsonIgnore());
            // 自定义注解
            dto.getAnnotations().add(parseAnnotations(modelImpl.getExtensions()));
            // 属性
            AtomicInteger index = new AtomicInteger();
            List<Dto.Field> preProcessedFields = Optional.ofNullable(modelImpl.getProperties())
                    .orElse(Collections.emptyMap())
                    .entrySet()
                    .stream()
                    // 转换openapi的属性为Dto.Filed
                    .map(entry -> toDtoField(entry.getKey(), entry.getValue()))
                    // 必填字段处理
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
                        String aliasNamesJoinString = String.join("、", aliasValues);
                        log.info("处理DTO，以下字段({})映射Java 字段相同，按照定义顺序，以第一个定义详情为准", aliasNamesJoinString);
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
         * definition property 转换为 dto field
         *
         * @param fieldName 字段名
         * @param property  属性实体
         * @return dto field 对象
         */
        private Dto.Field toDtoField(String fieldName, Schema<?> property) {
            Dto.Field field = new Dto.Field();
            field.setName(fieldName);
            field.setDescription(property.getDescription());
            // 默认拿了 default 值,特殊类型下面在处理格式
            field.setValue(getPropertyDefaultValue(property));

            // rename，当存在多个的时候，序列化名称会以第一个为准
            field.setAliasValues(Collections.emptyList());
            Optional.ofNullable(getRenameVal(property.getExtensions()))
                    .filter(val -> !ObjectUtils.isEmpty(val))
                    .ifPresent(alias -> {
                        String originalName = field.getName();
                        field.setName(alias.trim());
                        field.setAliasValues(Collections.singletonList(originalName));
                    });

            // 需要导入的class，需要指定  x-import: xx.xx.xx,yy.yy.yy,zz.zz.zz
            field.getImports().add(getImports(property.getExtensions()));
            field.getAnnotations().add(parseAnnotations(property.getExtensions()));
            field.setReadOnly(Boolean.TRUE.equals(property.getReadOnly()));

            if (property instanceof ArraySchema) {
                // array 暂不支持默认值
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
//                TypeMapping mapping = TypeMapping.parse(property.getType(), property.getFormat());
//                field.setType(mapping.getType());
//                field.getImports().add(mapping.getImportValue());
//
//                SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(property.getType(), property.getFormat());
//                field.setType(mapping.formatType());
//                field.getImports().add(mapping.imports());
                // todo
            }
            return field;
        }


    }

}
