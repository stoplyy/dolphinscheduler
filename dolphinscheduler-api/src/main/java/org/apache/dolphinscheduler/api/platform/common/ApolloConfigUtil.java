package org.apache.dolphinscheduler.api.platform.common;

import java.util.Set;

import com.ctrip.framework.apollo.Config;
import com.ctrip.framework.apollo.ConfigChangeListener;
import com.ctrip.framework.apollo.ConfigService;

/**
 * Created by bailei on 2020/5/20
 */
public class ApolloConfigUtil {

    private static Config config = ConfigService.getAppConfig();

    public static void registerAppConfigListener(ConfigChangeListener listener, Set<String> interestedKeys) {
        config.addChangeListener(listener, interestedKeys);
    }

    public static String getProperty(String key) {
        return config.getProperty(key, "");
    }

    /**
     * 获取sre token
     * 
     * @return
     */
    public static String getStellarOpsPlatformAuthToken() {
        return config.getProperty("stellarops.auth.head", "X-SRE-SEC: xxxxx");
    }

    /* platform 配置数据 */
    public static String getPlatformConfig(String platformString) {
        return config.getProperty("maintenance.platform.config." + platformString, "{}");
    }
}
