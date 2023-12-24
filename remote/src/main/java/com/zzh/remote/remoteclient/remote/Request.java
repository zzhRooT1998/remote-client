package com.zzh.remote.remoteclient.remote;

/**
 * @Author：zzh
 */

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;

/**
 * Use for defined request object
 *
 * @param <V> Response body class type
 * @param <R> Request body class type
 */
public interface Request<V, R> {
    R getBody();

    String getPath();

    String getService();

    HttpMethod getMethod();

    HttpHeaders getHeaders();

    Class<V> getResponseType();
}
