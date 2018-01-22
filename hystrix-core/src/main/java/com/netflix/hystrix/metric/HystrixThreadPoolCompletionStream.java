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
package com.netflix.hystrix.metric;

import com.netflix.hystrix.HystrixThreadPoolKey;
import com.netflix.hystrix.datastore.HystrixDataStoreProvider;
import com.netflix.hystrix.datastore.HystrixKeyDataStore;
import com.netflix.hystrix.util.Lazy;
import rx.Observable;
import rx.subjects.PublishSubject;
import rx.subjects.SerializedSubject;
import rx.subjects.Subject;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/**
 * Per-ThreadPool stream of {@link HystrixCommandCompletion}s.  This gets written to by {@link HystrixThreadEventStream}s.
 * Events are emitted synchronously in the same thread that performs the command execution.
 */
public class HystrixThreadPoolCompletionStream implements HystrixEventStream<HystrixCommandCompletion> {

    private final HystrixThreadPoolKey threadPoolKey;

    private final Subject<HystrixCommandCompletion, HystrixCommandCompletion> writeOnlySubject;
    private final Observable<HystrixCommandCompletion> readOnlyStream;

    private static final Lazy<HystrixKeyDataStore<HystrixThreadPoolKey, HystrixThreadPoolCompletionStream>> streams = HystrixDataStoreProvider.lazyInitKeyDataStore();

    public static HystrixThreadPoolCompletionStream getInstance(HystrixThreadPoolKey threadPoolKey) {
        return streams.get().getOrLoad(threadPoolKey, () -> new HystrixThreadPoolCompletionStream(threadPoolKey));
    }

    HystrixThreadPoolCompletionStream(final HystrixThreadPoolKey threadPoolKey) {
        this.threadPoolKey = threadPoolKey;

        this.writeOnlySubject = new SerializedSubject<HystrixCommandCompletion, HystrixCommandCompletion>(PublishSubject.<HystrixCommandCompletion>create());
        this.readOnlyStream = writeOnlySubject.share();
    }

    public static void reset() {
        streams.get().clear();
    }

    public void write(HystrixCommandCompletion event) {
        writeOnlySubject.onNext(event);
    }

    @Override
    public Observable<HystrixCommandCompletion> observe() {
        return readOnlyStream;
    }

    @Override
    public String toString() {
        return "HystrixThreadPoolCompletionStream(" + threadPoolKey.name() + ")";
    }
}
