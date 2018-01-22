package com.netflix.hystrix.util;

import java.util.concurrent.Callable;
import java.util.function.Supplier;

/**
 * Classic implementation of lazily initialized instance reference.
 * This is necessary to over come Hystrix over abundance of static initialization.
 * @param <T> type of the reference that will be lazily initialized.
 */
public class Lazy<T> implements Supplier<T> {
    private final Supplier<T> initializer;
    private volatile T instance;

    /**
     * Creates a Lazily initialized instance from the supplied initializer.
     * @param initializer the Callable that will produce the value to use.
     */
    public Lazy(Supplier<T> initializer) {
        this.initializer = initializer;
    }

    /**
     * idiomatic implementation of double checked lazy initialization.
     * https://en.wikipedia.org/wiki/Double-checked_locking#Usage_in_Java
     */
    public T get() {
        T result = instance;
        if (result == null) {
            synchronized(this) {
                result = instance;
                if (result == null) {
                    try {
                        instance = result = initializer.get();
                    } catch (Exception e) {
                        throw new RuntimeException("Error initializing instance", e);
                    }
                }
            }
        }
        return result;
    }
}
