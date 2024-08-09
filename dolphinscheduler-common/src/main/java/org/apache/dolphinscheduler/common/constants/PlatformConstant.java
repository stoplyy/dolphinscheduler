package org.apache.dolphinscheduler.common.constants;

public class PlatformConstant {

    public static final String PLATFORM_PARAM_PRIFEX = "platform_";

    public static final String PLATFORM_PARAM_NAME = "platform_name";
    public static final String PLATFORM_PARAM_APP_ID = "platform_appid";
    public static final String PLATFORM_PARAM_BASE_URL = "platform_baseUrl";

    /**
     * remoteShell 插件的 datasource 参数名称
     */
    public static final String REMOTESHELL_DATASOURCE_PARAM_NAME = "platform.datasource.remoteShell";

    /**
     * task 任务参数id值 分隔符
     */
    public static final String PARAM_VALUE_SEPARATOR = ",";
    /**
     * task 任务的 datasource 参数名称
     * value为datasource 的
     */
    public static final String DATASOURCE_PARAM_NAME = "platform.datasource";
    /**
     * task 任务的 node 参数名称
     */
    public static final String NODE_PARAM_NAME = "platform.node";
    /**
     * task 任务的 cluster 参数名称
     */
    public static final String CLUSTER_PARAM_NAME = "platform.cluster";
    /**
     * task 任务的 node 参数值名称
     */
    public static final String NODE_PARAM_PROPS_NAME = "platform.node.props";
    /**
     * task 任务的 cluster 参数值名称
     */
    public static final String CLUSTER_PARAM_PROPS_NAME = "platform.cluster.props";

    // dynamic节点 LIST 参数分隔符
    public static final String DYNAMIC_LIST_SEPAROTOR = "LIST";
}