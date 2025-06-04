package org.apache.dolphinscheduler.api.platform.dto.halley;

import java.sql.Date;

import lombok.Data;

/**
 * halley 用户信息
 * 
 * @Date: 2021-03-24 16:57:38
 * @LastEditors: Liuyangyang
 * @LastEditTime: 2021-03-25 16:23:51
 **/
@Data
public class UserInfoDto {
    private Integer id;
    private Integer departmentId;
    // private String avatarUrl;
    private SimpleDepartmentDto departmentInfo;
    // private String email;
    // private Boolean isActive;
    // private Boolean isStaff;
    // private Boolean isSuperuser;
    // private Date lastLogin;
    // private String phone;
    private String realName;
    private String username;
    // private String wechatId;
}
