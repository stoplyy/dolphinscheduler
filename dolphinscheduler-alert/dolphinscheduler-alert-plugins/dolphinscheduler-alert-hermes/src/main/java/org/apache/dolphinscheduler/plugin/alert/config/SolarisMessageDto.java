package org.apache.dolphinscheduler.plugin.alert.config;

import lombok.Data;

@Data
public class SolarisMessageDto {
    private String endpoint;
    private int endpointType;
    private String platform;
    private String level;
    private String eventId;
    private String status;
    private String content;
    private String title;
    private String type;
    private String metric;
    private String eventTime;
    private String detailUrl;
}
