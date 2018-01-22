/**
 * Copyright 2015 Netflix, Inc.
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
package com.netflix.hystrix.collapser;

import java.util.concurrent.ConcurrentHashMap;

import com.netflix.hystrix.datastore.HystrixDataStoreProvider;
import com.netflix.hystrix.datastore.HystrixKeyDataStore;
import com.netflix.hystrix.util.Lazy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.netflix.hystrix.HystrixCollapserKey;
import com.netflix.hystrix.HystrixCollapserProperties;
import com.netflix.hystrix.strategy.HystrixPlugins;
import com.netflix.hystrix.strategy.concurrency.HystrixConcurrencyStrategy;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableHolder;
import com.netflix.hystrix.strategy.concurrency.HystrixRequestVariableLifecycle;
import com.netflix.hystrix.strategy.properties.HystrixPropertiesFactory;
import com.netflix.hystrix.util.HystrixTimer;

/**
 * Factory for retrieving the correct instance of a RequestCollapser.
 * 
 * @param <BatchReturnType>
 * @param <ResponseType>
 * @param <RequestArgumentType>
 */
public class RequestCollapserFactory<BatchReturnType, ResponseType, RequestArgumentType> {

    private static final Logger logger = LoggerFactory.getLogger(RequestCollapserFactory.class);

    private final CollapserTimer timer;
    private final HystrixCollapserKey collapserKey;
    private final HystrixCollapserProperties properties;
    private final HystrixConcurrencyStrategy concurrencyStrategy;
    private final Scope scope;

    public static interface Scope {
        String name();
    }
    
    // internally expected scopes, dealing with the not-so-fun inheritance issues of enum when shared between classes
    private static enum Scopes implements Scope {
        REQUEST, GLOBAL
    }
    
    public RequestCollapserFactory(HystrixCollapserKey collapserKey, Scope scope, CollapserTimer timer, HystrixCollapserProperties.Setter propertiesBuilder) {
        this(collapserKey, scope, timer, HystrixPropertiesFactory.getCollapserProperties(collapserKey, propertiesBuilder));
    }

    public RequestCollapserFactory(HystrixCollapserKey collapserKey, Scope scope, CollapserTimer timer, HystrixCollapserProperties properties) {
         /* strategy: ConcurrencyStrategy */
        this.concurrencyStrategy = HystrixPlugins.getInstance().getConcurrencyStrategy();
        this.timer = timer;
        this.scope = scope;
        this.collapserKey = collapserKey;
        this.properties = properties;

    }

    public HystrixCollapserKey getCollapserKey() {
        return collapserKey;
    }

    public Scope getScope() {
        return scope;
    }

    public HystrixCollapserProperties getProperties() {
        return properties;
    }

    public RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType> getRequestCollapser(HystrixCollapserBridge<BatchReturnType, ResponseType, RequestArgumentType> commandCollapser) {
        if (Scopes.REQUEST == Scopes.valueOf(getScope().name())) {
            return getCollapserForUserRequest(commandCollapser);
        } else if (Scopes.GLOBAL == Scopes.valueOf(getScope().name())) {
            return getCollapserForGlobalScope(commandCollapser);
        } else {
            logger.warn("Invalid Scope: {}  Defaulting to REQUEST scope.", getScope());
            return getCollapserForUserRequest(commandCollapser);
        }
    }

    /**
     * Static global cache of RequestCollapsers for Scope.GLOBAL
     */
    // String is CollapserKey.name() (we can't use CollapserKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static Lazy<HystrixKeyDataStore<HystrixCollapserKey, RequestCollapser<?, ?, ?>>> globalScopedCollapsers = HystrixDataStoreProvider.lazyInitKeyDataStore();

    @SuppressWarnings("unchecked")
    private RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType> getCollapserForGlobalScope(HystrixCollapserBridge<BatchReturnType, ResponseType, RequestArgumentType> commandCollapser) {
        return (RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>) globalScopedCollapsers.get().getOrLoad(collapserKey, () ->
                new RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>(commandCollapser, properties, timer, concurrencyStrategy));
    }

