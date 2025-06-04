package org.apache.dolphinscheduler.api.platform;

public enum PathEnum {
    NONE("none"),
    CLUSTER_LIST("cluster.list.rest"),
    CLUSTER_PARAMS("cluster.params.rest"),
    NODE_LIST("node.list.rest"),
    NODE_PARAMS("node.params.rest"),
    TASK_PARAMS("task.params.rest"),
    ENV_CHECK("env.check.rest");

    private final String path;

    PathEnum(String path) {
        this.path = path;
    }

    public String getPath() {
        return path;
    }

    public static PathEnum fromPath(String path) {
        for (PathEnum value : values()) {
            if (value.path.equalsIgnoreCase(path)) {
                return value;
            }
        }
        return PathEnum.NONE;
    }
}