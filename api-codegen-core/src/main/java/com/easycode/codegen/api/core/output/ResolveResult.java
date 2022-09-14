package com.easycode.codegen.api.core.output;

import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: ResolveResult
 * @Description: api 据解析结果
 * @Author: Mr.Zeng
 * @Date: 2021-04-24 18:17
 */
@Data
public class ResolveResult {

    private List<HandlerClass> classes;

    private List<Dto> dtos;

    public static ResolveResult merge(List<ResolveResult> resolveResults) {
        ResolveResult result = new ResolveResult();
        result.setClasses(resolveResults.stream().flatMap(o -> o.getClasses().stream()).collect(Collectors.toList()));
        result.setDtos(resolveResults.stream().flatMap(o -> o.getDtos().stream()).collect(Collectors.toList()));
        return result;
    }

}
