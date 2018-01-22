/**
 * Copyright 2012 Netflix, Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.netflix.hystrix.strategy.properties;

import java.util.concurrent.ConcurrentHashMap;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixThreadPool;
import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.HystrixThreadPoolProperties;
import com.netflix.hystrix.datastore.HystrixDataStore;
import com.netflix.hystrix.datastore.HystrixDataStoreProvider;
import com.netflix.hystrix.datastore.HystrixKeyDataStore;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.util.Lazy;

/**
 * Factory for retrieving properties implementations.
 * <p>
 * This uses given {@link HystrixPropertiesStrategy} implementations to construct Properties instances and caches each instance according to the cache key provided.
 * 
 * @ExcludeFromJavadoc
 */
public class HystrixPropertiesFactory {

    /**
     * Clears all the defaults in the static property cache. This makes it possible for property defaults to not persist for
     * an entire JVM lifetime.  May be invoked directly, and also gets invoked by <code>Hystrix.reset()</code>
     */
    public static void reset() {
        commandProperties.get().clear();
        threadPoolProperties.get().clear();
        collapserProperties.get().clear();
    }

    // String is CommandKey.name() (we can't use CommandKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static final Lazy<HystrixDataStore<String, HystrixCommandProperties>> commandProperties = HystrixDataStoreProvider.lazyInitDataStore();

    /**
     * Get an instance of {@link HystrixCommandProperties} with the given factory {@link HystrixPropertiesStrategy} implementation for each {@link HystrixCommand} instance.
     * 
     * @param key
     *            Pass-thru to {@link HystrixPropertiesStrategy#getCommandProperties} implementation.
     * @param builder
     *            Pass-thru to {@link HystrixPropertiesStrategy#getCommandProperties} implementation.
     * @return {@link HystrixCommandProperties} instance
     */
    public static HystrixCommandProperties getCommandProperties(HystrixCommandKey key, HystrixCommandProperties.Setter builder) {
        HystrixPropertiesStrategy hystrixPropertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        String cacheKey = hystrixPropertiesStrategy.getCommandPropertiesCacheKey(key, builder);
        if (cacheKey != null) {
            return commandProperties.get().getOrLoad(cacheKey, () ->  {
                HystrixCommandProperties.Setter keyBuilder = builder != null ? builder : HystrixCommandProperties.Setter();
                // create new instance
                return hystrixPropertiesStrategy.getCommandProperties(key, keyBuilder);
            });
        } else {
            // no cacheKey so we generate it with caching
            return hystrixPropertiesStrategy.getCommandProperties(key, builder);
        }
    }

    // String is ThreadPoolKey.name() (we can't use ThreadPoolKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static final Lazy<HystrixDataStore<String, HystrixThreadPoolProperties>> threadPoolProperties = HystrixDataStoreProvider.lazyInitDataStore();

    /**
     * Get an instance of {@link HystrixThreadPoolProperties} with the given factory {@link HystrixPropertiesStrategy} implementation for each {@link HystrixThreadPool} instance.
     * 
     * @param key
     *            Pass-thru to {@link HystrixPropertiesStrategy#getThreadPoolProperties} implementation.
     * @param builder
     *            Pass-thru to {@link HystrixPropertiesStrategy#getThreadPoolProperties} implementation.
     * @return {@link HystrixThreadPoolProperties} instance
     */
    public static HystrixThreadPoolProperties getThreadPoolProperties(HystrixThreadPoolKey key, HystrixThreadPoolProperties.Setter builder) {
        HystrixPropertiesStrategy hystrixPropertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        String cacheKey = hystrixPropertiesStrategy.getThreadPoolPropertiesCacheKey(key, builder);
        if (cacheKey != null) {
            return threadPoolProperties.get().getOrLoad(cacheKey, () -> {
                HystrixThreadPoolProperties.Setter keyBuilder = builder != null ? builder : HystrixThreadPoolProperties.Setter();
                // create new instance
                return hystrixPropertiesStrategy.getThreadPoolProperties(key, keyBuilder);
            });
        } else {
            // no cacheKey so we generate it with caching
            return hystrixPropertiesStrategy.getThreadPoolProperties(key, builder);
        }
    }

    // String is CollapserKey.name() (we can't use CollapserKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static final Lazy<HystrixDataStore<String, HystrixCollapserProperties>> collapserProperties = HystrixDataStoreProvider.lazyInitDataStore();

    /**
     * Get an instance of {@link HystrixCollapserProperties} with the given factory {@link HystrixPropertiesStrategy} implementation for each {@link HystrixCollapserKey} instance.
     * 
     * @param key
     *            Pass-thru to {@link HystrixPropertiesStrategy#getCollapserProperties} implementation.
     * @param builder
     *            Pass-thru to {@link HystrixPropertiesStrategy#getCollapserProperties} implementation.
     * @return {@link HystrixCollapserProperties} instance
     */
    public static HystrixCollapserProperties getCollapserProperties(HystrixCollapserKey key, HystrixCollapserProperties.Setter builder) {
        HystrixPropertiesStrategy hystrixPropertiesStrategy = HystrixPlugins.getInstance().getPropertiesStrategy();
        String cacheKey = hystrixPropertiesStrategy.getCollapserPropertiesCacheKey(key, builder);
        if (cacheKey != null) {
            return collapserProperties.get().getOrLoad(cacheKey, () -> {
                HystrixCollapserProperties.Setter keyBuilder = builder != null ? builder : HystrixCollapserProperties.Setter();
                // create new instance
                return hystrixPropertiesStrategy.getCollapserProperties(key, keyBuilder);
            });
        } else {
            // no cacheKey so we generate it with caching
            return hystrixPropertiesStrategy.getCollapserProperties(key, builder);
        }
    }

}
