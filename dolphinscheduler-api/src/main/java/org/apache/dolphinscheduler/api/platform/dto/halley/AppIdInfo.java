package org.apache.dolphinscheduler.api.platform.dto.halley;

import java.util.List;

import lombok.Data;

@Data
public class AppIdInfo {
    String id;

    // 部门信息
    DepartmentDto department;

    // 机器信息
    List<AssetsInfo> assets;

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public List<AssetsInfo> getAssets() {
        return assets;
    }

    public void setAssets(List<AssetsInfo> assets) {
        this.assets = assets;
    }
}
