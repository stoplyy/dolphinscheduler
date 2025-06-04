package org.apache.dolphinscheduler.api.platform.dto.solaris;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.util.List;

/**
 * Created by bailei on 2020/10/16
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@Data
public class SolarisUserApp {

  String userName;
  @JsonProperty("list")
  List<SolarisUserAppRole> appRoles;
}
