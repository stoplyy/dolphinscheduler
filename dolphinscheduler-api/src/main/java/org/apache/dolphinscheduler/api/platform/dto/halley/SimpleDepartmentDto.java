package org.apache.dolphinscheduler.api.platform.dto.halley;

import java.util.List;

import lombok.Data;

@Data
public class SimpleDepartmentDto {
    private Integer id;
    private String label;
    private String name;
    private String humanName;
    private List<SimpleDepartmentDto> children;
}
