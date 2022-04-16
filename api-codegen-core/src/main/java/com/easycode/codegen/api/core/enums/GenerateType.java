package com.easycode.codegen.api.core.enums;

import lombok.Getter;

/**
 * @EnumName: GenerateType
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-13 17:48
 */
@Getter
public enum GenerateType {
    /**
     * 生成springmvc 的 controller service dto
     */
    SPRING_MVC("springmvc"),
    /**
     * 生成feignClient 的 FeignClient接口、dto
     */
    FEIGN_CLIENT("feignclient");

    private final String id;

    GenerateType(String id) {
        this.id = id;
    }

}
