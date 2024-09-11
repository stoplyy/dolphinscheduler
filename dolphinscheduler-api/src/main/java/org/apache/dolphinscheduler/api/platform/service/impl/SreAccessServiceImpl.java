package org.apache.dolphinscheduler.api.platform.service.impl;

import java.io.IOException;
import java.util.HashMap;
import java.util.concurrent.TimeUnit;

import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.common.PlatformApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.service.SreAccessService;
import org.apache.dolphinscheduler.common.config.ApolloConfigUtil;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

@Service
@Slf4j
public class SreAccessServiceImpl implements SreAccessService {

    OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();

    Request.Builder baseRequestBuilder = new Request.Builder()
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            .addHeader("X-SRE-SEC", PlatformApolloConfigUtil.getSreAuthToken());

    /**
     * 给机器贴上公钥
     * 
     * @param ip
     * @return
     * @throws IOException
     */
    @Override
    public String pastePubRsa(String ip) {
        String res = "";
        try {
            String postBody = String.format(
                    "{\"action\":\"add\", \"auth_name\": \"pubkey-middleware01\", \"hosts\": [\"%s\"]}", ip);

            RequestBody requestBody = RequestBody.create(MediaType.parse("application/json"), postBody);

            Request request = baseRequestBuilder
                    .url(PlatformApolloConfigUtil.getSreApiUrl() + "/api/post-auth-host-key-operate")
                    .post(requestBody).build();

            // 发送请求并获取响应
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            String responseStr = "";
            try {
                responseStr = responseBody.string(); // Internal Decode
            } catch (IOException e) {
                log.error("SoaUtils.pastePubRsa failed, {}", ip, e);
                throw e;
            }
            // 正常返回结果：状态码==200且code==20000
            // {"code":20000,"data":"异步操作中,1分钟内完成,如有问题请联系管理员,异步任务ID为:T1681880880262095000N230","message":""}
            HashMap hashMap = JSONUtils.toObject(responseStr, HashMap.class);
            log.info("SoaUtils.pastePubRsa finish, ip:{} response:{}", ip, responseStr);

            if (hashMap.containsKey("code") && "20000".equals(hashMap.get("code").toString())) {
                return "success";
            }
            res = hashMap.get("message").toString();
            // 异常返回结果：状态码!=200或code!=20000
            // {"code":20999,"data":"","message":"以下主机IP未被授权，请确认:10.3.252.168,10.3.252.169,10.3.252.17,10.3.252.170"}
            log.error("SoaUtils.pastePubRsa failed, ip:{} response:{}", ip, responseStr);
        } catch (Exception ex) {
            log.error("SoaUtils.pastePubRsa failed, {}", ip, ex);
        }

        return res;
    }

    @Override
    public String syncSREByAppid(String appId) {
        String res = "";
        Request request = baseRequestBuilder
                .url(PlatformApolloConfigUtil.getSreApiUrl() + "/api/db_sync_by_appid?appid=" + appId)
                .get().build();

        String responseStr = "{}";

        // 发送请求并获取响应
        try {
            Response response = client.newCall(request).execute();
            ResponseBody responseBody = response.body();
            responseStr = responseBody.string(); // Internal Decode
        } catch (Exception e) {
            log.error("SoaUtils.syncSREByAppid failed, {}", appId, e);
            return "request exception";
        }

        // 正常返回结果：状态码==200且code==20000
        // {"code":20000,"data":"同步完成","message":""}
        HashMap hashMap = JSONUtils.toObject(responseStr, HashMap.class);
        log.info("SoaUtils.syncSREByAppid finish, appId:{} response:{}", appId, responseStr);

        if (hashMap.containsKey("code") && "20000".equals(hashMap.get("code").toString())) {
            return "success";
        }
        res = hashMap.get("message").toString();
        // 异常返回结果：状态码!=200或code!=20000
        // {"code":20999,"data":"","message":"以下主机IP未被授权，请确认:10.3.252.168,10.3.252.169,10.3.252.17,10.3.252.170"}
        log.error("SoaUtils.syncSREByAppid failed, appId:{} response:{}", appId, responseStr);
        return res;
    }

}
