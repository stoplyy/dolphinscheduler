package org.apache.dolphinscheduler.common.config;

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

    public static String getProperty(String key, String defaultValue) {
        return config.getProperty(key, defaultValue);
    }
}
