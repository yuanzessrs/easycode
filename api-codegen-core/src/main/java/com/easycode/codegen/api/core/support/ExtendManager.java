package com.easycode.codegen.api.core.support;

import com.easycode.codegen.api.core.config.GlobalConfig;
import com.easycode.codegen.api.core.meta.ResolveResult;
import com.easycode.codegen.api.core.support.impl.*;

import java.util.ArrayList;
import java.util.List;

/**
 * @class-name: ExtendManager
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-08-13 20:55
 */
public class ExtendManager {

    private static final List<IExtendHandler> EXTEND_HANDLERS = new ArrayList<>();

    static {
        EXTEND_HANDLERS.add(new CustomDtoToStringHandlerImpl());
        EXTEND_HANDLERS.add(new CustomDTOBuilderHandlerImpl());
        EXTEND_HANDLERS.add(new ClassAutoImportHandlerImpl());
        EXTEND_HANDLERS.add(new DtoStringFieldCheckPluginHandler());

        EXTEND_HANDLERS.add(new DuplicatedNameControllerCheckHandlerImpl());
//        EXTEND_HANDLERS.add(new DuplicatedNameDTOCheckHandlerImpl());
    }

    public static void handle(GlobalConfig config, ResolveResult resolveResult) {
        EXTEND_HANDLERS.forEach(handler -> handler.handle(config, resolveResult));
    }

}
