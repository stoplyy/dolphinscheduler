package org.apache.dolphinscheduler.api.platform.enums;

public enum DataFrom {
    NONE("none"),
    AUTO("auto"),
    MANUAL("manual");

    private String value;

    DataFrom(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static DataFrom of(String value) {
        for (DataFrom dataFrom : DataFrom.values()) {
            if (dataFrom.getValue().equals(value)) {
                return dataFrom;
            }
        }
        return DataFrom.NONE;
    }
}
