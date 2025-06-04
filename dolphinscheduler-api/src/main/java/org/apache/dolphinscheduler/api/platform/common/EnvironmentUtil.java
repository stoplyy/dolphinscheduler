package org.apache.dolphinscheduler.api.platform.common;

import com.tuhu.commons.environment.BasicEnvironment;
import com.tuhu.commons.environment.enums.EnvEnum;

public class EnvironmentUtil {
    public static String getAppId() {
        String appId = BasicEnvironment.getInstance().getAppId();

        return appId;
    }

    public static String getEnvironment() {
        String env = BasicEnvironment.getInstance().getEnvironment();
        if ("prod".equals(env) || "production".equals(env) || "prd".equals(env)) {
            return "prod";
        }
        if ("tuhutest".equals(env)) {
            return "test";
        }
        EnvEnum envEnum = getEnvEnum();
        if(envEnum != null) {
            return envEnum.getCode();
        }
        return "work";
    }

    public static EnvEnum getEnvEnum() {
        String env = BasicEnvironment.getInstance().getEnvironment();
        // env转换为EnvEnum
        for (EnvEnum envEnum : EnvEnum.values()) {
            if (envEnum.getCode().equalsIgnoreCase(env)) {
                return envEnum;
            }
        }
        return null;
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
