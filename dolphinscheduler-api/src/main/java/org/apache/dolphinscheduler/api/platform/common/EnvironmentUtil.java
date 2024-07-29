package org.apache.dolphinscheduler.api.platform.common;

import com.tuhu.commons.environment.BasicEnvironment;

public class EnvironmentUtil {
    public static String getAppId() {
        String appId = BasicEnvironment.getInstance().getAppId();

        return appId;
    }

    public static String getEnv() {
        String environment = BasicEnvironment.getInstance().getEnvironment();
        return environment;
    }

    public static String getEnvironment() {
        String env = BasicEnvironment.getInstance().getEnvironment();
        if ("prod".equals(env) || "production".equals(env) || "prd".equals(env)) {
            return "prod";
        }
        if ("work".equals(env)) {
            return "work";
        }
        if ("tuhutest".equals(env)) {
            return "test";
        }
        return "work";
    }

    public static boolean isProd() {
        String env = BasicEnvironment.getInstance().getEnvironment();
        if ("prod".equals(env) || "production".equals(env)
                || "prd".equals(env)) {
            return true;
        }
        return false;
    }
}
