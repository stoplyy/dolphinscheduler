package org.apache.dolphinscheduler.plugin.alert.hermes;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.alert.api.AlertData;
import org.apache.dolphinscheduler.alert.api.AlertResult;
import org.apache.dolphinscheduler.alert.api.HttpServiceRetryStrategy;
import org.apache.dolphinscheduler.common.config.ApolloConfigUtil;
import org.apache.dolphinscheduler.common.config.ApolloConfigUtil.ApolloConfigKey;
import org.apache.dolphinscheduler.common.enums.ListenerEventType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.alert.comm.AlertDataExpHelper;
import org.apache.dolphinscheduler.plugin.alert.config.SolarisHttpClientConfig;
import org.apache.dolphinscheduler.plugin.alert.config.SolarisMessageDto;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class SolarisSender {

    private static final String DEFAULT_CHARSET = "utf-8";
    private static final int DEFAULT_TIMEOUT = 10;

    // 缓存 HttpClient 实例，避免重复创建
    private static final Map<SolarisHttpClientConfig, CloseableHttpClient> HTTP_CLIENT_CACHE = new ConcurrentHashMap<>();

    private final String url;
    private final String token;
    private final int timeout;
    private final SolarisHttpClientConfig config;

    private HttpRequestBase httpRequest;
    private CloseableHttpClient httpClient;

    public SolarisSender(Map<String, String> paramsMap) {
        this.url = StringUtils.isNotBlank(paramsMap.get("url"))
                ? paramsMap.get("url")
                : "https://solaris.tuhu.work/solaris/api/notify";
        this.token = StringUtils.isNotBlank(paramsMap.get("token"))
                ? paramsMap.get("token")
                : "";
        this.timeout = StringUtils.isNotBlank(paramsMap.get("timeout"))
                ? Integer.parseInt(paramsMap.get("timeout"))
                : DEFAULT_TIMEOUT;

        this.config = new SolarisHttpClientConfig(url, token, timeout);
        initHttpRequest();
    }

    private void initHttpRequest() {
        if (httpRequest == null) {
            httpRequest = new HttpPost(url);
        }

        httpRequest.setHeader("Content-Type", "application/json;charset=UTF-8");
        httpRequest.setHeader("Accept", "application/json");
        httpRequest.setHeader("Accept-Charset", "UTF-8");
        httpRequest.setHeader("appId", "int-service-arch-stellarops-event");
        httpRequest.setHeader("token", token);

        // 使用缓存获取或创建 HttpClient
        httpClient = getOrCreateHttpClient();
    }

    /**
     * 获取或创建 HttpClient，使用缓存避免重复创建
     */
    private CloseableHttpClient getOrCreateHttpClient() {
        return HTTP_CLIENT_CACHE.computeIfAbsent(config, this::createHttpClient);
    }

    /**
     * 创建新的 HttpClient 实例
     */
    private CloseableHttpClient createHttpClient(SolarisHttpClientConfig config) {
        log.info("Creating new HttpClient for config: {}", config);
        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(config.getTimeout() * 1000)
                .setConnectionRequestTimeout(config.getTimeout() * 1000)
                .setSocketTimeout(config.getTimeout() * 1000)
                .build();

        return HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(HttpServiceRetryStrategy.retryStrategy)
                .build();
    }

    /**
     * 清理缓存中的 HttpClient（可选的清理方法）
     */
    public static void clearCache() {
        log.info("Clearing HttpClient cache, current size: {}", HTTP_CLIENT_CACHE.size());
        HTTP_CLIENT_CACHE.values().forEach(client -> {
            try {
                client.close();
            } catch (Exception e) {
                log.warn("Error closing HttpClient", e);
            }
        });
        HTTP_CLIENT_CACHE.clear();
    }

    /**
     * 获取当前缓存大小（用于监控）
     */
    public static int getCacheSize() {
        return HTTP_CLIENT_CACHE.size();
    }

    public String getResponseString(HttpRequestBase httpRequest) throws Exception {
        CloseableHttpResponse response = httpClient.execute(httpRequest);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, DEFAULT_CHARSET);
    }

    public void send(AlertData alertData) {

        String msg = alertData.getContent();

        ListenerEventType eventType = alertData.getListenerEventType();
        if (eventType == null) {
            log.error("alertType is null, alertData: {}", alertData);
        }

        List<Map> eventList = JSONUtils.toList(msg, Map.class);
        for (Map<String, String> eventMap : eventList) {
            if (!isAlertEvent(eventMap)) {
                log.info("not an alert, skip it. eventMap: {}", eventMap);
                continue;
            }
            SolarisMessageDto dto = convertToDto(eventMap, eventType);
            sendMessage(dto);
        }
    }

    private boolean isAlertEvent(Map<String, String> eventMap) {
        String projectName = AlertDataExpHelper.tryGetProjectName(eventMap);
        String processName = AlertDataExpHelper.tryGetProcessName(eventMap);
        //process from apollo
        String processKey = String.format(ApolloConfigKey.SOLARIS_ALERT_PROCESS_ENABLED.getKey(), projectName, processName);
        String processEnable = ApolloConfigUtil.getProperty(processKey, "none");
        if(processEnable.equals("none")){
            String projectKey = String.format(ApolloConfigKey.SOLARIS_ALERT_PROJECT_ENABLED.getKey(), projectName);
            return ApolloConfigUtil.getProperty(projectKey, "none").equals("true");
        }
        return processEnable.equals("true");
    }

    private SolarisMessageDto convertToDto(Map<String, String> eventMap, ListenerEventType eventType) {
        SolarisMessageDto dto = new SolarisMessageDto();
        dto.setEndpoint(AlertDataExpHelper.tryGetTargetAppId(eventMap));
        dto.setEndpointType(0); 
        dto.setPlatform("stellarops");
        dto.setLevel("LOW");
        dto.setEventId(String.valueOf(System.currentTimeMillis()));
        dto.setStatus("ERROR");
        dto.setContent(AlertDataExpHelper.formatContent(eventMap, eventType));
        dto.setTitle(AlertDataExpHelper.formatTiltle(eventMap, eventType));
        dto.setType("SYSTEM");
        dto.setMetric("DolphinSchedulerAlert");
        dto.setEventTime(String.valueOf(System.currentTimeMillis()));
        dto.setDetailUrl(ApolloConfigUtil.getProperty(ApolloConfigKey.ALERT_SOLARIS_DETAIL_URL));
        return dto;
    }

    public void sendMessage(SolarisMessageDto dto) {
        try {
            final String bodyString = JSONUtils.toJsonString(dto);
            log.info("send alarm body {}", bodyString);

            StringEntity entity = new StringEntity(bodyString, DEFAULT_CHARSET);
            ((HttpPost) httpRequest).setEntity(entity);

            String response = this.getResponseString(httpRequest);
            log.info("send alarm response {}", response);

            // 根据响应判断是否成功
            @SuppressWarnings("unchecked")
            Map<String, Object> responseMap = JSONUtils.parseObject(response, Map.class);
            boolean isSuccess = responseMap != null && !responseMap.containsKey("error");
            log.info("send alarm result: {}", isSuccess ? "success" : "failed");
        } catch (Exception e) {
            log.error("send alarm error. ", e);
        }
    }
}
