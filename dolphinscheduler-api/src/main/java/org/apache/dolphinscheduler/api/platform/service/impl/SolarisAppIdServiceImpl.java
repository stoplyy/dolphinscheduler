package org.apache.dolphinscheduler.api.platform.service.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.platform.common.EnvironmentUtil;
import org.apache.dolphinscheduler.api.platform.common.JSONUtils;
import org.apache.dolphinscheduler.api.platform.common.PlatformApolloConfigUtil;
import org.apache.dolphinscheduler.api.platform.dto.solaris.AppIdDTO;
import org.apache.dolphinscheduler.api.platform.dto.solaris.AppRole;
import org.apache.dolphinscheduler.api.platform.dto.solaris.SolarisUserApp;
import org.apache.dolphinscheduler.api.platform.dto.solaris.SolarisUserAppRole;
import org.apache.dolphinscheduler.api.platform.dto.sre.AppSreRelInfo;
import org.apache.dolphinscheduler.api.platform.service.AppIdService;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.github.pagehelper.PageInfo;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;

import lombok.extern.slf4j.Slf4j;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

@Service
@Slf4j
public class SolarisAppIdServiceImpl implements AppIdService {

    MediaType jsonMediaType = MediaType.parse("application/json; charset=UTF-8");
    String jsonContentType = "application/json; charset=UTF-8";

    private final OkHttpClient httpClient = new OkHttpClient().newBuilder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(60, TimeUnit.SECONDS)
            .readTimeout(60, TimeUnit.SECONDS)
            .build();

    private final Cache<String, AppIdDTO> appIdCache = CacheBuilder.newBuilder().maximumSize(10000)
            .expireAfterWrite(5L, TimeUnit.MINUTES).concurrencyLevel(16).build();

    // @PostConstruct
    // public void init() {
    // this.syncToCache();
    // List r =
    // this.queryWithNameFlat(Lists.newArrayList("int-service-arch-mario-transfer-worker"));
    // List a = this.queryByUserAccount("liuyangyang", true);
    // List b =
    // this.query(Lists.newArrayList("int-service-arch-mario-transfer-worker"));
    // List c = this.queryAll();
    // String e = this.queryAppOwnerNames("int-service-arch-mario-transfer-worker");
    // this.syncAppId("int-service-arch-mario-transfer-worker");
    // List f = this.getNotAdminAppId("liuyangyang", true);
    // Object g = this.queryByAppIdAndUser("int-service-arch-mario-transfer-worker",
    // "liuyangyang");
    // }

