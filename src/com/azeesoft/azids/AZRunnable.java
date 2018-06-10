package com.azeesoft.azids;

public abstract class AZRunnable<T> implements Runnable {
    T param;

    public AZRunnable(T t) {
        setParam(t);
    }

    public void setParam(T param) {
        this.param = param;
    }

    public T getParam() {
        return param;
    }
}
