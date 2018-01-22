package com.netflix.hystrix.datastore;

import java.util.Collection;
import java.util.function.Supplier;

public interface HystrixDataStore<K, V> {
    /**
     * MUST abide by the contract of return the current value for the key if present else generate the value from
     * the initializer then insert and return the newly initialized value.
     * Because of concurrency consideration the instance returned from the DataStore may not be the same one provided by
     * this threads invocation of the initializer.
     * @param key         the value to key the storage on
     * @param initializer the initializing expression that provides a value and should adhere to follow dictates:
     *                    It should be fast and simple, and not throw exceptions
     * @return the existing or new value associated with the key.
     */
    V getOrLoad(K key, Supplier<? extends V> initializer);

    /**
     * Returns the value associated with the key if present or else null.
     * @param key the value to find the entry for.
     * @return the value associated with key or null.
     */
    V getIfPresent(K key);

    /**
     * Must return the approximate number of keys currently stored.
     * Because of concurrency consideration the number may be an approximation rather than an exact value.
     * @return the number of keys in the DataStore.
     */
    int size();

    /**
     * Removes all entries from the DataStore.
     */
    void clear();

    /**
     * Removes the entry associated with a specific key from the DataStore.
     * @param key the key to remove the entry for.
     */
    void remove(K key);

    /**
     * @return a Collection view of the values stored in the DataStore.
     */
    Collection<V> values();

    interface Factory {
        <K, V> HystrixDataStore<K, V> create();

//        <K, V> HystrixDataStore<K, V> create(Function<K, V> mapper);
    }

}
