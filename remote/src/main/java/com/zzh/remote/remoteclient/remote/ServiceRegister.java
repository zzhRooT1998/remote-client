package com.zzh.remote.remoteclient.remote;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

import java.util.HashMap;
import java.util.Map;

import static java.lang.String.format;

/**
 * @Author：zzh
 */
@Component
@ConfigurationProperties(prefix = "remote")
public class ServiceRegister {

    @Setter
    private Map<String, Service> services = new HashMap<>();

    public Service find(String serviceName) {
        Service service = this.services.get(serviceName);

        Assert.notNull(service, format("The service=%s is not registered", serviceName));

        return service;
    }


    @Getter
    @Setter
    static class Service {
        private String ip;
        private String namespace;
    }
}
