/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.plugin.alert.hermes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.alert.api.AlertData;
import org.apache.dolphinscheduler.alert.api.AlertResult;
import org.apache.dolphinscheduler.alert.api.HttpServiceRetryStrategy;
import org.apache.dolphinscheduler.common.enums.ListenerEventType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.alert.comm.AlertDataExpHelper;
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
public final class HermesSender {

    /**
     * request type get
     */
    private static final String DEFAULT_CHARSET = "utf-8";
    private final int timeout;
    private final String eventCode;
    private final List<Long> ignoreProjectCodes;
    private final List<String> filterEventTypes;
    private final String url;
    private final String token;
    private final String env;

    private HttpRequestBase httpRequest;
    private CloseableHttpClient httpClient;

    public HermesSender(Map<String, String> paramsMap) {
        eventCode = StringUtils.isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_EVENT_CODE, ""))
                ? paramsMap.get(HermesAlertConstants.NAME_EVENT_CODE)
                : "stellarops-comm";
        token = StringUtils.isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_TOKEN, ""))
                ? paramsMap.get(HermesAlertConstants.NAME_TOKEN)
                : "9ce670c97889488f8b5db67853f5c1fa";
        ignoreProjectCodes = StringUtils
                .isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_IGNORE_PROJECT_CODE, ""))
                        ? Arrays.stream(paramsMap.getOrDefault(HermesAlertConstants.NAME_IGNORE_PROJECT_CODE, "")
                                .toString().split(","))
                                .map(Long::parseLong)
                                .collect(Collectors.toList())
                        : new ArrayList<Long>();
        filterEventTypes = StringUtils
                .isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_FILTER_TASK_TYPE_CODE, ""))
                        ? Arrays.stream(paramsMap.getOrDefault(HermesAlertConstants.NAME_FILTER_TASK_TYPE_CODE, "")
                                .toString().split(","))
                                .collect(Collectors.toList())
                        : new ArrayList<String>();
        url = StringUtils.isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_URL, ""))
                ? paramsMap.get(HermesAlertConstants.NAME_URL)
                : "https://event-server.tuhu.work/event/upload";

        timeout = StringUtils.isNotBlank(paramsMap.get(HermesAlertConstants.NAME_TIMEOUT))
                ? Integer.parseInt(paramsMap.get(HermesAlertConstants.NAME_TIMEOUT))
                : HermesAlertConstants.DEFAULT_TIMEOUT;
        env = StringUtils.isNotBlank(paramsMap.getOrDefault(HermesAlertConstants.NAME_ENV, ""))
                ? paramsMap.get(HermesAlertConstants.NAME_ENV)
                : "work";

        initHttpRequest();
    }

    public AlertResult send(AlertData alertData) {
        String msg = alertData.getContent();

        ListenerEventType eventType = alertData.getListenerEventType();
        if (eventType == null) {
            log.error("alertType is null, alertData: {}", alertData);
            return new AlertResult("false", "alertType is null");
        }
        if (filterEventTypes.contains(eventType.name())) {
            log.info("event type is filtered: {}", eventType.name());
            return new AlertResult("true", "event type is filtered: " + eventType.name());
        }

        List<Map> eventList = JSONUtils.toList(msg, Map.class);
        List<AlertResult> alertResults = new ArrayList<>();
        for (Map<String, String> eventMap : eventList) {
            AlertResult alertResult = new AlertResult();
            if (ignoreProjectCodes.contains(eventMap.getOrDefault("projectCode", "0"))) {
                log.info("ignore project code: {}, eventMap: {}", eventMap.get("projectCode"), eventMap);
                continue;
            }

            try {
                Map<String, Object> paramsMap = new HashMap<>();
                paramsMap.put("eventCode", AlertDataExpHelper.tryGetHermesEventCode(eventType));
                paramsMap.put("eventOccurTime", AlertDataExpHelper.tryGetEventStartTimg(eventMap));
                // paramsMap.put("templateKeyValue", eventMap);
                paramsMap.put("targetAppId", AlertDataExpHelper.tryGetTargetAppId(eventMap));
                paramsMap.put("operate", AlertDataExpHelper.tryGetOperate(eventMap));
                paramsMap.put("content", AlertDataExpHelper.formatContent(eventMap, eventType));
                paramsMap.put("env", env);
                paramsMap.put("workspaceId", 1);
                final String bodyString = JSONUtils.toJsonString(paramsMap);
                log.info("http request body: {}", bodyString);
                StringEntity entity = new StringEntity(bodyString, DEFAULT_CHARSET);
                ((HttpPost) httpRequest).setEntity(entity);
            } catch (Exception e) {
                log.error("send http alert msg  exception : {}", e.getMessage());
            }

            if (httpRequest == null) {
                alertResult.setStatus("false");
                alertResult.setMessage("Request types are not supported");
                alertResults.add(alertResult);
                continue;
            }

            try {
                // response error:
                // {"timestamp":"2025-06-04T11:41:48.524+08:00","status":400,"error":"Bad
                // Request","path":"/event/upload"}
                String resp = this.getResponseString(httpRequest);
                Map<String, Object> responseMap = JSONUtils.parseObject(resp, Map.class);
                if (responseMap.containsKey("status") && responseMap.get("status").equals(400)) {
                    alertResult.setStatus("false");
                    alertResult.setMessage(
                            String.format("Send http request alert failed: %s", responseMap.get("error")));
                    alertResults.add(alertResult);
                    log.error("send http alert msg failed, response: {}", resp);
                    continue;
                }
                log.info("send http alert msg success, response: {}", resp);
                alertResult.setStatus("true");
                alertResult.setMessage(resp);
                alertResults.add(alertResult);
            } catch (Exception e) {
                log.error("send http alert msg  exception : {}", e.getMessage());
                alertResult.setStatus("false");
                alertResult.setMessage(
                        String.format("Send http request alert failed: %s", e.getMessage()));
                alertResults.add(alertResult);
            }
        }

        // alertResults 合并生产一个alertResult
        AlertResult result = new AlertResult();
        result.setStatus(alertResults.stream().allMatch(alertResult -> alertResult.getStatus().equals("true")) ? "true"
                : "false");
        result.setMessage(alertResults.stream().map(AlertResult::getMessage).collect(Collectors.joining(", ")));
        return result;
    }

    public String getResponseString(HttpRequestBase httpRequest) throws Exception {
        CloseableHttpResponse response = httpClient.execute(httpRequest);
        HttpEntity entity = response.getEntity();
        return EntityUtils.toString(entity, DEFAULT_CHARSET);
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

        RequestConfig requestConfig = RequestConfig.custom()
                .setConnectTimeout(timeout * 1000)
                .setConnectionRequestTimeout(timeout * 1000)
                .setSocketTimeout(timeout * 1000)
                .build();
        httpClient = HttpClients.custom()
                .setDefaultRequestConfig(requestConfig)
                .setRetryHandler(HttpServiceRetryStrategy.retryStrategy).build();
    }

}
