package org.apache.dolphinscheduler.api.platform;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.openfeign.FeignContext;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

import feign.Client;
import feign.Contract;
import feign.Feign;
import feign.Request;
import feign.codec.Decoder;
import feign.codec.Encoder;

@Component
public class StellarOpsOpenApiFactory implements ApplicationContextAware {

    private ApplicationContext applicationContext;

    private Client client;

    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Autowired
    public void setClient(Client client) {
        this.client = client;
    }

    public StellarOpsOpenApiEndpoint createClient(String appId) {
        Feign.Builder builder = Feign.builder();
        builder.client(client);
        return builder.options(new Request.Options(5 * 1000, 20 * 1000))
                .encoder(get(Encoder.class))
                .decoder(get(Decoder.class))
                .contract(get(Contract.class))
                .target(StellarOpsOpenApiEndpoint.class, "http://" + appId);
    }

    protected <T> T get(Class<T> type) {
        FeignContext context = applicationContext.getBean(FeignContext.class);
        T instance = context.getInstance("int-service-arch-stellarops-api", type);
        if (instance == null) {
            throw new IllegalStateException(
                    "No bean found of type " + type + " for " + "int-service-arch-stellarops-api");
        }
        return instance;
    }
}