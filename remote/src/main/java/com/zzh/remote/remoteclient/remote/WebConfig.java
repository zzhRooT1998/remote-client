package com.zzh.remote.remoteclient.remote;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.DefaultUriBuilderFactory;

/**
 * @Author：zzh
 */
@Configuration
public class WebConfig {

    @Bean
    public RestTemplate restTemplate() {
        HttpClientBuilder httpClientBuilder = HttpClients.custom();
        PoolingHttpClientConnectionManager pool =  buildHttpClientPool(httpClientBuilder);

        CloseableHttpClient build = httpClientBuilder.setConnectionManager(pool).build();

        HttpComponentsClientHttpRequestFactory requestFactory = new HttpComponentsClientHttpRequestFactory(build);
        RestTemplate restTemplate = new RestTemplate(requestFactory);
        DefaultUriBuilderFactory defaultUriBuilderFactory = new DefaultUriBuilderFactory();
        defaultUriBuilderFactory.setEncodingMode(DefaultUriBuilderFactory.EncodingMode.VALUES_ONLY);
        restTemplate.setUriTemplateHandler(defaultUriBuilderFactory);

        return restTemplate;
    }

    private PoolingHttpClientConnectionManager buildHttpClientPool(HttpClientBuilder httpClientBuilder) {
        PoolingHttpClientConnectionManager pool = new PoolingHttpClientConnectionManager();
        pool.setDefaultMaxPerRoute(100);
        pool.setMaxTotal(100);

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(-1)
                .setSocketTimeout(-1)
                .setConnectionRequestTimeout(-1)
                .build();

        httpClientBuilder.setDefaultRequestConfig(requestConfig);
        return pool;
    }
}
