package com.easycode.codegen.api.core.meta;

import java.util.List;
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

}
