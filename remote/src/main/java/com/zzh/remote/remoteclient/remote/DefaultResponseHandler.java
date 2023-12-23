package com.zzh.remote.remoteclient.remote;

import org.springframework.core.io.Resource;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.FieldManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.description.type.TypeDefinition;
import net.bytebuddy.description.type.TypeDescription;
import net.bytebuddy.dynamic.DynamicType;
import net.bytebuddy.implementation.FieldAccessor;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.Nullable;

import java.io.InputStream;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author：zzh
 */
@Slf4j
public class DefaultResponseHandler<V> implements ResponseHandler<V> {

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <R> V handle(ResponseEntity<Resource> response, Request<V, R> request) throws RemoteClientException {
        Resource body = response.getBody();
        if (Objects.isNull(body)) {
            return null;
        }

        HttpRequest<V, R> httpRequest = (HttpRequest<V, R>) request;
        boolean directlyReturn = httpRequest.directlyReturn();
        Class<V> responseType = httpRequest.getResponseType();
        String responseDataField = httpRequest.getResponseDataField();

        try {
            InputStream inputStream = body.getInputStream();

            if (directlyReturn) {
                return JsonUtil.parse(inputStream, responseType);
            }

            final Class<? extends Response<V>> responseDeriveClass = ResponseTransfer.generateClass(responseType, responseDataField);

            final Response<V> responseBody = JsonUtil.parse(inputStream, responseDeriveClass);

            if (log.isDebugEnabled()) {
                log.debug("Response body is: {}", responseBody);
            }

            Field declaredField = responseBody.getClass().getDeclaredField(responseDataField);

            return (V) declaredField.get(responseBody);
        } catch (Exception e) {
            throw new RemoteClientException("Failed to parse third part service response", e);
        }
    }

    @SuppressWarnings("unchecked")
    static class ResponseTransfer {

        private static final String SPLIT_CHAR = "#";
        static final String REMOTE_RESPONSE = "RemoteResponse";

        private ResponseTransfer() {};

        private static final Map<String, Class<? extends Response<?>>> RESPONSE_DERIVE_CLASS_MAP = new ConcurrentHashMap<>();

        public static <V> Class<? extends Response<V>> generateClass(Class<V> responseDataType, String filedName) {
            final String responseDeriveClassCacheKey = responseDataType.getName()  + SPLIT_CHAR + filedName;

            if (RESPONSE_DERIVE_CLASS_MAP.containsKey(responseDeriveClassCacheKey)) {
                return (Class<? extends Response<V>>) RESPONSE_DERIVE_CLASS_MAP.get(responseDeriveClassCacheKey);
            }

            return createResponseDeriveClass(responseDataType, filedName, responseDeriveClassCacheKey);
        }

        private static <V> Class<? extends Response<V>> createResponseDeriveClass(
                Class<V> responseDataType, String filedName, String responseDeriveClassCacheKey
        ) {
            //Double-check to prevent multiple instances from being created for a single key
            if (RESPONSE_DERIVE_CLASS_MAP.containsKey(responseDeriveClassCacheKey)) {
                return (Class<? extends Response<V>>) RESPONSE_DERIVE_CLASS_MAP.get(responseDeriveClassCacheKey);
            }

            TypeDescription.Generic genericSuperClass =
                    TypeDescription.Generic.Builder.parameterizedType(Response.class, responseDataType).build();

            DynamicType.Builder<?> classBuilder = new ByteBuddy().subclass(genericSuperClass)
                    .name(Response.class.getPackage().getName().concat(".").concat(REMOTE_RESPONSE));

            DynamicType.Unloaded<?> responseDeriveClassFile = definedProperty(classBuilder, filedName, TypeDefinition.Sort.describe(responseDataType), false, Visibility.PUBLIC).make();

            Class<? extends Response<V>> responseDeriveClass = (Class<? extends Response<V>>) responseDeriveClassFile.load(Response.class.getClassLoader()).getLoaded();
            RESPONSE_DERIVE_CLASS_MAP.put(responseDeriveClassCacheKey, responseDeriveClass);

            return responseDeriveClass;
        }

        private static <S> DynamicType.Builder.FieldDefinition.Optional<S> definedProperty(DynamicType.Builder<?> builder, String name, TypeDescription.Generic type, boolean readOnly, Visibility visibility) {
            if (name.length() == 0) {
                throw new IllegalArgumentException("A bean property cannot have an empty name");
            } else if (type.represents(void.class)) {
                throw new IllegalArgumentException("A bean property cannot have a void type");
            }

            FieldManifestation fieldManifestation;
            if (!readOnly) {
                builder = builder
                        .defineMethod("set" + Character.toUpperCase(name.charAt(0)) + name.substring(1), void.class, Visibility.PUBLIC)
                        .withParameters(type)
                        .intercept(FieldAccessor.ofField(name));
                fieldManifestation = FieldManifestation.PLAIN;
            } else {
                fieldManifestation = FieldManifestation.FINAL;
            }

            return (DynamicType.Builder.FieldDefinition.Optional<S>) builder
                    .defineMethod((type.represents(boolean.class)
                            ? "is"
                            : "get") + Character.toUpperCase(name.charAt(0)) + name.substring(1), type, Visibility.PUBLIC)
                    .intercept(FieldAccessor.ofField(name))
                    .defineField(name, type, visibility, fieldManifestation);
        }
    }
}
