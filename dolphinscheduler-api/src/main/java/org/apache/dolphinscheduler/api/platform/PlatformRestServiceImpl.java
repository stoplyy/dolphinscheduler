package org.apache.dolphinscheduler.api.platform;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

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

@Slf4j
public class PlatformRestServiceImpl {

    static OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    static Request.Builder baseRequestBuilder = new Request.Builder()
            .addHeader("content-type", "application/json")
            .addHeader("Authorization", ApolloConfigUtil.getStellarOpsPlatformAuthToken());

    public static BizResponse<Map<String, Object>> getRestBizResponse(String rest) {
        BizResponse<Map<String, Object>> result = new BizResponse<>();
        try {

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
        } catch (Exception ex) {
            log.error("http get error", ex);
            return BizResponse.operationFailed("http get error: " + rest);
        }

        return result;
    }

    public static <T> BizResponse<List<T>> getRestBizResponse(String rest, Class<T> clazz) {
        BizResponse<List<T>> result = new BizResponse<>();
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
            return BizResponse.operationFailed("http get error: " + rest);
        }

        return result;
    }

    // 新的静态方法
    public static <T> void mapToResult(BizResponse<List<T>> result, Map resultMap, Class<T> clazz) {
        result.setCode((Integer) resultMap.get("code"));
        result.setMessage((String) resultMap.get("message"));
        if (resultMap.containsKey("data")) {
            List<T> list = JSONUtils.toObject(JSONUtils.toJson(resultMap.get("data")),
                    TypeFactory.defaultInstance().constructCollectionType(List.class, clazz));
            result.setData(list);
        }
    }

}
