package org.apache.dolphinscheduler.plugin.alert.config;

import java.util.Objects;

import lombok.Data;

/**
 * HttpClient 配置键，用于缓存
 */
@Data
public class SolarisHttpClientConfig {

    private final String url;
    private final String token;
    private final int timeout;

    public SolarisHttpClientConfig(String url, String token, int timeout) {
        this.url = url;
        this.token = token;
        this.timeout = timeout;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null || getClass() != obj.getClass())
            return false;
        SolarisHttpClientConfig that = (SolarisHttpClientConfig) obj;
        return timeout == that.timeout &&
                Objects.equals(url, that.url) &&
                Objects.equals(token, that.token);
    }

    @Override
    public int hashCode() {
        return Objects.hash(url, token, timeout);
    }

    @Override
    public String toString() {
        return "HttpClientConfig{url='" + url + "', token='" + token + "', timeout=" + timeout + "}";
    }
}
