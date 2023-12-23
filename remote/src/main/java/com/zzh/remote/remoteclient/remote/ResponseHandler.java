package com.zzh.remote.remoteclient.remote;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;

/**
 * @Author：zzh
 */
@FunctionalInterface
public interface ResponseHandler<V> {
    <R> V handle(ResponseEntity<Resource> response, Request<V, R> request) throws RemoteClientException;
}
