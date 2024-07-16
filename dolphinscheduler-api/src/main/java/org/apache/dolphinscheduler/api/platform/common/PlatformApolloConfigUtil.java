package org.apache.dolphinscheduler.api.platform.common;

import org.apache.dolphinscheduler.common.config.ApolloConfigUtil;

/**
 * Created by bailei on 2020/5/20
 */
public class PlatformApolloConfigUtil {

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
}
