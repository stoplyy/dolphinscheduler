package org.apache.dolphinscheduler.plugin.alert.comm;

import java.text.ParseException;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.apache.dolphinscheduler.common.enums.ListenerEventType;

public class AlertDataExpHelper {

    public static String tryGetTargetAppId(Map<String, String> eventMap) {
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

    public static Long tryGetEventStartTimg(Map<String, String> eventParamMap) throws ParseException {
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

    public static String tryGetOperate(Map<String, String> eventParamMap) {
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

    public static String tryGetHermesEventCode(ListenerEventType listenerEventType) {
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

    public static String tryGetProjectName(Map<String, String> eventParamMap) {
        return eventParamMap.getOrDefault("projectName", "unknown-project");
    }

    public static String tryGetProcessName(Map<String, String> eventParamMap) {
        return eventParamMap.getOrDefault("processName", "unknown-process");
    }

    public static String formatTiltle(Map<String, String> eventMap, ListenerEventType eventType) {
        String operate = tryGetOperate(eventMap);
        String projectName = tryGetProjectName(eventMap);
        String processName = tryGetProcessName(eventMap);
        String taskName = eventMap.getOrDefault("taskName", "");
        switch (eventType) {
            case PROCESS_DEFINITION_CREATED:
                return String.format("【%s】项目工作流【%s】 创建成功！", projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_DEFINITION_UPDATED:
                return String.format("【%s】项目工作流【%s】更新成功！", projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_DEFINITION_DELETED:
                return String.format("【%s】项目工作流【%s】删除成功！", projectName, eventMap.getOrDefault("name", "none"));
            case PROCESS_START:
                return String.format("【%s】项目工作流【%s】启动成功！", projectName, processName);
            case PROCESS_END:
                return String.format("【%s】项目工作流【%s】结束成功！", projectName, processName);
            case PROCESS_FAIL:
                return String.format("【%s】项目工作流【%s】执行失败！", projectName, processName);
            case TASK_START:
                return String.format("【%s】项目工作流【%s】- 任务-【%s】启动成功！", projectName, processName, taskName);
            case TASK_END:
                return String.format("【%s】项目工作流【%s】- 任务-【%s】执行结束！", projectName, processName, taskName);
            case TASK_FAIL:
                return String.format("【%s】项目工作流【%s】- 任务-【%s】执行失败！", projectName, processName, taskName);
            case SERVER_DOWN:
                return String.format("Server down event occurred, operate: %s, project: %s",
                        operate, projectName);
            default:
                return String.format("Unknown event type: %s", eventType);
        }
    }

    public static String formatContent(Map<String, String> eventMap, ListenerEventType eventType) {
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
}
