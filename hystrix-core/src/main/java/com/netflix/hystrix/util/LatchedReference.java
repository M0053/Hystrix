package com.netflix.hystrix.util;

import java.util.concurrent.atomic.AtomicBoolean;

/** A class that can only be modified until it has been accessed */
public class LatchedReference<T> {
    private final AtomicBoolean canModify = new AtomicBoolean(true);
    private T reference;

    public LatchedReference(T initialValue) {
        this.reference = initialValue;
    }

    /** Applies the semantics that once the reference is accessed it is latched to prevent further changes */
    public T get() {
        if (canModify.get()) {
            synchronized (canModify) {
                canModify.set(false);
            }
        }
        return reference;
    }

    /** Attempts to set the reference to the new value, returns if the reference was updated or not */
    public boolean set(T newValue) {
        synchronized (canModify) {
            if (canModify.get()) {
                this.reference = newValue;
            }
            return canModify.get();
        }
    }
}
