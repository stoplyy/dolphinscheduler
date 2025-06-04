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

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.dolphinscheduler.alert.api.AlertData;
import org.apache.dolphinscheduler.alert.api.AlertResult;
import org.apache.dolphinscheduler.alert.api.HttpServiceRetryStrategy;
import org.apache.dolphinscheduler.common.enums.ListenerEventType;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.http.HttpEntity;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

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
                paramsMap.put("eventCode", tryGetHermesEventCode(eventType));
                paramsMap.put("eventOccurTime", tryGetEventStartTimg(eventMap));
                // paramsMap.put("templateKeyValue", eventMap);
                paramsMap.put("targetAppId", tryGetTargetAppId(eventMap));
                paramsMap.put("operate", tryGetOperate(eventMap));
                paramsMap.put("content", formatContent(eventMap, eventType));
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

    private String tryGetHermesEventCode(ListenerEventType listenerEventType) {
        if (listenerEventType == null) {
            return "stellarops-comm";
        }
        switch (listenerEventType) {
            case PROCESS_DEFINITION_CREATED:
                return "stressops-process-created";
            case PROCESS_DEFINITION_UPDATED:
                return "stressops-process-updated";
            case PROCESS_DEFINITION_DELETED:
                return "stressops-process-deleted";
            case PROCESS_START:
                return "stressops-process-start";
            case PROCESS_END:
                return "stressops-process-end";
            case PROCESS_FAIL:
                return "stressops-process-fail";
            case TASK_START:
                return "stressops-task-start";
            case TASK_END:
                return "stressops-task-end";
            case TASK_FAIL:
                return "stressops-task-fail";
            case SERVER_DOWN:
                return "stressops-server-down";
            default:
                return "unknown-event-type";
        }
    }

    private String formatContent(Map<String, String> eventMap, ListenerEventType eventType) {
        String operate = tryGetOperate(eventMap);
        String projectName = eventMap.getOrDefault("projectName", "");
        String taskName = eventMap.getOrDefault("taskName", "");
        String processName = eventMap.getOrDefault("processName", "");
        String processState = eventMap.getOrDefault("processState", "");
        String taskState = eventMap.getOrDefault("taskState", "");
        String taskStartTime = eventMap.getOrDefault("taskStartTime", "");
        String processStartTime = eventMap.getOrDefault("processStartTime", "");
        switch (eventType) {
            case PROCESS_DEFINITION_CREATED:
                return String.format("%s 操作人员在%s项目中创建了【%s】工作流.",
                        operate, projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_DEFINITION_UPDATED:
                return String.format("%s 操作人员在%s项目中更新了【%s】工作流.",
                        operate, projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_DEFINITION_DELETED:
                return String.format("%s 操作人员在%s项目中删除了【%s】工作流. ",
                        operate, projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_START:
                return String.format("%s 操作人员在%s项目中启动了【%s】工作流，时间：%s. all process msg: [%s]",
                        operate, projectName, processName, processStartTime, eventMap);
            case PROCESS_END:
                return String.format("%s 操作人员在%s项目中结束了【%s】工作流，工作流状态：%s，时间：%s. all process msg: [%s]",
                        operate, projectName, processName, processState, processStartTime, eventMap);
            case PROCESS_FAIL:
                return String.format("%s 操作人员在%s项目中执行的【%s】工作流失败，工作流状态：%s，时间：%s. all process msg: [%s]",
                        operate, projectName, processName, processState, processStartTime, eventMap);
            case TASK_START:
                return String.format("%s 操作人员在%s项目中执行的【%s】工作流，当前任务：【%s】启动，状态:【%s】，启动时间：%s. all task msg: [%s]",
                        operate, projectName, processName, taskName, taskState, taskStartTime, eventMap);
            case TASK_END:
                return String.format("%s 操作人员在%s项目中执行的【%s】-【%s】任务执行结束，状态【%s】。结束时间：%s. all task msg: [%s]",
                        operate, projectName, processName, taskName, taskState, taskStartTime, eventMap);
            case TASK_FAIL:
                return String.format("%s 操作人员在%s项目中执行的【%s】-【%s】任务执行失败，状态：【%s】，时间：%s. all task msg: [%s]",
                        operate, projectName, processName, taskName, taskState, taskStartTime, eventMap);
            case SERVER_DOWN:
                return String.format("Server down event occurred, operate: %s, project: %s, time: %s",
                        operate, projectName, taskStartTime);
            default:
                return String.format("Unknown event type: %s", eventType);
        }
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

    private String tryGetTargetAppId(Map<String, String> eventMap) {
        String targetAppId = "int-service-arch-stellarops-api";
        // ProjectAppId,ClusterAppId
        if (eventMap.containsKey("projectAppId")) {
            targetAppId = eventMap.get("projectAppId");
        } else if (eventMap.containsKey("clusterAppId")) {
            targetAppId = eventMap.get("clusterAppId");
        }
        if (StringUtils.isBlank(targetAppId)) {
            targetAppId = "int-service-arch-stellarops-api";
        }
        return targetAppId;
    }

    private Long tryGetEventStartTimg(Map<String, String> eventParamMap) throws ParseException {
        Long eventStartTime = System.currentTimeMillis();
        if (eventParamMap.containsKey("taskStartTime")) {
            // eventParamMap.get("taskStartTime")="2025-06-03 18:18:13"
            // eventParamMap.get("taskStartTime")="1717459093000"
            if (eventParamMap.get("taskStartTime").matches("\\d+")) {
                eventStartTime = Long.parseLong(eventParamMap.get("taskStartTime"));
            } else {
                eventStartTime = DateUtils.parseDate(eventParamMap.get("taskStartTime"), "yyyy-MM-dd HH:mm:ss")
                        .getTime();
            }
        } else if (eventParamMap.containsKey("processStartTime")) {
            if (eventParamMap.get("processStartTime").matches("\\d+")) {
                eventStartTime = Long.parseLong(eventParamMap.get("processStartTime"));
            } else {
                eventStartTime = DateUtils.parseDate(eventParamMap.get("processStartTime"), "yyyy-MM-dd HH:mm:ss")
                        .getTime();
            }
        }
        return eventStartTime;
    }

    public String tryGetOperate(Map<String, String> eventParamMap) {
        String operate = "";
        if (eventParamMap.containsKey("owner")) {
            operate = eventParamMap.get("owner");
        }
        if (StringUtils.isBlank(operate) && eventParamMap.containsKey("modifyBy")) {
            operate = eventParamMap.get("modifyBy");
        }
        if (StringUtils.isBlank(operate) && eventParamMap.containsKey("createBy")) {
            operate = eventParamMap.get("createBy");
        }
        if (StringUtils.isBlank(operate)) {
            operate = "none";
        }
        return operate;
    }

    /**
     * set body params
     */
    private void setMsgInRequestBody(Map<String, String> eventMap) {
        try {
            ObjectNode objectNode = JSONUtils.createObjectNode();
            objectNode.put("eventCode", eventCode);
            objectNode.put("eventOccurTime", tryGetEventStartTimg(eventMap));
            objectNode.put("templateKeyValue", JSONUtils.toJsonString(eventMap));
            objectNode.put("targetAppId", tryGetTargetAppId(eventMap));
            objectNode.put("operate", tryGetOperate(eventMap));
            objectNode.put("env", env);
            objectNode.put("workspaceId", 1);
            final String bodyString = JSONUtils.toJsonString(objectNode);
            log.info("http request body: {}", bodyString);
            StringEntity entity = new StringEntity(bodyString, DEFAULT_CHARSET);
            ((HttpPost) httpRequest).setEntity(entity);
        } catch (Exception e) {
            log.error("send http alert msg  exception : {}", e.getMessage());
        }
    }
}
