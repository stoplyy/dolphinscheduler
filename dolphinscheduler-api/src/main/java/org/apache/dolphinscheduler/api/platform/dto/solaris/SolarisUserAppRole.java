package org.apache.dolphinscheduler.api.platform.dto.solaris;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

/**
 * Created by bailei on 2020/10/16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SolarisUserAppRole {

    /**
     * role : 2
     * appId : int-maven-arch-hubble
     */
    private int role;
    private String appId;
}
