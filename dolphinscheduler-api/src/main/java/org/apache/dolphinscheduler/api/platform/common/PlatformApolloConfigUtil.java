package org.apache.dolphinscheduler.api.platform.common;

import org.apache.dolphinscheduler.common.config.ApolloConfigUtil;

/**
 * Created by bailei on 2020/5/20
 */
public class PlatformApolloConfigUtil {

    public static String getPublicKey() {
        return ApolloConfigUtil.getProperty("datasource.pubKey", "public key not found");
    }

    /**
     * 获取sre token
     * 
     * @return
     */
    public static String getStellarOpsPlatformAuthToken() {
        return ApolloConfigUtil.getProperty("stellarops.auth.head", "X-SRE-SEC: xxxxx");
    }

    /* platform 配置数据 */
    public static String getPlatformConfig(String platformString) {
        return ApolloConfigUtil.getProperty("maintenance.platform.config." + platformString, "{}");
    }

    /**
     * 获取halley地址
     * 
     * @return
     */
    public static String getHalleyApiUrl() {
        return getHalleyApiBaseUrl() + "/api/v1";
    }

    /**
     * 获取halley地址
     * 
     * @return
     */
    public static String getHalleyApiBaseUrl() {
        return ApolloConfigUtil.getProperty("halley.url", "https://halley-service.tuhuyun.cn");
    }

    /**
     * 获取halley地址
     * 
     * @return
     */
    public static String getHalleyAuthToken() {
        return ApolloConfigUtil.getProperty("halley.url.auth",
                "eyJ0eXAiOiJKV1QiLCJhbGciOiJIUzI1NiJ9.eyJ1c2VyX2lkIjoxMjMzLCJ1c2VyX25hbWUiOiJkZXBsb3kiLCJuYW1lIjoiXHU1M2QxXHU1ZTAzXHU3Y2ZiXHU3ZWRmIiwiaXNfc3VwZXJ1c2VyIjowLCJzdWIiOjE2MDQ0NzE5MDQsImV4cCI6MTgyNTIyMzkwNH0.MNUbHjOfkLTHkGaYoyrbrdTm5t0e1qyA9f9ydZgsl6o");
    }

    public static String getSolariApiUrl() {
        return ApolloConfigUtil.getProperty("solaris.notify.url", "https://solaris.tuhu.work/solaris/api");
    }
}
