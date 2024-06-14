package org.apache.dolphinscheduler.api.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.platform.common.ApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.tuhu.boot.common.facade.response.BizResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import org.apache.dolphinscheduler.api.utils.Result;

@Slf4j
public class PlatformRestServiceImpl {

    static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    static Request.Builder baseRequestBuilder = new Request.Builder()
            .addHeader("content-type", "application/json")
            .addHeader("Authorization", ApolloConfigUtil.getStellarOpsPlatformAuthToken());

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
