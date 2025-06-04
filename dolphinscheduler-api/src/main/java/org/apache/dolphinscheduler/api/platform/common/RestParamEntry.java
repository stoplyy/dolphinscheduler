package org.apache.dolphinscheduler.api.platform.common;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dolphinscheduler.api.platform.PathEnum;
import org.apache.dolphinscheduler.common.constants.PlatformConstant;
import org.apache.dolphinscheduler.dao.entity.ProjectParameter;

import lombok.Data;

@Data
public class RestParamEntry {

    String platformName;
    String platformAppId;
    String platformBaseUrl;

    String clusterId;
    String nodeId;
    String taskName;

    final Map<PathEnum, String> restPathMap = new HashMap<PathEnum, String>(PathEnum.values().length);

    public static RestParamEntry newEntry() {
        return new RestParamEntry();
    }

    public RestParamEntry build(String clusterId, String nodeId, String taskName) {
        this.clusterId = clusterId == null ? "0" : clusterId;
        this.nodeId = nodeId == null ? "0" : nodeId;
        this.taskName = taskName == null ? "empty" : taskName;
        return this;
    }

    public RestParamEntry build(Map<String, Object> params) {
        this.clusterId = (String) params.getOrDefault("clusterId", 0);
        this.nodeId = (String) params.getOrDefault("nodeId", 0);
        this.taskName = (String) params.getOrDefault("taskName", "empty");
        return this;
    }

    public String replaceNewString(String restUri) {
        String result = restUri;
        result = result.replaceAll("\\{clusterId\\}", clusterId == null ? "0" : clusterId);
        result = result.replaceAll("\\{nodeId\\}", nodeId == null ? "0" : nodeId);
        result = result.replaceAll("\\{taskName\\}", taskName == null ? "empty" : taskName);
        return result;
    }

    public void putRestPath(PathEnum pathEnum, String restPath) {
        restPathMap.put(pathEnum, restPath);
    }

    public RestParamEntry buildRestParamEntiy(List<ProjectParameter> platformParamList) {
        RestParamEntry entry = this;

        List<ProjectParameter> inpuParameters = platformParamList == null ? new ArrayList<>() : platformParamList;
        for (ProjectParameter param : inpuParameters) {
            if (PlatformConstant.PLATFORM_PARAM_NAME.equalsIgnoreCase(param.getParamName())) {
                entry.setPlatformName(param.getParamValue());
            }
            if (PlatformConstant.PLATFORM_PARAM_APP_ID.equalsIgnoreCase(param.getParamName())) {
                entry.setPlatformAppId(param.getParamValue());
            }
            if (PlatformConstant.PLATFORM_PARAM_BASE_URL.equalsIgnoreCase(param.getParamName())) {
                entry.setPlatformBaseUrl(param.getParamValue());
            }

            for (PathEnum pathEnum : PathEnum.values()) {
                String fullPath = PlatformConstant.PLATFORM_PARAM_PRIFEX.concat(pathEnum.getPath());
                if (fullPath.equalsIgnoreCase(param.getParamName())) {
                    entry.putRestPath(pathEnum, param.getParamValue());
                    break;
                }
            }
        }
        return entry;
    }

}
