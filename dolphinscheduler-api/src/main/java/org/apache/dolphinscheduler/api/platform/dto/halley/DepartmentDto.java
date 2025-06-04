package org.apache.dolphinscheduler.api.platform.dto.halley;

import lombok.Data;

/**
 * halley 部门信息
 * 
 * @Date: 2021-03-24 16:57:09
 * @LastEditors: Liuyangyang
 * @LastEditTime: 2021-03-24 17:04:07
 **/
@Data
public class DepartmentDto {
    private Integer gid;
    private String humanName;
    private Integer id;
    private Integer leaderId;
    private String levelOne;
    private UserInfoDto leaderInfo;
    private String name;
    private DepartmentDto parent;
}
