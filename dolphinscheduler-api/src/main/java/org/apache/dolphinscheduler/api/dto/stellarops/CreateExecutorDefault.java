package org.apache.dolphinscheduler.api.dto.stellarops;

import lombok.Data;
import org.apache.dolphinscheduler.common.enums.CommandType;
import org.apache.dolphinscheduler.common.enums.ComplementDependentMode;
import org.apache.dolphinscheduler.common.enums.ExecutionOrder;
import org.apache.dolphinscheduler.common.enums.FailureStrategy;
import org.apache.dolphinscheduler.common.enums.Priority;
import org.apache.dolphinscheduler.common.enums.RunMode;
import org.apache.dolphinscheduler.common.enums.TaskDependType;
import org.apache.dolphinscheduler.common.enums.WarningType;

@Data
public class CreateExecutorDefault {

     /** 不明参数 */
     private String workerGroup = "default";
     private Integer version;
     private String startNodeList;
     private Integer warningGroupId;
     private Integer expectedParallelismNumber;

     /*** 默认参数 */
     private ComplementDependentMode complementDependentMode = ComplementDependentMode.OFF_MODE;
     private RunMode runMode = RunMode.RUN_MODE_SERIAL;
     private Priority processInstancePriority = Priority.MEDIUM;
     private FailureStrategy failureStrategy = FailureStrategy.CONTINUE;
     private TaskDependType taskDependType = TaskDependType.TASK_POST;
     private CommandType execType = CommandType.START_PROCESS;
     private WarningType warningType = WarningType.NONE;
     private boolean allLevelDependent = false;
     private ExecutionOrder executionOrder = ExecutionOrder.DESC_ORDER;
     private int dryRun = 0;
     private int testFlag = 0;
     private Integer timeout = org.apache.dolphinscheduler.common.constants.Constants.MAX_TASK_TIMEOUT;

     /*
      * e: 15323568123328
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
