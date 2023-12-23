package com.zzh.remote.remoteclient.remote;

public interface RemoteHandle {

    <V, R> V execute(Request<V, R> request) throws RemoteClientException;
}
