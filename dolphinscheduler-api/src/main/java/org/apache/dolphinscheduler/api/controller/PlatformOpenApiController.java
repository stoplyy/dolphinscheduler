package org.apache.dolphinscheduler.api.controller;

import static org.apache.dolphinscheduler.api.enums.Status.QUERY_PROJECT_DETAILS_BY_CODE_ERROR;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.platform.AutoPlatformFactory;
import org.apache.dolphinscheduler.api.platform.PathEnum;
import org.apache.dolphinscheduler.api.platform.PlatformRestService;
import org.apache.dolphinscheduler.api.platform.common.ApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.common.PlatformConstant;
import org.apache.dolphinscheduler.api.platform.common.RestParamEntry;
import org.apache.dolphinscheduler.api.platform.facade.PlatformOpenApi;
import org.apache.dolphinscheduler.api.service.ProjectParameterService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.Project;
import org.apache.dolphinscheduler.dao.entity.ProjectParameter;
import org.apache.dolphinscheduler.dao.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.tuhu.boot.common.facade.response.BizResponse;
import com.tuhu.dolphin.common.JsonUtils;
import com.tuhu.stellarops.client.core.StellarOpsClusterInfo;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;
import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

import io.swagger.annotations.SwaggerDefinition;
import io.swagger.v3.oas.annotations.Parameter;
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

    @Autowired
    private ProjectParameterService projectParameterService;

    @Autowired
    ProjectService projectService;

    @Autowired
    PlatformRestService platformRestService;

    private Result<Map<String, Object>> getResponseWithAppId(String rest, String appId,
            RestParamEntry params) {

        StellarOpsOpenApiEndpoint dynamicClient = autoPlatformFactory.getClient(appId);

        PathEnum pathEnum = PathEnum.fromPath(rest);
        BizResponse<Map<String, Object>> result = null;
        switch (pathEnum) {
            case CLUSTER_PARAMS:
                result = dynamicClient.getClusterCommParam(params.getClusterId(), params.getTaskName());
                break;
            case NODE_PARAMS:
                result = dynamicClient.getNodeCommParam(params.getClusterId(), params.getNodeId(),
                        params.getTaskName());
                break;
            case TASK_PARAMS:
                result = dynamicClient.getTaskCommParam(params.getClusterId(), params.getTaskName());
                break;
            case ENV_CHECK:
                result = dynamicClient.checkEnv();
                break;
            // 添加其他路径的处理...
            default:
                result = new BizResponse<>();
                result.setCode(Status.PLATFORM_UNKNOW_PATH_ARGS.getCode());
                result.setMessage("Unknown path: " + rest);
        }

        return PlatformRestService.mapToResult(result);
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
                return PlatformRestService.getRestBizResponse(entry.replaceNewString(restUri),
                        StellarOpsClusterInfo.class);
            } else if (configMap.containsKey("baseHost")) {
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestService.getRestBizResponse(
                        restUri + "/stellarops/cluster/list",
                        StellarOpsClusterInfo.class);
            } else if (configMap.containsKey("appId")) {
                BizResponse<List<StellarOpsClusterInfo>> bizResponse = autoPlatformFactory
                        .getClient((String) configMap.get("appId"))
                        .getClusterList();
                return PlatformRestService.mapToResult(bizResponse);
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

        RestParamEntry entry = new RestParamEntry().build(clusterId, null, taskName);
        try {
            configMap = JSONUtils.toObject(config, Map.class);
            if (configMap == null) {
                configMap = new HashMap<>();
            }
            if (configMap.containsKey(rest)) {
                String restUri = (String) configMap.get(rest);
                return PlatformRestService.getRestBizResponse(entry.replaceNewString(restUri),
                        StellarOpsNodeInfo.class);
            } else if (configMap.containsKey("baseHost")) {
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestService.getRestBizResponse(
                        entry.replaceNewString(restUri) +
                                "/stellarops/node/list/" + clusterId + "?taskName=" + taskName,
                        StellarOpsNodeInfo.class);
            } else if (configMap.containsKey("appId")) {
                BizResponse<List<StellarOpsNodeInfo>> bizResponse = autoPlatformFactory
                        .getClient((String) configMap.get("appId"))
                        .getNodeList(entry.getClusterId(), entry.getTaskName());
                return PlatformRestService.mapToResult(bizResponse);
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
                return PlatformRestService.getRestBizResponse(entry.replaceNewString(restUri));
            } else if (configMap.containsKey("baseHost")) {
                // TODO: 根据rest 替换参数，当前没有替换，无法使用
                String restUri = (String) configMap.get("baseHost");
                return PlatformRestService.getRestBizResponse(entry.replaceNewString(restUri));
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

    @GetMapping("{projectCode}/clusterlist")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_DETAILS_BY_CODE_ERROR)
    public Result<List<StellarOpsClusterInfo>> queryClusterListByPlatform(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode) {

        Result<Project> pj = projectService.queryByCode(loginUser, projectCode);
        if (!pj.isSuccess()) {
            return Result.errorWithArgs(Status.QUERY_PROJECT_DETAILS_BY_CODE_ERROR, "project not found");
        }
        projectService.checkHasProjectWritePermissionThrowException(loginUser, pj.getData());

        Result<PageInfo<ProjectParameter>> platformParamList = projectParameterService.queryProjectParameterListPaging(
                loginUser, projectCode, 100, 1, PlatformConstant.PLATFORM_PARAM_PRIFEX);

        RestParamEntry entry = RestParamEntry.newEntry()
                .buildRestParamEntiy(platformParamList.getData().getTotalList());
        Result<List<StellarOpsClusterInfo>> list = platformRestService.getClusterList(entry);

        if (list.isSuccess()) {
            return list;
        } else {
            log.warn("Project:{} cluster list not found.", pj.getData().getName());
            return Result.success(new ArrayList<>());
        }
    }

    @GetMapping("{projectCode}/nodelist/{clusterId}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_DETAILS_BY_CODE_ERROR)
    public Result<List<StellarOpsNodeInfo>> queryNodeListByPlatform(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable String clusterId) {

        Result<Project> pj = projectService.queryByCode(loginUser, projectCode);

        if (!pj.isSuccess()) {
            return Result.errorWithArgs(Status.QUERY_PROJECT_DETAILS_BY_CODE_ERROR, "project not found");
        }
        projectService.checkHasProjectWritePermissionThrowException(loginUser, pj.getData());

        Result<PageInfo<ProjectParameter>> platformParamList = projectParameterService.queryProjectParameterListPaging(
                loginUser, projectCode, 100, 1, PlatformConstant.PLATFORM_PARAM_PRIFEX);

        RestParamEntry entry = RestParamEntry.newEntry()
                .build(clusterId + "", null, null)
                .buildRestParamEntiy(platformParamList.getData().getTotalList());

        Result<List<StellarOpsNodeInfo>> list = platformRestService.getNodeList(entry);

        if (list.isSuccess()) {
            return list;
        } else {
            log.warn("Project:{} cluster:{} node list not found.", pj.getData().getName(), clusterId);
            return Result.success(new ArrayList<>());
        }
    }

    @GetMapping("{projectCode}/rest")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_DETAILS_BY_CODE_ERROR)
    public Result<Map<String, Object>> restQuery(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @RequestParam(required = true) String rest,
            @RequestParam(required = false) String clusterId,
            @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String taskName) {

        final Result<Project> pj = projectService.queryByCode(loginUser, projectCode);
        if (!pj.isSuccess()) {
            return Result.errorWithArgs(Status.QUERY_PROJECT_DETAILS_BY_CODE_ERROR, "project not found");
        }

        final PathEnum path = PathEnum.fromPath(rest);
        if (path == PathEnum.NONE) {
            return Result.errorWithArgs(Status.PLATFORM_UNKNOW_PATH_ARGS, "rest not found");
        }

        projectService.checkHasProjectWritePermissionThrowException(loginUser, pj.getData());

        Result<PageInfo<ProjectParameter>> platformParamList = projectParameterService.queryProjectParameterListPaging(
                loginUser, projectCode, 100, 1, PlatformConstant.PLATFORM_PARAM_PRIFEX);

        RestParamEntry entry = RestParamEntry.newEntry()
                .build(clusterId, nodeId, taskName)
                .buildRestParamEntiy(platformParamList.getData().getTotalList());

        Result<Map<String, Object>> result = platformRestService.getRest(entry, path);
        if (result.isSuccess()) {
            return result;
        } else {
            log.warn("request rest:{} failed. result:{}", rest, JsonUtils.toJson(result));
            return Result.success(new HashMap<>());
        }
    }
}
