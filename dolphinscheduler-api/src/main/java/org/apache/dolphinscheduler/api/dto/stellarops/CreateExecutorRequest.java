package org.apache.dolphinscheduler.api.dto.stellarops;

import java.util.HashMap;
import java.util.Map;

import lombok.Data;

@Data
public class CreateExecutorRequest extends CreateExecutorDefault {

    /***** stellarops start */
    /**
     * 是否延迟启动 默认false
     * 设定延迟后，任务为暂停状态
     */
    private boolean delayStart = false;


    /***** stellarops end */

    /**
     * 项目编码
     */
    private long projectCode;

    /**
     * 流程定义编码
     */
    private long processDefinitionCode;

    /**
     * 调度时间 {"complementStartDate":"2025-04-22
     * 00:00:00","complementEndDate":"2025-04-22 00:00:00"}
     */
    private String scheduleTime;

    /**
     * 启动参数
     * eq:{"a":"4","platform.datasource":"4","platform.cluster":"2,3","platform.node":"14,15,31"}
     */
    private Map<String, String> startParamMap = new HashMap<>();

    /**
     * 租户 root
     */
    private String tenantCode = "root";
    /* 环境编码 */
    private Long environmentCode = -1L;

    /*
     * processDefinitionCode: 15323568123328
     * failureStrategy: CONTINUE
     * warningType: NONE
     * warningGroupId:
     * execType: START_PROCESS
     * startNodeList:
     * taskDependType: TASK_POST
     * complementDependentMode: OFF_MODE
     * runMode: RUN_MODE_SERIAL
     * processInstancePriority: MEDIUM
     * workerGroup: default
     * tenantCode: root
     * environmentCode:
     * startParams:
     * {"a":"4","platform.datasource":"4","platform.cluster":"2","platform.node":
     * "14"}
     * expectedParallelismNumber:
     * dryRun: 0
     * testFlag: 0
     * version: 1
     * allLevelDependent: false
     * executionOrder: DESC_ORDER
     * isPlatform: true
     * isPlatformCluster: true
     * isPlatformNode: true
     * platformSource: 4
     * platformClusters: 2
     * platformNodes: 14
     * scheduleTime: {"complementStartDate":"2025-04-22 00:00:00"
     * ,"complementEndDate":"2025-04-22 00:00:00"}
     */
}
