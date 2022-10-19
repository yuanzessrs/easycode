package com.easycode.codegen.api.core.resolver.impl;

import com.easycode.codegen.api.core.components.SwaggerTypeConvertor;
import com.easycode.codegen.api.core.constants.HandlerMethodParamTag;
import com.easycode.codegen.api.core.constants.SwaggerConstants;
import com.easycode.codegen.api.core.input.Option;
import com.easycode.codegen.api.core.input.SwaggerOption;
import com.easycode.codegen.api.core.input.SwaggerOption.Preprocess;
import com.easycode.codegen.api.core.output.Dto;
import com.easycode.codegen.api.core.output.Dto.Field;
import com.easycode.codegen.api.core.output.HandlerClass;
import com.easycode.codegen.api.core.output.HandlerMethod;
import com.easycode.codegen.api.core.output.ResolveResult;
import com.easycode.codegen.api.core.resolver.IResolver;
import com.easycode.codegen.api.core.resolver.ResolverContext;
import com.easycode.codegen.api.core.util.AnnotationUtils;
import com.easycode.codegen.api.core.util.SpringAnnotations;
import com.easycode.codegen.api.core.util.SwaggerUtils;
import com.easycode.codegen.utils.FormatUtils;
import com.easycode.codegen.utils.Methods;
import io.swagger.models.*;
import io.swagger.models.parameters.*;
import io.swagger.models.properties.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.util.CollectionUtils;
import org.springframework.util.ObjectUtils;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
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
    private final SwaggerTypeConvertor typeConvertor = new SwaggerTypeConvertor();

    public SwaggerResolver(ResolverContext context) {
        this.context = context;
        this.init();
    }

    void init() {
        Optional.ofNullable(context.getSwaggerOption()).map(SwaggerOption::getTypeMappings).ifPresent(typeMappings -> {
            typeMappings.forEach(typeMapping -> {
                typeConvertor.register(
                        SwaggerTypeConvertor.Mapping.builder()
                                .type(typeMapping.getSwaggerType())
                                .format(typeMapping.getSwaggerFormat())
                                .javaType(SwaggerTypeConvertor.JavaType.builder()
                                        .type(typeMapping.getJavaType())
                                        .subtype(typeMapping.getJavaSubtype())
                                        .imports(Optional.ofNullable(typeMapping.getImports()).orElse(Collections.emptyList()))
                                        .build())
                                .build()
                );
            });
        });

    }

    public ResolveResult resolve() {
        return ResolveResult.merge(
                Stream.concat(
                                SwaggerUtils.scanModelsByPath(context.getDefinitionPath()).stream(),
                                SwaggerUtils.scanModelsByURLs(context.getDefinitionUrls()).stream()
                        )
                        .map(this::preprocessModel)
                        .map(this::resolve)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList())
        );
    }

    public Swagger preprocessModel(Swagger swagger) {
        Optional.ofNullable(context.getSwaggerOption()).map(SwaggerOption::getPreprocess).ifPresent(preprocess -> {
            preprocessCommon(swagger, preprocess);
            Optional.ofNullable(preprocess.getTag()).ifPresent(tag -> preprocessTag(swagger, tag));
            Optional.ofNullable(preprocess.getOperation()).ifPresent(val -> preprocessOperation(swagger, val));
            Optional.ofNullable(preprocess.getRef()).ifPresent(ref -> preprocessRef(swagger, ref));
            Optional.ofNullable(preprocess.getDefinition()).ifPresent(dto -> preprocessDefinition(swagger, dto));
        });
        return swagger;
    }

    public void preprocessCommon(Swagger swagger, Preprocess option) {
        Optional.ofNullable(option).map(Preprocess::getConsumesAndProducesFilter).ifPresent(consumeProduceFilter -> {
            Set<String> patterns = Optional.ofNullable(consumeProduceFilter.getPatterns()).orElse(Collections.emptySet());
            Predicate<String> removeIfFunction = contentType -> consumeProduceFilter.enabledRegex()
                    ? patterns.stream().anyMatch(pattern -> Pattern.matches(pattern, contentType))
                    : patterns.contains(contentType);
            Optional.ofNullable(swagger.getConsumes()).ifPresent(vals -> vals.removeIf(removeIfFunction));
            Optional.ofNullable(swagger.getProduces()).ifPresent(vals -> vals.removeIf(removeIfFunction));

            swagger.getPaths().forEach(((url, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {
                    Optional.ofNullable(operation.getConsumes()).ifPresent(vals -> vals.removeIf(removeIfFunction));
                    Optional.ofNullable(operation.getProduces()).ifPresent(vals -> vals.removeIf(removeIfFunction));

                }));
            }));
        });

    }

    public void preprocessTag(Swagger swagger, Preprocess.TagOption tagOption) {
        if (tagOption.hashFilter()) {
            if (tagOption.getIncludeFilter() != null) {
                Set<String> nameFilter = Optional.ofNullable(tagOption.getIncludeFilter().getByNames()).orElse(Collections.emptySet());
                swagger.getTags().removeIf(currentTag -> !nameFilter.contains(currentTag.getName()));

                swagger.getPaths().forEach((url, path) -> {
                    path.getOperationMap()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().getTags().stream().noneMatch(nameFilter::contains))
                            .map(Map.Entry::getKey)
                            .forEach(httpMethod -> path.set(httpMethod.name().toLowerCase(), null));
                });

//                swagger.getPaths().forEach((url, path) -> {
//                    path.getOperationMap().entrySet().removeIf(entry -> entry.getValue().getTags().stream().noneMatch(nameFilter::contains));
//                });

//                swagger.getPaths().entrySet().removeIf(entry -> entry.getValue().getOperationMap().isEmpty());


            } else if (tagOption.getExcludeFilter() != null) {
                Set<String> nameFilter = Optional.ofNullable(tagOption.getExcludeFilter().getByNames()).orElse(Collections.emptySet());
                swagger.getTags().removeIf(currentTag -> nameFilter.contains(currentTag.getName()));
                swagger.getPaths().forEach((url, path) -> {
                    path.getOperationMap()
                            .entrySet()
                            .stream()
                            .filter(entry -> entry.getValue().getTags().stream().anyMatch(nameFilter::contains))
                            .map(Map.Entry::getKey)
                            .forEach(httpMethod -> path.set(httpMethod.name().toLowerCase(), null));
                });
            }
        }

        Optional.ofNullable(tagOption.getAppendVendorExtensions()).ifPresent(appendVendorExtensions -> {
            swagger.getTags().forEach(tag -> {
                appendVendorExtensions.stream().filter(rule -> {
                    return rule.enabledRegex() ? Pattern.matches(rule.getTagNamePattern(), tag.getName()) : tag.getName().equals(rule.getTagNamePattern());
                }).findFirst().ifPresent(rule -> {
                    tag.getVendorExtensions().putAll(rule.getVendorExtensions());
                });
            });
        });

        // tag rename
        Optional.ofNullable(tagOption.getRenames()).ifPresent(renames -> {
            Map<String, String> renameTagMap = new HashMap<>();
            swagger.getTags().forEach(tag -> {
                String originalTagName = tag.getName();
                Optional<Preprocess.TagOption.TagRename> tagRenameOptional = renames.stream().filter(rule -> {
                    if (Boolean.TRUE.equals(rule.getEnabledRegex())) {
                        return Pattern.matches(rule.getSourceName(), originalTagName);
                    }
                    return originalTagName.equals(rule.getSourceName());
                }).findFirst();
                tagRenameOptional.ifPresent(tagRename -> {
                    String finalTagName = tagRename.getTargetName();
                    if (Boolean.TRUE.equals(tagRename.getEnabledRegex())) {
                        Matcher matcher = Pattern.compile(tagRename.getSourceName()).matcher(originalTagName);
                        matcher.matches();
                        for (int index = 1; index <= matcher.groupCount(); index++) {
                            finalTagName = finalTagName.replaceAll("\\{" + index + "}", matcher.group(index));
                        }
                    }
                    if (Boolean.TRUE.equals(tagRename.getUpgradeToUpperCamel())) {
                        finalTagName = FormatUtils.toUpperCamel(finalTagName);
                    }
                    tag.setName(finalTagName);
                    renameTagMap.put(originalTagName, finalTagName);
                });
            });
            swagger.getPaths().forEach(((s, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {
                    List<String> targetTagNames = operation.getTags()
                            .stream()
                            .map(tagName -> renameTagMap.getOrDefault(tagName, tagName))
                            .collect(Collectors.toList());
                    operation.setTags(targetTagNames);
                }));
            }));
        });


    }

    public void preprocessOperation(Swagger swagger, Preprocess.OperationOption operationOption) {
        if (operationOption.hashFilter()) {
            if (operationOption.getIncludeFilter() != null) {
                Set<String> idFilter = Optional.ofNullable(operationOption.getIncludeFilter().getByIds()).orElse(Collections.emptySet());
                Set<String> urlFilter = Optional.ofNullable(operationOption.getIncludeFilter().getByUrls()).orElse(Collections.emptySet());
                swagger.getPaths().entrySet().removeIf(pathEntry -> !urlFilter.contains(pathEntry.getKey()));
                swagger.getPaths().forEach(((url, path) -> {
                    path.getOperationMap().entrySet().removeIf(operationEntry -> !idFilter.contains(operationEntry.getValue().getOperationId()));
                }));
            } else if (operationOption.getExcludeFilter() != null) {
                Set<String> idFilter = Optional.ofNullable(operationOption.getExcludeFilter().getByIds()).orElse(Collections.emptySet());
                Set<String> urlFilter = Optional.ofNullable(operationOption.getExcludeFilter().getByUrls()).orElse(Collections.emptySet());
                swagger.getPaths().entrySet().removeIf(pathEntry -> urlFilter.contains(pathEntry.getKey()));
                swagger.getPaths().forEach(((url, path) -> {
                    path.getOperationMap().entrySet().removeIf(entry -> idFilter.contains(entry.getValue().getOperationId()));
                }));
            }
        }

        Optional.ofNullable(operationOption.getRequiredQueryParams()).ifPresent(requiredQueryParams -> {
            swagger.getPaths().forEach(((url, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {
                    Set<String> paramNames = requiredQueryParams.stream().filter(rule -> {
                        boolean methodMatch = httpMethod.name().equalsIgnoreCase(rule.getHttpMethod());
                        // method filter
                        if (!methodMatch) {
                            return false;
                        }
                        if (Boolean.TRUE.equals(rule.getEnabledRegex())) {
                            return Pattern.matches(rule.getUrl(), url);
                        }
                        return url.equals(rule.getUrl());
                    }).flatMap(rule -> rule.getParamNames().stream()).collect(Collectors.toSet());
                    operation.getParameters().forEach(parameter -> {
                        if (paramNames.contains(parameter.getName())) {
                            parameter.setRequired(true);
                        }
                    });
                }));
            }));
        });

        Optional.ofNullable(operationOption.getConsumeRewrites()).ifPresent(consumeRewrites -> {
            swagger.getPaths().forEach(((url, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {
                    consumeRewrites.stream().filter(consumeRewrite -> {
                        boolean methodMatch = httpMethod.name().equalsIgnoreCase(consumeRewrite.getHttpMethod());
                        // method filter
                        if (!methodMatch) {
                            return false;
                        }
                        if (Boolean.TRUE.equals(consumeRewrite.getEnabledRegex())) {
                            return Pattern.matches(consumeRewrite.getUrl(), url);
                        }
                        return url.equals(consumeRewrite.getUrl());
                    }).findFirst().ifPresent(consumeRewrite -> {
                        operation.setConsumes(Collections.singletonList(consumeRewrite.getConsume()));
                        if (Boolean.TRUE.equals(consumeRewrite.getClearFlag())) {
                            operation.setConsumes(Collections.emptyList());
                        }
                    });
                }));
            }));
        });

        Optional.ofNullable(operationOption.getIdRewrites()).ifPresent(idRewrites -> {
            swagger.getPaths().forEach(((s, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {
                    idRewrites.stream().filter(idRewrite -> {
                        boolean limitedTagPass = Optional.ofNullable(idRewrite.getLimitedTag())
                                .map(limitedTag -> idRewrite.getEnabledRegex() ? operation.getTags().stream().anyMatch(tag -> Pattern.matches(limitedTag, tag)) : operation.getTags().contains(limitedTag))
                                .orElse(true);
                        // tag过滤
                        if (!limitedTagPass) {
                            return false;
                        }
                        if (Boolean.TRUE.equals(idRewrite.getEnabledRegex())) {
                            return Pattern.matches(idRewrite.getOriginalId(), operation.getOperationId());
                        }
                        return operation.getOperationId().equals(idRewrite.getOriginalId());
                    }).findFirst().ifPresent(idRewrite -> {
                        String targetId = idRewrite.getTargetId();
                        if (Boolean.TRUE.equals(idRewrite.getEnabledRegex())) {
                            Pattern pattern = Pattern.compile(idRewrite.getOriginalId());
                            Matcher matcher = pattern.matcher(operation.getOperationId());
                            matcher.matches();
                            for (int index = 1; index <= matcher.groupCount(); index++) {
                                targetId = targetId.replaceAll("\\{" + index + "}", matcher.group(index));
                            }
                        }
                        operation.setOperationId(targetId);
                    });
                }));
            }));
        });


    }

    public void preprocessRef(Swagger swagger, Preprocess.RefOption ref) {
        Optional.ofNullable(ref.getRewrites()).ifPresent(refRewrites -> {
            swagger.getDefinitions().forEach(((name, model) -> {
                Optional.ofNullable(model.getProperties()).orElse(Collections.emptyMap()).forEach(((propertyName, property) -> {
                    Optional.ofNullable(property)
                            .map(p -> {
                                if (property instanceof ArrayProperty) {
                                    ArrayProperty arrayProperty = (ArrayProperty) property;
                                    if (arrayProperty.getItems() instanceof RefProperty) {
                                        return (RefProperty) arrayProperty.getItems();
                                    }
                                }
                                if (property instanceof RefProperty) {
                                    return (RefProperty) property;
                                }
                                return null;
                            })
                            .ifPresent(refProperty -> {
                                refRewrites.stream().filter(refRewrite -> {
                                    if (Boolean.TRUE.equals(refRewrite.getEnabledRegex())) {
                                        return Pattern.matches(refRewrite.getOriginalRef(), refProperty.getOriginalRef());
                                    }
                                    return refProperty.getOriginalRef().equals(refRewrite.getOriginalRef());
                                }).findFirst().ifPresent(refRewrite -> {
                                    String targetRef = refRewrite.getTargetRef();
                                    if (Boolean.TRUE.equals(refRewrite.getEnabledRegex())) {
                                        Pattern pattern = Pattern.compile(refRewrite.getOriginalRef());
                                        Matcher matcher = pattern.matcher(refProperty.getOriginalRef());
                                        matcher.matches();
                                        for (int index = 1; index <= matcher.groupCount(); index++) {
                                            targetRef = targetRef.replaceAll("\\{" + index + "}", matcher.group(index));
                                        }
                                    }
                                    refProperty.set$ref(targetRef);
                                    Optional.ofNullable(refRewrite.getImports())
                                            .ifPresent(imports -> model.getVendorExtensions().put(X_IMPORT, imports));
                                });
                            });
                }));
            }));
            swagger.getPaths().forEach(((s, path) -> {
                path.getOperationMap().forEach(((httpMethod, operation) -> {

                    // resp
                    operation.getResponses().forEach(((status, response) -> {
                        if (response.getResponseSchema() instanceof RefModel) {
                            RefModel refModel = (RefModel) response.getResponseSchema();
                            refRewrites.stream().filter(refRewrite -> {
                                if (Boolean.TRUE.equals(refRewrite.getEnabledRegex())) {
                                    return Pattern.matches(refRewrite.getOriginalRef(), refModel.getOriginalRef());
                                }
                                return refModel.getOriginalRef().equals(refRewrite.getOriginalRef());
                            }).findFirst().ifPresent(refRewrite -> {
                                String targetRef = refRewrite.getTargetRef();
                                if (Boolean.TRUE.equals(refRewrite.getEnabledRegex())) {
                                    Pattern pattern = Pattern.compile(refRewrite.getOriginalRef());
                                    Matcher matcher = pattern.matcher(refModel.getOriginalRef());
                                    matcher.matches();
                                    for (int index = 1; index <= matcher.groupCount(); index++) {
                                        targetRef = targetRef.replaceAll("\\{" + index + "}", matcher.group(index));
                                    }
                                }
                                refModel.set$ref(targetRef);
                                Optional.ofNullable(refRewrite.getImports())
                                        .ifPresent(imports -> operation.setVendorExtension(X_IMPORT, imports));
                            });
                        }
                    }));

                }));
            }));
        });
    }

    public void preprocessDefinition(Swagger swagger, Preprocess.DefinitionOption definition) {

        if (definition.hashFilter()) {
            if (definition.getIncludeFilter() != null) {
                Preprocess.DefinitionOption.Filter includeFilter = definition.getIncludeFilter();
                Set<String> namePatterns = Optional.ofNullable(includeFilter.getByNames()).orElse(Collections.emptySet());
                List<String> toExcludeDefinitionNames = swagger.getDefinitions().keySet().stream().filter(definitionName -> {
                    if (namePatterns.contains(definitionName)) {
                        return false;
                    }
                    return namePatterns.stream().noneMatch(namePattern -> Pattern.matches(namePattern, definitionName));
                }).collect(Collectors.toList());
                toExcludeDefinitionNames.forEach(toExcludeDefinitionName -> swagger.getDefinitions().remove(toExcludeDefinitionName));
            } else if (definition.getExcludeFilter() != null) {
                Preprocess.DefinitionOption.Filter excludeFilter = definition.getExcludeFilter();
                Set<String> namePatterns = Optional.ofNullable(excludeFilter.getByNames()).orElse(Collections.emptySet());
                List<String> toExcludeDefinitionNames = swagger.getDefinitions().keySet().stream().filter(definitionName -> {
                    if (namePatterns.contains(definitionName)) {
                        return true;
                    }
                    return namePatterns.stream().anyMatch(namePattern -> Pattern.matches(namePattern, definitionName));
                }).collect(Collectors.toList());
                toExcludeDefinitionNames.forEach(toExcludeDefinitionName -> swagger.getDefinitions().remove(toExcludeDefinitionName));
            }
        }

    }

    private ResolveResult resolve(Swagger swagger) {
        // 关闭的swagger文档不处理
        if (isIgnore(swagger.getVendorExtensions())) {
            // log
            return null;
        }
        if ("/".equals(swagger.getBasePath())) {
            swagger.setBasePath(null);
        }

        List<Dto> dtos = parseDtos(swagger);
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
                handlerMethod.setHandlerMethodParams(getHandlerMethodParams(op.getOperationId(), op.getParameters(), dtos, extParams));

                // setting handlerMethod return def
                handlerMethod.setHandlerMethodReturn(getHandlerMethodReturn(op));

                // 集成path上的注解, 当前方法的扩展注解
                handlerMethod.getAnnotations().add(parseAnnotations(pathExtParams));
                handlerMethod.getControllerAnnotations().add(parseControllerAnnotations(pathExtParams));
                handlerMethod.getServiceAnnotations().add(parseServiceAnnotations(pathExtParams));
                handlerMethod.getFeignClientAnnotations().add(parseFeignClientAnnotations(pathExtParams));

                handlerMethod.getAnnotations().add(parseAnnotations(extParams));
                handlerMethod.getControllerAnnotations().add(parseControllerAnnotations(extParams));
                handlerMethod.getServiceAnnotations().add(parseServiceAnnotations(extParams));
                handlerMethod.getFeignClientAnnotations().add(parseFeignClientAnnotations(extParams));

                // 集成path上的import, 当前方法的import
                handlerMethod.getImports().add(getImports(pathExtParams));
                handlerMethod.getImports().add(getImports(extParams));

                if (handlerMethod.enableRequestBody()) {
                    handlerMethod.getHandlerMethodParams()
                            .stream()
                            .filter(p -> p.getTag() == HandlerMethodParamTag.BODY)
                            .forEach(param -> {
                                param.getControllerAnnotations().add(SpringAnnotations.RequestBody());
                                param.getFeignClientAnnotations().add(SpringAnnotations.RequestBody());
                            });
                }

                if (handlerMethod.enableResponseBody()) {
                    handlerMethod.getControllerAnnotations().add(SpringAnnotations.ResponseBody());
                    handlerMethod.getFeignClientAnnotations().add(SpringAnnotations.ResponseBody());
                }

                handlerClass.getHandlerMethods().add(handlerMethod);
            });
        });
        ResolveResult resolveResult = new ResolveResult();
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
    private List<Dto> parseDtos(Swagger swagger) {
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
                    return toDto(definitionName, (ModelImpl) o.getValue());
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
        if (isIgnore(vendorExtensions)) {
            return null;
        }
        Map<String, String> renameMap = getRenameMap(vendorExtensions);
        // 生成定义
        Dto dto = new Dto();
        dto.setName(getClassNameFromDefinitionName(definitionName));
        dto.setDescription(description);
        // 自定义注解
        dto.getAnnotations().add(parseAnnotations(vendorExtensions));
        dto.getImports().add(getImports(vendorExtensions));
        // 属性
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
                    Field field = multiFields.stream().sorted(Field.COMPARATOR).collect(Collectors.toList()).get(0);
                    List<String> aliasValues = multiFields
                            .stream()
                            .flatMap(f -> Optional.ofNullable(f.getAliasValues()).map(List::stream).orElse(Stream.empty()))
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
                .sorted(Field.COMPARATOR)
                .collect(Collectors.toList());

        dto.setFields(finalFields);
        return dto;
    }

    /**
     * definition property 转换为 dto field
     *
     * @param fieldName 字段名
     * @param property  属性实体
     * @return dto field 对象
     */
    private Field toDtoField(String fieldName, Property property) {
        Field field = new Field();
        field.setName(fieldName);
        field.setDescription(property.getDescription());
        // 默认拿了 default 值,特殊类型下面在处理格式
        field.setValue(
                Optional.ofNullable(getXDefault(property.getVendorExtensions()))
                        .orElse(getPropertyDefaultValue(property))
        );
        // rename，当存在多个的时候，序列化名称会以第一个为准
        Optional.ofNullable(getRenameVal(property.getVendorExtensions()))
                .filter(val -> !ObjectUtils.isEmpty(val))
                .ifPresent(f -> {
                    String originalName = field.getName();
                    field.setName(f.trim());
                    field.setAliasValues(Collections.singletonList(originalName));
                });
        // 需要导入的class，需要指定  x-import: xx.xx.xx,yy.yy.yy,zz.zz.zz
        field.getImports().add(getImports(property.getVendorExtensions()));
        field.getAnnotations().add(parseAnnotations(property.getVendorExtensions()));
        field.setReadOnly(Boolean.TRUE.equals(property.getReadOnly()));
        if (property.getRequired()) {
            field.getAnnotations().add(AnnotationUtils.notNull());
        }

        if (hasXFormat(property.getVendorExtensions())) {
            String xFormat = getXFormat(property.getVendorExtensions());
            // x-Type: xx ,则对应 xx
            String[] xFormatArr = xFormat.split("\\.");
            String type = xFormatArr[xFormatArr.length - 1];
            field.setType(type);
            if (xFormatArr.length > 1) {
                field.getImports().add(xFormat);
            }
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof ArrayProperty) {
            // array 暂不支持默认值
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
                // 没有设置属性, 则对应 java.lang.Object
                field.setType(Object.class.getSimpleName());
            } else {
                // 有属性，则需要生成类中类
                String innerClassName = FormatUtils.snakeToUpperCamel(fieldName);
                field.setDto(toDto(innerClassName, objectProperty));
                field.setType(innerClassName);
            }
            field.getAnnotations().add(AnnotationUtils.valid());
        } else if (property instanceof MapProperty) {
            field.setType(Object.class.getSimpleName());
            field.getAnnotations().add(AnnotationUtils.valid());
        } else {
            SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(property.getType(), property.getFormat());
            field.setType(mapping.formatType());
            field.getImports().add(mapping.imports());
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

                    handlerClass.getAnnotations().add(parseAnnotations(tag.getVendorExtensions()));
                    handlerClass.getControllerAnnotations().add(parseControllerAnnotations(tag.getVendorExtensions()));
                    handlerClass.getServiceAnnotations().add(parseServiceAnnotations(tag.getVendorExtensions()));
                    handlerClass.getFeignClientAnnotations().add(parseFeignClientAnnotations(tag.getVendorExtensions()));
                    handlerClass.getImports().add(getImports(tag.getVendorExtensions()));

                    handlerClass.getControllerAnnotations().add(
                            SpringAnnotations.Controller(),
                            SpringAnnotations.Validated()
                    );

                    handlerClass.getFeignClientAnnotations().add(
                            SpringAnnotations.FeignClient(
                                    getFeignClientName(tag.getVendorExtensions()),
                                    Optional.ofNullable(getFeignClientContextId(tag.getVendorExtensions())).orElse(tag.getName()),
                                    getFeignClientPath(tag.getVendorExtensions()),
                                    disableFeignClientPathQuotes(tag.getVendorExtensions())
                            ),
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
    private List<HandlerMethod.Param> getHandlerMethodParams(String opName,
                                                             List<Parameter> params,
                                                             List<Dto> dtos,
                                                             Map<String, Object> vendorExtensions) {
        // 所有参数分三类，path直接存放，query参数合并生成对象，body参数也直接存放
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
                        SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(pathParameter.getType(), pathParameter.getFormat());
                        param.setType(mapping.formatType());
                        param.getImports().add(mapping.imports());

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
                        if (model instanceof RefModel) {
                            // TODO 同 DTO   arrayModel refModel modelImpl
                            String typeName = SwaggerUtils.getClassNameFromRefPath(model.getReference());
                            param.setType(typeName);
                            param.setTag(HandlerMethodParamTag.BODY);
                            param.getControllerAnnotations().add(SpringAnnotations.Valid());
                        } else if (model instanceof ArrayModel && ((ArrayModel) model).getItems() instanceof RefProperty) {
                            RefProperty refProperty = ((RefProperty) ((ArrayModel) model).getItems());
                            // TODO 同 DTO   arrayModel refModel modelImpl
                            String typeName = SwaggerUtils.getClassNameFromRefPath(refProperty.getOriginalRef());
                            param.setType(String.format("%s<%s>", List.class.getSimpleName(), typeName));
                            param.setTag(HandlerMethodParamTag.BODY);
                            param.getControllerAnnotations().add(SpringAnnotations.Valid());
                            param.getImports().add(List.class.getName());
                        } else if (model instanceof ModelImpl) {
                            ModelImpl modelImpl = (ModelImpl) model;
                            SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(modelImpl.getType(), modelImpl.getFormat());
                            param.setType(mapping.formatType());
                            param.getImports().add(mapping.imports());
                        } else {
                            throw new RuntimeException("不支持的body类型参数");
                        }
                    } else {
                        throw new RuntimeException("目前只能处理 query path body 三类参数");
                    }
                    return param;
                }).collect(Collectors.toList());
        List<QueryParameter> queryParameters = parameters.stream()
                .filter(o -> o instanceof QueryParameter)
                .map(o -> (QueryParameter) o)
                .collect(Collectors.toList());
        // 封装 queryParams 为 QueryParamsDTO
        if (!CollectionUtils.isEmpty(queryParameters)) {
            boolean disabledMergeQueryParam = Optional.ofNullable(context.getOptions())
                    .map(Option::getDisabledMergeQueryParam)
                    .orElse(false);
            disabledMergeQueryParam = getOptionalDisabledMergeQueryParam(vendorExtensions).orElse(disabledMergeQueryParam);
            if (disabledMergeQueryParam) {
                queryParameters.stream().map(parameter -> {
                    HandlerMethod.Param param = new HandlerMethod.Param();
                    param.setName(parameter.getName());
                    param.setDescription(parameter.getDescription());
                    param.getAnnotations().add(parseAnnotations(parameter.getVendorExtensions()));
                    param.getImports().add(getImports(parameter.getVendorExtensions()));

                    String paramName = param.getName();
                    String defaultValue = Optional.ofNullable(parameter.getDefaultValue()).map(Object::toString).orElse(null);
                    getOptionalRename(parameter.getVendorExtensions()).ifPresent(param::setName);
                    param.getControllerAnnotations().add(AnnotationUtils.RequestParam(paramName, defaultValue, parameter.getRequired()));
                    param.getFeignClientAnnotations().add(AnnotationUtils.RequestParam(paramName, defaultValue, parameter.getRequired()));

                    if (SwaggerConstants.TYPE_ARRAY.equals(parameter.getType())) {
                        if (null == parameter.getItems()) {
                            throw new RuntimeException("QueryParam array类型参数应该具备子类型!");
                        }
                        if (parameter.getItems() instanceof RefProperty) {
                            throw new RuntimeException("QueryParam 暂不支持 List<$ref> ");
                        }
                        String type = parameter.getItems().getType();
                        String format = parameter.getItems().getFormat();

                        SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(type, format);
                        param.setType(String.format("%s<%s>", List.class.getSimpleName(), mapping.formatType()));
                        param.getImports().add(mapping.imports());

                        param.getImports().add(List.class.getName());
                    } else if (ObjectUtils.isEmpty(parameter.getType())) {
                        param.setType(Object.class.getSimpleName());
                    } else {
                        SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(parameter.getType(), parameter.getFormat());
                        param.setType(mapping.formatType());
                        param.getImports().add(mapping.imports());
                    }
                    return param;
                }).forEach(handlerMethodParams::add);

            } else {
                HandlerMethod.Param handlerMethodParam = new HandlerMethod.Param();
                handlerMethodParam.setTag(HandlerMethodParamTag.QUERY);
                handlerMethodParam.setName("queryParams");
                handlerMethodParam.setDescription("query参数,详情参考dto定义");
                handlerMethodParam.setType(SwaggerUtils.getClassNameFromHandlerMethodName(opName));
                handlerMethodParam.getControllerAnnotations().add(SpringAnnotations.Valid());
                // 追加到definition定义列表中
                handlerMethodParams.add(handlerMethodParam);
                dtos.add(createQueryParamsDto(opName, queryParameters));
            }
        }


        return handlerMethodParams;
    }

    private Dto createQueryParamsDto(String opName, List<QueryParameter> queryParameters) {
        Dto dto = new Dto();
        dto.setIsGetParamsDTO(Boolean.TRUE);
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
            // 默认值
            field.setValue(Optional.ofNullable(parameter.getDefaultValue()).map(Object::toString).orElse(null));
            field.setDescription(parameter.getDescription());
            field.getAnnotations().add(parseAnnotations(parameter.getVendorExtensions()));
            field.getImports().add(getImports(parameter.getVendorExtensions()));
            //获取别名
            getOptionalRename(parameter.getVendorExtensions()).ifPresent(javaFieldName -> {
                String apiDefName = field.getName();
                field.setName(javaFieldName.trim());
                field.setAliasValues(Collections.singletonList(apiDefName));
            });
            if (SwaggerConstants.TYPE_ARRAY.equals(parameter.getType())) {
                if (null == parameter.getItems()) {
                    throw new RuntimeException("QueryParam array类型参数应该具备子类型!");
                }
                if (parameter.getItems() instanceof RefProperty) {
                    throw new RuntimeException("QueryParam 暂不支持 List<$ref> ");
                }
                String type = parameter.getItems().getType();
                String format = parameter.getItems().getFormat();

                SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(type, format);
                field.setType(String.format("%s<%s>", List.class.getSimpleName(), mapping.formatType()));
                field.getImports().add(mapping.imports());

                field.getImports().add(List.class.getName());
            } else if (ObjectUtils.isEmpty(parameter.getType())) {
                field.setType(Object.class.getSimpleName());
            } else {
                SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(parameter.getType(), parameter.getFormat());
                field.setType(mapping.formatType());
                field.getImports().add(mapping.imports());
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
        Response resp = Optional.ofNullable(op.getResponses())
                .map(responses -> responses.get("200"))
                .orElseThrow(() -> new RuntimeException("接口定义需要有一个200的返回声明!"));
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
                SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(childProperty.getType(), childProperty.getFormat());
                childType = mapping.formatType();
                handlerMethodReturn.getImports().add(mapping.imports());
            }
            handlerMethodReturn.setType(String.format("%s<%s>", List.class.getSimpleName(), childType));
            handlerMethodReturn.getImports().add(List.class.getName());
        } else if (model instanceof ModelImpl) {
            // 基础类型
            ModelImpl modelImpl = (ModelImpl) model;
            Map<String, Object> extParams = modelImpl.getVendorExtensions();
            if (SwaggerConstants.TYPE_OBJECT.equalsIgnoreCase(modelImpl.getType())) {
                String xFormat = getXFormat(extParams);
                if (null == xFormat) {
                    // 没有配置 x-Type,则对应 java.lang.Object
                    handlerMethodReturn.setType(Object.class.getSimpleName());
                } else {
                    // x-Type: xx ,则对应 xx
                    handlerMethodReturn.setType(xFormat);
                    handlerMethodReturn.getImports().add(getImports(extParams));
                }
            } else {
                SwaggerTypeConvertor.JavaType mapping = typeConvertor.convert(modelImpl.getType(), modelImpl.getFormat());
                handlerMethodReturn.setType(mapping.formatType());
                handlerMethodReturn.getImports().add(mapping.imports());
            }
        } else {
            throw new RuntimeException("resp返回值只支持 $ref | type | List<$ref> |List<type> ");
        }
//        Optional.ofNullable(model)
//                .map(Model::getVendorExtensions)
//                .map(SwaggerVendorExtensions::getImports)
//                .ifPresent(handlerMethodReturn.getImports()::add);
        return handlerMethodReturn;
    }

}
