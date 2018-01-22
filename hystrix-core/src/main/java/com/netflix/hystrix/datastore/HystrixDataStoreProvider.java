package com.netflix.hystrix.datastore;

import com.netflix.hystrix.HystrixKey;
import com.netflix.hystrix.util.LatchedReference;
import com.netflix.hystrix.util.Lazy;

public class HystrixDataStoreProvider {
//    private static final HystrixDataStore.Factory DEFAULT_FACTORY = new HystrixDefaultDataStore.Factory();
    private static final HystrixDataStore.Factory DEFAULT_FACTORY = new HystrixCachedDataStore.Factory.Builder().build();
    private static final LatchedReference<HystrixDataStore.Factory> FACTORY = new LatchedReference<>(DEFAULT_FACTORY);

    public static <K extends HystrixKey, V> Lazy<HystrixKeyDataStore<K, V>> lazyInitKeyDataStore() {
        return new Lazy<>(() -> new HystrixKeyDataStore<K, V>(FACTORY.get().create()));
    }

    public static <K, V> Lazy<HystrixDataStore<K, V>> lazyInitDataStore() {
        return new Lazy<>(() -> FACTORY.get().create());
    }

    public static void setFactory(final HystrixDataStore.Factory factory) {
        if (!FACTORY.set(factory)) {
            throw new IllegalStateException("Factory has already been configured or accessed.");
        }
    }
}
