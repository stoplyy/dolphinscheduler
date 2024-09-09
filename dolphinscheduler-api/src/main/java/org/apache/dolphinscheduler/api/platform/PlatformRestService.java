package org.apache.dolphinscheduler.api.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.common.PlatformApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.common.RestParamEntry;
import org.apache.dolphinscheduler.api.utils.Result;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.tuhu.boot.common.facade.response.BizResponse;
import com.tuhu.stellarops.client.core.StellarOpsClusterInfo;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;
import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Slf4j
@Service
public class PlatformRestService {

    @Autowired
    AutoPlatformFactory autoPlatformFactory;

    public Result<Map<String, Object>> getPlatformRestWithUri(String restOrinUri, RestParamEntry params) {
        return getRestBizResponse(params.replaceNewString(restOrinUri));
    }

    public Result<Map<String, Object>> getPlatformRest(PathEnum pathEnum, String appIdOrUrl,
            RestParamEntry params) {

        StellarOpsOpenApiEndpoint dynamicClient = autoPlatformFactory.getClient(appIdOrUrl);

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
                result.setMessage("Unknown path: " + pathEnum);
        }

        return PlatformRestService.mapToResult(result);
    }

    public Result<List<StellarOpsClusterInfo>> getPlatformClusterListWithUri(String uri) {
        return getRestBizResponse(uri, StellarOpsClusterInfo.class);
    }

    public Result<List<StellarOpsClusterInfo>> getPlatformClusterList(String appIdOrUrl) {
        BizResponse<List<StellarOpsClusterInfo>> bizResponse = autoPlatformFactory
                .getClient(appIdOrUrl).getClusterList();
        return PlatformRestService.mapToResult(bizResponse);
    }

    public Result<List<StellarOpsNodeInfo>> getPlatformNodeListWithUri(String uri, RestParamEntry entry) {
        return getRestBizResponse(entry.replaceNewString(uri), StellarOpsNodeInfo.class);
    }

    public Result<List<StellarOpsNodeInfo>> getPlatformNodeList(String appIdOrUrl, RestParamEntry entry) {
        BizResponse<List<StellarOpsNodeInfo>> bizResponse = autoPlatformFactory
                .getClient(appIdOrUrl).getNodeList(entry.getClusterId(), entry.getTaskName());
        return PlatformRestService.mapToResult(bizResponse);
    }

    /*
     * 优先级：rest > baseUrl > appId
     */
    public Result<List<StellarOpsClusterInfo>> getClusterList(RestParamEntry entry) {
        return executePlatformRequest(entry, PathEnum.CLUSTER_LIST, (path) -> getPlatformClusterList(path));
    }

    public Result<List<StellarOpsNodeInfo>> getNodeList(RestParamEntry entry) {
        return executePlatformRequest(entry, PathEnum.NODE_LIST, (path) -> getPlatformNodeList(path, entry));
    }

    public Result<Map<String, Object>> getRest(RestParamEntry entry, PathEnum pathEnum) {
        return executePlatformRequest(entry, pathEnum, (path) -> getPlatformRest(pathEnum, path, entry));
    }

    private <T> Result<T> executePlatformRequest(
            RestParamEntry entry, PathEnum pathEnum, Function<String, Result<T>> serviceMethod) {
        if (entry.getRestPathMap().containsKey(pathEnum)) {
            if (PathEnum.CLUSTER_LIST.equals(pathEnum)) {
                return (Result<T>) getPlatformClusterListWithUri(entry.getRestPathMap().get(pathEnum));
            } else if (PathEnum.NODE_LIST.equals(pathEnum)) {
                return (Result<T>) getPlatformNodeListWithUri(entry.getRestPathMap().get(pathEnum), entry);
            } else {
                return (Result<T>) getPlatformRestWithUri(entry.getRestPathMap().get(pathEnum), entry);
            }
        } else if (StringUtils.isNotBlank(entry.getPlatformBaseUrl())) {
            return serviceMethod.apply(entry.getPlatformBaseUrl());
        } else if (StringUtils.isNotBlank(entry.getPlatformAppId())) {
            return serviceMethod.apply(entry.getPlatformAppId());
        } else {
            return Result.errorWithArgs(Status.PLATFORM_UNKNOW_PATH_ARGS, pathEnum + " rest not found");
        }
    }

    static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    static Request.Builder baseRequestBuilder = new Request.Builder()
            .addHeader("content-type", "application/json")
            .addHeader("Authorization", PlatformApolloConfigUtil.getStellarOpsPlatformAuthToken());

    public static Result<Map<String, Object>> getRestBizResponse(String rest) {
        try {

            BizResponse<Map<String, Object>> result = new BizResponse<>();
            Request request = baseRequestBuilder
                    .url(rest)
                    .get().build();

            // 发送请求并获取响应
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            if (responseBody != null) {
                // 将ResponseBody转换为BizResponse<Map<String, Object>>类型
                result = JSONUtils.toObject(responseBody.string(),
                        new TypeReference<BizResponse<Map<String, Object>>>() {
                        });
            } else {
                result.setCode(500);
                result.setMessage("responseBody is null");
            }

            return mapToResult(result);
        } catch (Exception ex) {
            log.error("http get error", ex);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS);
        }
    }

    public static <T> Result<List<T>> getRestBizResponse(String rest, Class<T> clazz) {
        Result<List<T>> result = new Result<>();
        try {

            Request request = baseRequestBuilder
                    .url(rest)
                    .get().build();

            // 发送请求并获取响应
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();

            Map resultMap = JSONUtils.toObject(responseBody.string(), Map.class);

            // 使用新的静态方法将map转换为result
            mapToResult(result, resultMap, clazz);
        } catch (Exception ex) {
            log.error("http get error", ex);
            return Result.error(Status.INTERNAL_SERVER_ERROR_ARGS);
        }

        return result;
    }

    // 新的静态方法
    public static <T> Result<T> mapToResult(BizResponse<T> bizResponse) {
        Result<T> result = new Result<>();
        Integer code = bizResponse.getCode();
        if (code == 10000) { // 10000是成功的code
            result.setCode(Status.SUCCESS.getCode());
        } else {
            result.setCode(code);
        }
        result.setMsg(bizResponse.getMessage());
        result.setData(bizResponse.getData());
        return result;
    }

    // 新的静态方法
    public static <T> void mapToResult(Result<List<T>> result, Map resultMap, Class<T> clazz) {
        Integer code = (Integer) resultMap.get("code");
        if (code == 10000) { // 10000是成功的code
            result.setCode(Status.SUCCESS.getCode());
        } else {
            result.setCode(code);
        }
        result.setMsg((String) resultMap.get("message"));
        if (resultMap.containsKey("data")) {
            List<T> list = JSONUtils.toObject(JSONUtils.toJson(resultMap.get("data")),
                    TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
            result.setData(list);
        }
    }

}
