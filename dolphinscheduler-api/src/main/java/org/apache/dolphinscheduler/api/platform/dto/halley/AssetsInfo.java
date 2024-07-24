package org.apache.dolphinscheduler.api.platform.dto.halley;

public class AssetsInfo {
    /**
     * { "ip": "10.3.6.134", "env": "prod", "hostName":
     * "sh1-int-spring-mlp-product-search-service-prod-2", "id":
     * "2418fed608ac4febbf06f2af8c9a0497" }
     */
    String ip;

    String env;
    String hostName;
    String id;

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public String getEnv() {
        return env;
    }

    public void setEnv(String env) {
        this.env = env;
    }

    public String getHostName() {
        return hostName;
    }

    public void setHostName(String hostName) {
        this.hostName = hostName;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }
}
