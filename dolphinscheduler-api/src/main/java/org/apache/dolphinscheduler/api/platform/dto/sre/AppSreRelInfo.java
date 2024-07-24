package org.apache.dolphinscheduler.api.platform.dto.sre;

import lombok.Data;

import java.util.List;

@Data
public class AppSreRelInfo {

    String appId;
    List<SreUserInfo> users;
    SreGroupInfo group;
}
