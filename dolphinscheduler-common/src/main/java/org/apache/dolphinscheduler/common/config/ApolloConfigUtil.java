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

    public static String getProperty(ApolloConfigKey key) {
        return config.getProperty(key.getKey(), "");
    }

    public enum ApolloConfigKey {
        ALERT_SOLARIS_DETAIL_URL("alert.solaris.detail.url"),
        // true/false/none(default)
        SOLARIS_ALERT_PROJECT_ENABLED("solar.alert.%s.enabled"),
        // true/false/none(default)
        SOLARIS_ALERT_PROCESS_ENABLED("solar.alert.%s.%s.enabled"),
        ;

        private String key;

        ApolloConfigKey(String key) {
            this.key = key;
        }

        public String getKey() {
            return key;
        }
    }
}
