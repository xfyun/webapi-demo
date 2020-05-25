package com.iflytek.webapi.sort;

public interface Sorter<T> {
    void put(T o, boolean last)throws Exception;
    T get() throws Exception;
}