    /**
     * Static global cache of RequestVariables with RequestCollapsers for Scope.REQUEST
     */
    // String is HystrixCollapserKey.name() (we can't use HystrixCollapserKey directly as we can't guarantee it implements hashcode/equals correctly)
    private static Lazy<HystrixKeyDataStore<HystrixCollapserKey, HystrixRequestVariableHolder<RequestCollapser<?, ?, ?>>>> requestScopedCollapsers = HystrixDataStoreProvider.lazyInitKeyDataStore();

    /* we are casting because the Map needs to be <?, ?> but we know it is <ReturnType, RequestArgumentType> for this thread */
    @SuppressWarnings("unchecked")
    private RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType> getCollapserForUserRequest(HystrixCollapserBridge<BatchReturnType, ResponseType, RequestArgumentType> commandCollapser) {
        return (RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>) getRequestVariableForCommand(commandCollapser).get(concurrencyStrategy);
    }

    /**
     * Lookup (or create and store) the RequestVariable for a given HystrixCollapserKey.
     * 
     * @param commandCollapser collapser to retrieve {@link HystrixRequestVariableHolder} for
     * @return HystrixRequestVariableHolder
     */
    @SuppressWarnings("unchecked")
    private HystrixRequestVariableHolder<RequestCollapser<?, ?, ?>> getRequestVariableForCommand(final HystrixCollapserBridge<BatchReturnType, ResponseType, RequestArgumentType> commandCollapser) {
        return requestScopedCollapsers.get().getOrLoad(commandCollapser.getCollapserKey(), () ->
                (HystrixRequestVariableHolder) new RequestCollapserRequestVariable(commandCollapser, properties, timer, concurrencyStrategy));
    }

    /**
     * Clears all state. If new requests come in instances will be recreated and metrics started from scratch.
     */
    public static void reset() {
        globalScopedCollapsers.get().clear();
        requestScopedCollapsers.get().clear();
        HystrixTimer.reset();
    }

    /**
     * Used for testing
     */
    public static void resetRequest() {
        requestScopedCollapsers.get().clear();
    }

    /**
     * Used for testing
     */
    public static HystrixRequestVariableHolder<RequestCollapser<?, ?, ?>> getRequestVariable(HystrixCollapserKey key) {
        return requestScopedCollapsers.get().getIfPresent(key);
    }

    /**
     * Request scoped RequestCollapser that lives inside a RequestVariable.
     * <p>
     * This depends on the RequestVariable getting reset before each user request in NFFilter to ensure the RequestCollapser is new for each user request.
     */
    private final class RequestCollapserRequestVariable extends HystrixRequestVariableHolder<RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>> {

        /**
         * NOTE: There is only 1 instance of this for the life of the app per HystrixCollapser instance. The state changes on each request via the initialValue()/get() methods.
         * <p>
         * Thus, do NOT put any instance variables in this class that are not static for all threads.
         */

        private RequestCollapserRequestVariable(final HystrixCollapserBridge<BatchReturnType, ResponseType, RequestArgumentType> commandCollapser, final HystrixCollapserProperties properties, final CollapserTimer timer, final HystrixConcurrencyStrategy concurrencyStrategy) {
            super(new HystrixRequestVariableLifecycle<RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>>() {
                @Override
                public RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType> initialValue() {
                    // this gets calls once per request per HystrixCollapser instance
                    return new RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType>(commandCollapser, properties, timer, concurrencyStrategy);
                }

                @Override
                public void shutdown(RequestCollapser<BatchReturnType, ResponseType, RequestArgumentType> currentCollapser) {
                    // shut down the RequestCollapser (the internal timer tasks)
                    if (currentCollapser != null) {
                        currentCollapser.shutdown();
                    }
                }
            });
        }
    }
}