    @Scheduled(fixedRate = 1000 * 60 * 5, initialDelay = 1000)
    public void syncToCache() {
        long start = System.currentTimeMillis();
        Integer pageSize = 100;
        Integer pageNo = 1;

        PageInfo<String> pageInfo = null;
        do {
            pageInfo = this.pageQueryAllAppIds(pageNo, pageSize);

            pageInfo.getList().stream().forEach(appId -> {
                AppIdDTO dto = new AppIdDTO();
                dto.setAppId(appId);
                appIdCache.put(appId, dto);
            });

            try {
                // 防止 qps 过高
                Thread.sleep(100);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            pageNo++;

        } while (pageInfo != null && CollectionUtils.isNotEmpty(pageInfo.getList()));
        log.info("sync app ids to cache cost {} ms, total app cnt {}",
                System.currentTimeMillis() - start,
                appIdCache.size());
    }

    @Override
    public List<AppIdDTO> query(List<String> appIds) {
        Map<String, Object> data = new HashMap<>();
        data.put("channel", "5");
        data.put("postData", appIds);
        RequestBody requestBody = RequestBody.create(jsonMediaType, JSONUtils.toJson(data));
        Request request = new Request.Builder()
                .url(buildSolarisApiUrl() + "/sre/v2/app/public/owners")
                .post(requestBody)
                .addHeader("Content-Type", jsonContentType)
                .build();
        try {
            Response res = httpClient.newCall(request).execute();
            Map resultMap = JSONUtils.toObject(res.body().string(), Map.class);
            if (!resultMap.getOrDefault("code", "500").equals(10000)) {
                throw new RuntimeException("appid 服务异常");
            }

            List<AppSreRelInfo> appSreRelInfos = JSONUtils.toObject(JSONUtils.toJson(resultMap.get("data")),
                    new TypeReference<List<AppSreRelInfo>>() {
                    });

            if (appSreRelInfos != null) {
                return appSreRelInfos.stream().flatMap(
                        appSreRelInfo -> appSreRelInfo.getUsers().stream().map(sreUser -> {
                            AppIdDTO dto = new AppIdDTO();
                            dto.setAppId(appSreRelInfo.getAppId());
                            dto.setUserName(sreUser.getUserName());
                            dto.setRole(AppRole.role(sreUser.getRole()));
                            return dto;
                        })).collect(Collectors.toList());
            }
            return new ArrayList<>();
        } catch (IOException e) {
            log.error("query error", e);
            throw new RuntimeException("appid 服务异常");
        }
    }

    @Override
    public AppIdDTO queryByAppIdAndUser(String appId, String userName) {
        List<AppIdDTO> appIdInfos = this.query(Lists.newArrayList(appId));
        Optional<AppIdDTO> an = appIdInfos.stream().filter(appIdDTO -> appIdDTO.getUserName().equals(userName))
                .findAny();
        return an.orElse(null);
    }

    @Override
    public List<String> queryByUserAccount(String userAccount, boolean isOwner) {
        Set<String> appIds = new HashSet<>();
        Map<String, Object> data = new HashMap<>();
        data.put("postData", userAccount);
        data.put("channel", "5");
        RequestBody requestBody = RequestBody.create(jsonMediaType, JSONUtils.toJson(data));
        Request request = new Request.Builder()
                .url(buildSolarisApiUrl() + "/sre/v2/app/public/user")
                .post(requestBody)
                .addHeader("Content-Type", jsonContentType)
                .build();
        try {
            Response res = httpClient.newCall(request).execute();

            Map resultMap = JSONUtils.toObject(res.body().string(), Map.class);
            if (!resultMap.getOrDefault("code", "500").equals(10000)) {
                throw new RuntimeException("appid服务异常");
            }

            SolarisUserApp solarisUserApp = JSONUtils.toObject(JSONUtils.toJson(resultMap.get("data")),
                    SolarisUserApp.class);
            if (solarisUserApp != null && solarisUserApp.getAppRoles() != null) {
                if (isOwner) {
                    appIds.addAll(solarisUserApp.getAppRoles().stream()
                            .filter(i -> AppRole.OWNER == AppRole.role(i.getRole() + ""))
                            .map(SolarisUserAppRole::getAppId).collect(Collectors.toList()));
                } else {
                    appIds.addAll(solarisUserApp.getAppRoles().stream().map(SolarisUserAppRole::getAppId)
                            .collect(Collectors.toList()));
                }

            }
        } catch (IOException e) {
            log.error("query error", e);
            throw new RuntimeException("appid服务异常");
        }
        return Lists.newArrayList(appIds.toArray(new String[] {}));
    }

    @Override
    public List<AppIdDTO> queryAll() {
        return new ArrayList<>(this.appIdCache.asMap().values());
    }

    @Override
    public PageInfo<String> pageQueryAllAppIds(Integer pageNo, Integer pageSize) {
        Map<String, Object> data = new HashMap<>();
        data.put("length", pageSize);
        data.put("offSet", pageNo);
        data.put("channel", "5");
        RequestBody requestBody = RequestBody.create(jsonMediaType, JSONUtils.toJson(data));
        Request request = new Request.Builder()
                .url(buildSolarisApiUrl() + "/sre/v2/app/public/list")
                .post(requestBody)
                .addHeader("Content-Type", jsonContentType)
                .build();
        try {
            Response res = httpClient.newCall(request).execute();

            Map resultMap = JSONUtils.toObject(res.body().string(), Map.class);
            if (!resultMap.getOrDefault("code", "500").equals(10000)) {
                throw new RuntimeException("appid服务异常");
            }

            PageInfo<SolarisUserAppRole> pageInfo = JSONUtils.toObject(JSONUtils.toJson(resultMap.get("data")),
                    new TypeReference<PageInfo<SolarisUserAppRole>>() {
                    });

            if (pageInfo != null) {
                List<String> appRoles = pageInfo.getList().stream().map(SolarisUserAppRole::getAppId)
                        .collect(Collectors.toList());
                PageInfo<String> pageInfo1 = new PageInfo<>();
                pageInfo1.setList(appRoles);
                pageInfo1.setTotal(pageInfo.getTotal());
                pageInfo1.setPageNum(pageInfo.getPageNum());
                pageInfo1.setPageSize(pageInfo.getPageSize());
                pageInfo1.setPages(pageInfo.getPages());
                return pageInfo1;
            }
            return new PageInfo<>();

        } catch (IOException e) {
            log.error("pageQueryAllAppIds", e);
            throw new RuntimeException("appid服务异常");
        }
    }

    @Override
    public List<AppIdDTO> queryWithNameFlat(List<String> appIds) {
        List<AppIdDTO> appIdDtos = this.query(appIds);
        List<AppIdDTO> results = new ArrayList<>();
        Set<String> apps = appIdDtos.stream().map(AppIdDTO::getAppId).collect(Collectors.toSet());
        Map<String, List<AppIdDTO>> developerMap = appIdDtos.stream().filter(i -> AppRole.DEVELOPER == i.getRole())
                .collect(Collectors.groupingBy(AppIdDTO::getAppId));
        Map<String, List<AppIdDTO>> ownerMap = appIdDtos.stream().filter(i -> AppRole.OWNER == i.getRole())
                .collect(Collectors.groupingBy(AppIdDTO::getAppId));
        for (String appId : apps) {
            List<AppIdDTO> leaders = ownerMap.get(appId);
            List<AppIdDTO> owners = developerMap.get(appId);
            Set<String> names = Sets.newLinkedHashSet();
            if (leaders != null) {
                names.addAll(leaders.stream().map(AppIdDTO::getUserName).collect(Collectors.toList()));
            }
            if (owners != null) {
                names.addAll(owners.stream().map(AppIdDTO::getUserName).collect(Collectors.toList()));
            }
            AppIdDTO e = new AppIdDTO();
            e.setUserName(StringUtils.join(names, " "));
            e.setAppId(appId);
            results.add(e);
        }
        return results;

    }

    @Override
    public String queryAppOwnerNames(String appId) {
        List<AppIdDTO> appIdDTOS = this.query(Lists.newArrayList(appId)).stream()
                .filter(i -> AppRole.OWNER == i.getRole()).collect(Collectors.toList());
        if (CollectionUtils.isNotEmpty(appIdDTOS)) {
            return StringUtils.join(appIdDTOS.stream().map(AppIdDTO::getUserName).collect(Collectors.toList()), ", ");
        }
        return null;
    }

    @Override
    public void syncAppId(String appId) {
        Map<String, Object> data = new HashMap<>();
        data.put("channel", "5");
        data.put("postData", appId);
        RequestBody requestBody = RequestBody.create(jsonMediaType,
                JSONUtils.toJson(data));
        Request request = new Request.Builder().url(buildSolarisApiUrl() + "/sre/v2/app/public/sync")
                .post(requestBody)
                .addHeader("Content-Type", jsonContentType)
                .build();
        try {
            Response res = httpClient.newCall(request).execute();
            Map bizBaseResponse = JSONUtils.toObject(res.body().string(), Map.class);

            if (bizBaseResponse == null) {
                throw new RuntimeException("appid服务异常");
            }

            boolean isSuccess = bizBaseResponse.getOrDefault("code", "500").equals(10000);

            if (isSuccess) {
                List<AppIdDTO> appIdDTOS = this.queryWithNameFlat(Lists.newArrayList(appId));
                if (CollectionUtils.isNotEmpty(appIdDTOS)) {
                    appIdCache.put(appId, appIdDTOS.get(0));
                }
                return;
            } else {
                throw new RuntimeException("同步失败");
            }
        } catch (Exception e) {
            log.error("syncAppId", e);
            throw new RuntimeException("appid服务异常");
        }
    }

    @Override
    public List<String> getNotAdminAppId(String user, boolean onlyOwner) {
        List<String> results = queryByUserAccount(user, onlyOwner);
        return results == null ? Lists.newArrayList("no-admin") : results;
    }

    private String buildSolarisApiUrl() {
        if (EnvironmentUtil.isProd()) {
            return PlatformApolloConfigUtil.getSolariApiUrl() + "/solaris";
        } else {
            return PlatformApolloConfigUtil.getSolariApiUrl();
        }
    }

}
