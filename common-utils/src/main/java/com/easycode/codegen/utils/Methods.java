package com.easycode.codegen.utils;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;
import java.util.stream.Stream;

/**
 * @class-name: Methods
 * @description:
 * @author: Mr.Zeng
 * @date: 2022-01-06 22:53
 */
public class Methods {

    private Methods() {
    }

    @SafeVarargs
    public static <T> T or(T... values) {
        return Stream.of(values).filter(Objects::nonNull).findFirst().orElse(null);
    }

    @SafeVarargs
    public static <T> T or(Supplier<T>... values) {
        return Stream.of(values)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst()
                .orElse(null);
    }

    @SafeVarargs
    public static <T> Optional<T> orWithOptional(Supplier<T>... values) {
        return Stream.of(values)
                .map(Supplier::get)
                .filter(Objects::nonNull)
                .findFirst();
    }

    public static void main(String[] args) {
        Supplier<String> t1 = () -> {
            System.out.println("exec t1.");
            return "t1";
        };
        Supplier<String> t2 = () -> {
            System.out.println("exec t2.");
            return "t2";
        };
        Supplier<String> t3 = () -> {
            System.out.println("exec t3.");
            return "t3";
        };
        // don't output exec t2. exec t3.
        System.out.println(or(t1, t2, t3));
    }

}
