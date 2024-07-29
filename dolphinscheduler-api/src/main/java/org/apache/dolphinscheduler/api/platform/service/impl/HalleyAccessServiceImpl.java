package org.apache.dolphinscheduler.api.platform.service.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.dolphinscheduler.api.platform.common.EnvironmentUtil;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.common.PlatformApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.dto.halley.AppIdInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.AssetsInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.DepartmentByAppIdDto;
import org.apache.dolphinscheduler.api.platform.dto.halley.HalleyServerInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.SimpleDepartmentDto;
import org.apache.dolphinscheduler.api.platform.dto.halley.UserInfoDto;
import org.apache.dolphinscheduler.api.platform.dto.halley.UserInfoHalley;
import org.apache.dolphinscheduler.api.platform.service.HalleyAccessService;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.tuhu.boot.common.facade.response.BizResponse;

import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * @Date: 2021-03-25 14:50:53
 * @LastEditors: Liuyangyang
 * @LastEditTime: 2021-03-25 14:56:57
 **/
@Slf4j
@Service
public class HalleyAccessServiceImpl implements HalleyAccessService {

    OkHttpClient client = new OkHttpClient().newBuilder().connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS).readTimeout(30, TimeUnit.SECONDS).build();
    Request.Builder baseRequestBuilder = new Request.Builder()
            .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
            .addHeader("Authorization", PlatformApolloConfigUtil.getHalleyAuthToken());

    @Override
    public List<SimpleDepartmentDto> getDepartmentTreeList() {
        try {
            Request request = baseRequestBuilder
                    .url(PlatformApolloConfigUtil.getHalleyApiUrl() + "/auth/departments/tree")
                    .get().build();

            Response response = client.newCall(request).execute();
            BizResponse result = JSONUtils.toObject(response.body().string(),
                    new TypeReference<BizResponse<List<SimpleDepartmentDto>>>() {
                    });
            return (List<SimpleDepartmentDto>) result.getData();
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return null;
    }

    @Override
    public List<SimpleDepartmentDto> getChildDepartmentById(Integer departmentId) {
        try {
            Request request = baseRequestBuilder
                    .url(PlatformApolloConfigUtil.getHalleyApiUrl() + "/auth/departments?parent_id=" + departmentId)
                    .get()
                    .build();

            Response response = client.newCall(request).execute();
            BizResponse result = JSONUtils.toObject(response.body().string(),
                    new TypeReference<BizResponse<List<SimpleDepartmentDto>>>() {
                    });
            // log.info("send halley tree res {}", response.isSuccessful());
            return (List<SimpleDepartmentDto>) result.getData();
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return null;
    }

    @Override
    public UserInfoDto getUserByHalley(String userName) {
        try {
            Request request = baseRequestBuilder
                    .url(PlatformApolloConfigUtil.getHalleyApiUrl() + "/auth/users?username=" + userName).get().build();

            Response response = client.newCall(request).execute();
            String str = response.body().string();

            BizResponse<UserInfoHalley> result = JSONUtils.toObject(str,
                    new TypeReference<BizResponse<UserInfoHalley>>() {
                    });
            if (result.getData() != null) {
                List<UserInfoDto> list = result.getData().getData();
                return list == null ? null : list.get(0);
            }
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return null;
    }

    @Override
    public List<AssetsInfo> getAssetsInfoByAppId(String appid) {
        try {

            Request request = baseRequestBuilder.url(
                    PlatformApolloConfigUtil.getHalleyApiUrl() + "/appid/list?app_id=" + appid
                            + "&page_num=1&page_size=100")
                    .get().build();

            Response response = client.newCall(request).execute();
            String responseStr = response.body().string();
            Map m = JSONUtils.toObject(responseStr, Map.class);
            String dataStr = JSONUtils.toJson(((ArrayList) ((Map) m.get("data")).get("data")).get(0));
            AppIdInfo appIds = JSONUtils.toObject(dataStr, AppIdInfo.class);
            String baseEnv = EnvironmentUtil.getEnvironment();
            final String env = "test".equals(baseEnv) ? "tuhutest" : baseEnv;
            return appIds.getAssets().stream().filter(i -> i.getEnv().equals(env)).collect(Collectors.toList());
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return null;
    }

    @Override
    public List<HalleyServerInfo> getAssetsInfoByIps(List<String> ips) {
        try {

            Request request = baseRequestBuilder
                    .url(PlatformApolloConfigUtil.getHalleyApiUrl() + "/cmdb/servers?ips=" + String.join(",", ips))
                    .get().build();

            Response response = client.newCall(request).execute();
            String str = response.body().string();

            BizResponse<List<HalleyServerInfo>> result = JSONUtils.toObject(str,
                    new TypeReference<BizResponse<List<HalleyServerInfo>>>() {
                    });
            List<HalleyServerInfo> list = result.getData();
            return list;
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return null;
    }

    @Override
    public List<String> getDepartList(String userName) {
        List<String> departList = new ArrayList<>();

        UserInfoDto halleyUser = getUserByHalley(userName);
        List<SimpleDepartmentDto> child = getChildDepartmentById(halleyUser.getDepartmentId());
        departList.add(halleyUser.getDepartmentInfo().getHumanName());
        if (child != null) {
            child.forEach(i -> {
                departList.addAll(getDepartName(i));
            });
        }
        return departList;
    }

    private List<String> getDepartName(SimpleDepartmentDto departmentDto) {
        List<String> departList = new ArrayList<>();
        departList.add(departmentDto.getHumanName());
        if (departmentDto.getChildren() != null) {
            departmentDto.getChildren().forEach(i -> {
                departList.addAll(getDepartName(i));
            });
        }
        return departList;
    }

    @Override
    public List<DepartmentByAppIdDto> getdepartmentInfoByAppId(List<String> appIdList) {
        List<DepartmentByAppIdDto> result = new ArrayList<>();
        try {
            int size = 20;
            for (int i = 0; i < appIdList.size();) {
                int fromindex = i;
                i += size;
                int toIndex = Math.min(appIdList.size(), i);

                Request.Builder baseRequestBuilder = new Request.Builder()
                        .addHeader("Content-Type", "application/x-www-form-urlencoded; charset=utf-8")
                        .addHeader("Authorization",
                                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjozMDc2LCJ1c2VyX25hbWUiOiJodWJibGUiLCJuYW1lIjoiaHViYmxlXHU1ZTczXHU1M2YwdG9rZW5cdTc1MjhcdTYyMzciLCJpc19zdXBlcnVzZXIiOjAsInN1YiI6MTY0NjgxMTk5MiwiZXhwIjoyMjc3NTMxOTkyfQ.rG6Y3teFby1ftbhLBTs4jkumZ_l8N4Tp_tzBO6aMMtU");

                Request request = baseRequestBuilder
                        .url(PlatformApolloConfigUtil.getHalleyApiBaseUrl() + "/third_api/v1/appids?app_ids="
                                + String.join(",", appIdList.subList(
                                        fromindex,
                                        toIndex)))
                        .get().build();

                Response response = client.newCall(request).execute();
                String source = response.body().string();
                Map m = JSONUtils.toObject(source, Map.class);
                String dataStr = JSONUtils.toJson(((ArrayList) ((Map) m.get("data")).get("data")));

                List<DepartmentByAppIdDto> tmp = JSONUtils.toObject(
                        dataStr,
                        new TypeReference<List<DepartmentByAppIdDto>>() {
                        });

                result.addAll(tmp);
            }
        } catch (Exception e) {
            log.error("send error. ", e);
        }
        return result;
    }

}
