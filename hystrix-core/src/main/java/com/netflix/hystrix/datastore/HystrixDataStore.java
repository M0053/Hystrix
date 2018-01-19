package com.netflix.hystrix.datastore;

import com.netflix.hystrix.util.Lazy;

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicMarkableReference;

public abstract class HystrixDataStore<K, V> {
    private static final HystrixDefaultDataStore.Factory DEFAULT_FACTORY = new HystrixDefaultDataStore.Factory();
    private static final AtomicMarkableReference<Factory> FACTORY = new AtomicMarkableReference<>(DEFAULT_FACTORY, false);

    public static void setFactory(final Factory factory) {
        if (!FACTORY.compareAndSet(DEFAULT_FACTORY, factory, false, true)) {
            throw new IllegalStateException("Factory has already been configured or accessed.");
        }
    }

    public static <V> Lazy<HystrixDataStore<String, V>> lazyStoreFor(final Class<V> clazz) {
        return new Lazy<>(() -> FACTORY.getReference().create(String.class, clazz));
    }

    public abstract V getOrLoad(K key, Callable<V> initializer);

    public abstract int size();

    public interface Factory {
        <K, V> HystrixDataStore<K, V> create(Class<K> keyClass, Class<V> valueClass);
    }

}
