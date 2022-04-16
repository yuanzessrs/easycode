package com.easycode.codegen.api.core.holders;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @ClassName: DataHolder
 * @Description: TODO
 * @Author: Mr.Zeng
 * @Date: 2021-05-24 21:54
 */
public class DataHolder<T> {

    private final List<T> container;

    public DataHolder() {
        this.container = new ArrayList<>();
    }

    public List<T> get() {
        return container;
    }

    public List<T> getAscSorted() {
        return container.stream().sorted().collect(Collectors.toList());
    }

    public List<T> getDescSorted() {
        List<T> result = container.stream().sorted().collect(Collectors.toList());
        Collections.reverse(result);
        return result;
    }

    @SafeVarargs
    public final void add(T... items) {
        add(Arrays.asList(items));
    }

    public void add(List<T> items) {
        container.addAll(items);
    }

}
