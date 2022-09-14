package com.easycode.codegen.api.core.support;

import com.easycode.codegen.api.core.input.GlobalConfig;
import com.easycode.codegen.api.core.output.ResolveResult;

/**
 * @interface-name: IExtendHandler
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 20:56
 */
public interface IExtendHandler {

    void handle(GlobalConfig config, ResolveResult resolveResult);

}
