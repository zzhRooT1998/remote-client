package com.zzh.remote.remoteclient.remote;

import com.fasterxml.jackson.databind.JsonNode;
import lombok.Getter;
import lombok.Setter;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.lang.NonNull;
import org.springframework.util.Assert;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.RequestMethod;

import java.util.*;

import static java.lang.String.format;

/**
 * @Author：zzh
 */
@Getter
@Setter
public class HttpRequest<V, R> implements Request<V, R> {
    public static final String TEXT = "text";
    public static final String ACCEPT = "Accept";
    public static final String CONTENT_TYPE = "Coneten-type";

    private String path;
    private String service;
    private List<String> urlParams;
    private Map<String, String> pathVariables;

    /**
     * Default method is {@link HttpMethod.GET}
     */
    private HttpMethod method = HttpMethod.GET;

    private HttpHeaders headers;

    private R body;

    /**
     * If request header contains Accept key and value contains text will be set to ture, such as;
     * 1. Accept=text/plain
     * 2. Accept=text/ xmL
     * 3. Accept=text/htmL
     */
    private Boolean directlyReturn;

    /**
     * Related field name of actual data of third-part service response
     */
    private String responseDataField;

    /**
     * Default response handler is {@link DefaultResponseHandler}
     */
    private Class<ResponseHandler<V>> responseHandler;

    /**
     * Default responseType is {@link JsonNode)
     */
    @SuppressWarnings("unchecked")
    private Class<V> responseType = (Class<V>) JsonNode.class;

    @SuppressWarnings("unchecked")
    public HttpRequest() {
        Class<? extends ResponseHandler> responseHandlerTemp = DefaultResponseHandler.class;
        responseHandler = (Class<ResponseHandler<V>>) responseHandlerTemp;
    }

    public HttpRequest<V, R> pathVariable(String key, Object value) {
        if (Objects.isNull(pathVariables)) {
            pathVariables = new HashMap<>();
        }

        Assert.isTrue(Objects.isNull(value) || value instanceof String, "Path variable parameter merely can be String type");

        pathVariables.put(key, Objects.isNull(value) ? null : String.valueOf(value));

        return this;
    }

    public HttpRequest<V, R> responseHandler(Class<ResponseHandler<V>> responseHandler) {
        this.responseHandler = responseHandler;
        return this;
    }

    public HttpRequest<V, R> service(String service) {
        this.service = service;
        return this;
    }


    @Override
    public String getPath() {
        Assert.isTrue(StringUtils.hasText(path), "Request path can not be empty!");

        if (!CollectionUtils.isEmpty(pathVariables)) {
            for (Map.Entry<String, String> entry : pathVariables.entrySet()) {
                String key = entry.getKey();
                String value = entry.getValue();

                path = path.replace(format("{%s}", key), value);
            }
        }

        StringBuilder requestPath = new StringBuilder(path);
        if (CollectionUtils.isEmpty(urlParams)){
            return requestPath.toString();
        }

        return requestPath.append("?").append(String.join("&", urlParams)).toString();
    }


    public HttpRequest<V, R> path(String path) {
        this.path = path;
        return this;
    }

    public HttpRequest<V, R> param(String key, Object value) {
        if (Objects.isNull(urlParams)) {
            urlParams = new ArrayList<>();
        }

        urlParams.add(key + format("=%s", value));

        return this;
    }

    public HttpRequest<V, R> body(R body) {
        this.body = body;
        return this;
    }

    public HttpRequest<V, R> post() {
        this.method = HttpMethod.POST;
        return this;
    }
    public HttpRequest<V, R> delete() {
        this.method = HttpMethod.DELETE;
        return this;
    }
    public HttpRequest<V, R> get() {
        this.method = HttpMethod.GET;
        return this;
    }
    public HttpRequest<V, R> put() {
        this.method = HttpMethod.PUT;
        return this;
    }

    public HttpRequest<V, R> responseType(Class<V> classType) {
        this.responseType = classType;
        return this;
    }

    public HttpRequest<V, R> header(String key, String value) {
        check(key, value);

        if (Objects.isNull(headers)) {
            headers = new HttpHeaders();
        }

        headers.add(key, Objects.isNull(value) ? null : value);
        return this;
    }

    private void check(String key, String value) {
        if (ACCEPT.equals(key) && Objects.nonNull(value) && value.contains(TEXT)) {
            directlyReturn = true;
        }
    }

    private void check(String key, @NonNull String[] values) {
        if (!ACCEPT.equals(key)) {
            return;
        }

        boolean anyMatch = Arrays.stream(values).anyMatch(value -> Objects.nonNull(value) && value.contains(TEXT));

        if (anyMatch){
            directlyReturn = true;
        }
    }

    public HttpRequest<V, R> header(String key, String[] value) {
        if (Objects.isNull(value)) {
            return this;
        }

        check(key, value);

        if (Objects.isNull(headers)) {
            headers = new HttpHeaders();
        }

        headers.addAll(key, Arrays.asList(value));
        return this;
    }

    public HttpRequest<V, R> method(RequestMethod requestMethod) {
        this.method = HttpMethod.resolve(requestMethod.name());
        return this;
    }
    public HttpRequest<V, R> responseDataField(String responseDataField) {
        this.responseDataField = responseDataField;
        return this;
    }

    public boolean directlyReturn() {
        return Boolean.TRUE.equals(directlyReturn);
    }
}
