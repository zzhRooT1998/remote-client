package com.zzh.remote.remoteclient.remote;

import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author：zzh
 */
@Slf4j
@Component
public class DefaultRemoteHandle implements RemoteHandle {

    private final RestTemplate restTemplate;
    private final ServiceRegister serviceRegister;
    private final Map<String, Object> responseHandlerMap = new ConcurrentHashMap<>();

    public DefaultRemoteHandle(RestTemplate restTemplate, ServiceRegister serviceRegister) {
        this.restTemplate = restTemplate;
        this.serviceRegister = serviceRegister;
    }

    @Override
    public <V, R> V execute(Request<V, R> request) throws RemoteClientException {
        String path = request.getPath();
        ServiceRegister.Service service = serviceRegister.find(request.getService());
        final String url = service.getIp() + service.getNamespace() + path;

        HttpMethod method = request.getMethod();
        final HttpEntity<R> httpEntity = createAndGetHttpEntity(request.getBody(), request.getHeaders());

        //GET request can't calculate the content length atomically using restTemplate API.
        Assert.isTrue(!HttpMethod.GET.equals(method) || Objects.isNull(httpEntity) || Objects.isNull(httpEntity.getBody()),
                "GET request doesn't allow to have request body, since GET request can't calculate the content length atomically using restTemplate API");

        log.info("Start to call {}", url);

        ResponseEntity<Resource> response = restTemplate.exchange(url, method, httpEntity, Resource.class, new HashMap<>(0));

        return handleResponse(request, response);
    }

    private <V, R> V handleResponse(Request<V, R> request, ResponseEntity<Resource> response) throws RemoteClientException {
        final Class<ResponseHandler<V>> responseHandler = ((HttpRequest<V, R>) request).getResponseHandler();
        String responseHandlerName = responseHandler.getName();
        ResponseHandler<V> handler;

        try {
            if (responseHandlerMap.containsKey(responseHandlerName)) {
                handler = (ResponseHandler<V>) responseHandlerMap.get(responseHandlerName);
            } else {
                synchronized (this) {
                    if (responseHandlerMap.containsKey(responseHandlerName)) {
                        handler = (ResponseHandler<V>) responseHandlerMap.get(responseHandlerName);
                    } else {
                        handler = responseHandler.getDeclaredConstructor().newInstance();
                        responseHandlerMap.put(responseHandlerName, handler);
                    }
                }
            }

            return handler.handle(response, request);
        } catch (Exception e) {
            throw new RemoteClientException("Failed to handle third part service response", e);
        }
    }

    private <R> HttpEntity<R> createAndGetHttpEntity(R body, HttpHeaders headers) {
        if (Objects.nonNull(headers) || Objects.nonNull(body)) {
            return new HttpEntity<>(body, headers);
        }

        return null;
    }
}
