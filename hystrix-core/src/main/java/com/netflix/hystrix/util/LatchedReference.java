package com.netflix.hystrix.util;

import java.util.concurrent.atomic.AtomicBoolean;

/** A class for holding a reference that can be modified until it has been accessed */
public class LatchedReference<T> {
    private final AtomicBoolean isLatched = new AtomicBoolean(false);
    private T reference;

    public LatchedReference(T initialValue) {
        this.reference = initialValue;
    }

    /**
     * Applies the semantics that once the reference is accessed it is latched to prevent further changes
     * @return the contained reference.
     */
    public T get() {
        if (!isLatched.get()) {
            synchronized (isLatched) {
                isLatched.set(true);
            }
        }
        return reference;
    }

    /**
     * Attempts to set the reference to the new value.
     * @return true if the reference was updated or false if it was not updated.
     */
    public boolean set(T newValue) {
        if (!isLatched.get()) {
            synchronized (isLatched) {
                if (!isLatched.get()) {
                    this.reference = newValue;
                    return true;
                }
            }
        }
        return false;
    }
}
