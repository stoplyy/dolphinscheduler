package org.apache.dolphinscheduler.api.platform.enums;

public enum DataFrom {
    NONE("none"),
    AUTO("auto"), // stellarops-sdk 接口导入
    HALLEY("halley"), // halley 接口导入
    MANUAL("manual"); // 手动导入

    private String value;

    DataFrom(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataFrom of(String value) {
        for (DataFrom dataFrom : DataFrom.values()) {
            if (dataFrom.getValue().equalsIgnoreCase(value)) {
                return dataFrom;
            }
        }
        return DataFrom.NONE;
    }
}
