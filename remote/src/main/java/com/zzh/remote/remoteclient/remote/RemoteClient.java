package com.zzh.remote.remoteclient.remote;

import org.springframework.stereotype.Component;

import java.lang.annotation.*;

@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface RemoteClient {

    /**
     * The value may indicate an actual service name that is running in AKS cluster, you can config this like : mow-api
     * Also, you can config as this format: ${extract. service}, it means will retrieve the actual value from environment variable
     */
    String value() default "";

    /**
     * The value may indicate a namespace that can inject from environment variable, you can config this like: mow-ds-iad
     * Also, you can config as this format: ${extract. namespace} , it means will retrieve the actual value from environment variable
     * If empty means call a same namespace service
     */
    String namespace() default "";

    /**
     * The value may indicate a response data field from third-part service response json, if response json like this:
     * {
     * "status": "Success",
     * "serviceData": "Data that you need"
     * }
     * you merely need config this value as "serviceData", then will return this field value and parse it to the corresponding
     * return type from the method signature to you.
     */
    String responseDataField() default "data";

    Type type() default Type.FQDN;

    enum Type {
        FQDN("http"),
        HTTPS("https");


        private final String type;

        Type(String type) {
            this.type = type;
        }

        public String getType() {
            return type;
        }
    }
}
