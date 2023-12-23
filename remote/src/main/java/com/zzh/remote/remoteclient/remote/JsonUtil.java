package com.zzh.remote.remoteclient.remote;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.lang.NonNull;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

/**
 * @Author：zzh
 */
public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    private JsonUtil(){}

    public static <T> T parse(@NonNull InputStream inputStream, Class<T> classType) throws IOException {
        return objectMapper.readValue(new InputStreamReader(inputStream), classType);
    }
}
