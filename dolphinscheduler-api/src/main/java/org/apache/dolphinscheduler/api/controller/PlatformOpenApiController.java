package org.apache.dolphinscheduler.api.controller;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.platform.AutoPlatformFactory;
import org.apache.dolphinscheduler.api.platform.PathEnum;
import org.apache.dolphinscheduler.api.platform.PlatformRestServiceImpl;
import org.apache.dolphinscheduler.api.platform.common.ApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.facade.PlatformOpenApi;
import org.apache.dolphinscheduler.api.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.tuhu.boot.common.facade.response.BizResponse;
import com.tuhu.stellarops.client.core.StellarOpsClusterInfo;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;
import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;

@Tag(name = "TUHU-PLATFORM-API", description = "Tuhu platform api")
@RequestMapping("/platform")
@SwaggerDefinition
@RestController
@Slf4j
public class PlatformOpenApiController implements PlatformOpenApi {

    @Autowired
    AutoPlatformFactory autoPlatformFactory;

    public class RestParamEntry {
        String clusterId;
        String nodeId;

        String taskName;

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
    }

    private Result<Map<String, Object>> getResponseWithAppId(String rest, String appId,
            RestParamEntry params) {

        StellarOpsOpenApiEndpoint dynamicClient = autoPlatformFactory.getClient(appId);

        PathEnum pathEnum = PathEnum.fromPath(rest);
        BizResponse<Map<String, Object>> result = null;
        switch (pathEnum) {
            case CLUSTER_PARAMS:
                result = dynamicClient.getClusterCommParam(params.clusterId, params.taskName);
            case NODE_PARAMS:
                result = dynamicClient.getNodeCommParam(params.clusterId, params.nodeId, params.taskName);
            case TASK_PARAMS:
                result = dynamicClient.getTaskCommParam(params.clusterId, params.taskName);
            case ENV_CHECK:
                result = dynamicClient.checkEnv();
                // 添加其他路径的处理...
            default:
                result = new BizResponse<>();
                result.setCode(Status.PLATFORM_UNKNOW_PATH_ARGS.getCode());
                result.setMessage("Unknown path: " + rest);
        }

        return PlatformRestServiceImpl.mapToResult(result);
    }

    @Override
    public Result<List<StellarOpsClusterInfo>> getPlatformClusterList(@PathVariable String platform) {
        String config = ApolloConfigUtil.getPlatformConfig(platform);
        String rest = PathEnum.CLUSTER_LIST.getPath();
        Map<String, Object> configMap = new HashMap<>();

        RestParamEntry entry = new RestParamEntry().build(null, null, null);
        try {
            configMap = JSONUtils.toObject(config, Map.class);
            if (configMap == null) {
                configMap = new HashMap<>();
            }
            if (configMap.containsKey(rest)) {
                String restUri = (String) configMap.get(rest);
                return PlatformRestServiceImpl.getRestBizResponse(entry.replaceNewString(restUri),
                        StellarOpsClusterInfo.class);
            } else if (configMap.containsKey("baseHost")) {
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestServiceImpl.getRestBizResponse(
                        restUri + "/stellarops/cluster/list",
                        StellarOpsClusterInfo.class);
            } else if (configMap.containsKey("appId")) {
                BizResponse<List<StellarOpsClusterInfo>> bizResponse = autoPlatformFactory
                        .getClient((String) configMap.get("appId"))
                        .getClusterList();
                return PlatformRestServiceImpl.mapToResult(bizResponse);
            } else {
                log.error(platform + "getPlatformRest error, rest not found");
                return Result.errorWithArgs(Status.PLATFORM_UNKNOW_PATH_ARGS, "rest not found");
            }
        } catch (Exception e) {
            log.error(platform + " getPlatformRest Exception", e);
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, "getPlatformRest Exception");
        }
    }

    @Override
    public Result<List<StellarOpsNodeInfo>> getPlatformNodeList(@PathVariable String platform,
            @PathVariable String clusterId,
            @RequestParam(required = false) String taskName) {
        String config = ApolloConfigUtil.getPlatformConfig(platform);
        String rest = PathEnum.NODE_LIST.getPath();
        Map<String, Object> configMap = new HashMap<>();
        RestParamEntry entry = new RestParamEntry().build(null, null, null);
        try {
            configMap = JSONUtils.toObject(config, Map.class);
            if (configMap == null) {
                configMap = new HashMap<>();
            }
            if (configMap.containsKey(rest)) {
                String restUri = (String) configMap.get(rest);
                return PlatformRestServiceImpl.getRestBizResponse(entry.replaceNewString(restUri),
                        StellarOpsNodeInfo.class);
            } else if (configMap.containsKey("baseHost")) {
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestServiceImpl.getRestBizResponse(
                        entry.replaceNewString(restUri) +
                                "/stellarops/node/list/" + clusterId + "?taskName=" + taskName,
                        StellarOpsNodeInfo.class);
            } else if (configMap.containsKey("appId")) {
                BizResponse<List<StellarOpsNodeInfo>> bizResponse = autoPlatformFactory
                        .getClient((String) configMap.get("appId")).getNodeList(clusterId, taskName);
                return PlatformRestServiceImpl.mapToResult(bizResponse);
            } else {
                log.error(platform + " getPlatformRest error, rest not found");
                return Result.errorWithArgs(Status.PLATFORM_UNKNOW_PATH_ARGS, "rest not found");
            }
        } catch (Exception e) {
            log.error(platform + " getPlatformRest Exception", e);
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, "getPlatformRest Exception");
        }
    }

    @Override
    public Result<Map<String, Object>> getPlatformRest(
            @PathVariable String rest,
            @PathVariable String platform,
            @RequestParam(required = false) String clusterId, @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String taskName) {
        String config = ApolloConfigUtil.getPlatformConfig(platform);

        Map<String, Object> configMap = new HashMap<>();
        RestParamEntry entry = new RestParamEntry().build(clusterId, nodeId, taskName);
        try {
            configMap = JSONUtils.toObject(config, Map.class);
            if (configMap == null) {
                configMap = new HashMap<>();
            }
            if (configMap.containsKey(rest)) {
                String restUri = (String) configMap.get(rest);
                return PlatformRestServiceImpl.getRestBizResponse(entry.replaceNewString(restUri));
            } else if (configMap.containsKey("baseHost")) {
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestServiceImpl.getRestBizResponse(entry.replaceNewString(restUri));
            } else if (configMap.containsKey("appId")) {
                return getResponseWithAppId(rest, (String) configMap.get("appId"), entry);
            } else {
                log.error(platform + " getPlatformRest error, rest not found");
                return Result.errorWithArgs(Status.PLATFORM_UNKNOW_PATH_ARGS, "rest not found");
            }
        } catch (Exception e) {
            log.error(platform + " getPlatformRest Exception", e);
            return Result.errorWithArgs(Status.INTERNAL_SERVER_ERROR_ARGS, "getPlatformRest Exception");
        }
    }

}
