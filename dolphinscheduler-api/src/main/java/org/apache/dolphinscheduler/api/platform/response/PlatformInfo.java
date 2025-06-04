package org.apache.dolphinscheduler.api.platform.response;

import java.util.Map;

import lombok.Data;

@Data
public class PlatformInfo {
    String platform;

    Map platformParam;
}
