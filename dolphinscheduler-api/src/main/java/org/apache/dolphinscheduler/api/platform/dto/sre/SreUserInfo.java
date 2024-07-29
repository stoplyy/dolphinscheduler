package org.apache.dolphinscheduler.api.platform.dto.sre;

import lombok.Data;

/**
 * Created by bailei on 2020/10/14
 */
@Data
public class SreUserInfo {

    Integer id;
    String userName;
    String realName;

    String mobile;
    String email;
    String wechatl;
    String role;//1-developer; 2-owner; 3-leader
}
