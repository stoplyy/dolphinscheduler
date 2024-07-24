package org.apache.dolphinscheduler.api.platform.dto.solaris;

import lombok.Data;

/**
 * Created by bailei on 2020/6/8
 */
@Data
public class AppIdDTO {

    private String appId;

    String userName;

    AppRole role;

    public AppIdDTO() {
    }

    public AppIdDTO(String appId) {
        this.appId = appId;
    }
}
