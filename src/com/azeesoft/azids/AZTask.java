package com.azeesoft.azids;

import javafx.concurrent.Task;

public abstract class AZTask<T1,T2> extends Task {
    T1 param;
    T2 param2;

    public AZTask(T1 t){
        setParam(t);
    }

    public AZTask(T1 t, T2 t2){
        setParam(t);
        setParam2(t2);
    }

    public void setParam(T1 param) {
        this.param = param;
    }

    public T1 getParam() {
        return param;
    }

    public void setParam2(T2 param2) {
        this.param2 = param2;
    }

    public T2 getParam2() {
        return param2;
    }
}
