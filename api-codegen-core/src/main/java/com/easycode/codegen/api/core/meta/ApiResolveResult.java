package com.easycode.codegen.api.core.meta;

import java.util.List;
import java.util.stream.Collectors;

import lombok.Data;

/**
 * @ClassName: ApiResolveResult
 * @Description: api 据解析结果
 * @Author: Mr.Zeng
 * @Date: 2021-04-24 18:17
 */
@Data
public class ApiResolveResult {

    private List<HandlerClass> classes;

    private List<Dto> dtos;



    public static ApiResolveResult merge(List<ApiResolveResult> resolveResults) {
        ApiResolveResult result = new ApiResolveResult();
        result.setClasses(resolveResults.stream().flatMap(o -> o.getClasses().stream()).collect(Collectors.toList()));
        result.setDtos(resolveResults.stream().flatMap(o -> o.getDtos().stream()).collect(Collectors.toList()));
        return result;
    }

}
