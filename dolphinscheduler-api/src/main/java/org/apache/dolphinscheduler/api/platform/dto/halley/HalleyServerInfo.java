package org.apache.dolphinscheduler.api.platform.dto.halley;

import lombok.Data;

@Data
public class HalleyServerInfo {
    String id;
    String clusterId;
    String ip;
    String hostName;
    Integer cpu;
    Integer memory;
    String zone;
}
