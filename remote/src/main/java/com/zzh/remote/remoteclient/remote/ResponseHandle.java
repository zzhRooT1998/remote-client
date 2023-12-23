package com.zzh.remote.remoteclient.remote;

import java.lang.annotation.*;

/**
 * @Author：zzh
 */
@Target(ElementType.METHOD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface ResponseHandle {

    Class<? extends  ResponseHandler> handle() default DefaultResponseHandler.class;
}
