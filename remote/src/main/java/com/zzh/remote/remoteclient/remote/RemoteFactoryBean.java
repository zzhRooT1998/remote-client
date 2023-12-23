package com.zzh.remote.remoteclient.remote;

import org.springframework.beans.factory.FactoryBean;

import java.lang.reflect.Proxy;

/**
 * @Author：zzh
 */
public class RemoteFactoryBean<T> implements FactoryBean<T> {

    private final Class<T> interfaceClass;

    public RemoteFactoryBean(Class<T> classType) {
        this.interfaceClass = classType;
    }

    @Override
    @SuppressWarnings("unchecked")
    public T getObject() throws Exception {
        return (T) Proxy.newProxyInstance(interfaceClass.getClassLoader(), new Class[]{interfaceClass}, ((proxy, method, args) -> null));
    }

    @Override
    public Class<?> getObjectType() {
        return interfaceClass;
    }
}
