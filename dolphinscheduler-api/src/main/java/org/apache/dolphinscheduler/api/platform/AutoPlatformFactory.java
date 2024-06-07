package org.apache.dolphinscheduler.api.platform;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

@Service
public class AutoPlatformFactory {

    @Autowired
    StellarOpsOpenApiFactory stellarOpsOpenApiFactory;

    private static final Map<String, StellarOpsOpenApiEndpoint> endpointCache = new ConcurrentHashMap<>();

    public StellarOpsOpenApiEndpoint getClient(String appId) {
        StellarOpsOpenApiEndpoint checkClient = endpointCache.get(appId);
        if (checkClient != null) {
            return checkClient;
        }
        synchronized (this) {
            checkClient = endpointCache.get(appId);
            if (checkClient != null) {
                return checkClient;
            }

            checkClient = stellarOpsOpenApiFactory.createClient(appId);
            endpointCache.put(appId, checkClient);
            return checkClient;
        }
    }

}
