package com.netflix.hystrix.datastore;

import java.util.concurrent.Callable;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Default DataStore implementation backed by a ConcurrentHashMap.
 */
public class HystrixDefaultDataStore<K, V> extends HystrixDataStore<K, V> {
    /**
     * We want the stronger guarantees of ConcurrentHashMap's computeIfAbsent instead of
     * the weaker ones from the ConcurrentMap interface.
     */
    private final ConcurrentHashMap<K, V> storage;

    private HystrixDefaultDataStore(ConcurrentHashMap<K, V> storage) {
        this.storage = storage;
    }

    /**
     * returns the existing value for the key or gets the value from the initializer to insert and return
     */
    @Override
    public V getOrLoad(K key, Callable<V> initializer) {
        return storage.computeIfAbsent(key, k -> {
            try {
                return initializer.call();
            } catch (Exception e) {
                throw new RuntimeException("Error initializing entry for " + key, e);
            }
        });
    }

    @Override
    public int size() {
        return storage.size();
    }

    public static class Factory implements HystrixDataStore.Factory {

        @Override
        public <K, V> HystrixDataStore<K, V> create(Class<K> keyClass, Class<V> valueClass) {
            return new HystrixDefaultDataStore<>(new ConcurrentHashMap<>());
        }
    }
}
