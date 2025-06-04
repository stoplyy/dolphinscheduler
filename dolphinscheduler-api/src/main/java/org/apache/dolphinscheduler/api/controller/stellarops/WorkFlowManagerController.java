package org.apache.dolphinscheduler.api.controller.stellarops;

import static org.apache.dolphinscheduler.api.enums.Status.START_PROCESS_INSTANCE_ERROR;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.dto.stellarops.CreateExecutorRequest;
import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.ExecutorService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "STELLAROPS_TAG")
@RestController
@RequestMapping("/stellarops/workflow")
public class WorkFlowManagerController {

    @Autowired
    private ExecutorService execService;

    @Autowired
    ProjectClusterService projectClusterService;

    @PostMapping(value = "create-instance")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(START_PROCESS_INSTANCE_ERROR)
    public Result createExecutor(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @RequestBody CreateExecutorRequest request) {
                
        tryAddPlatformClusterParam(request);

        Map<String, Object> result = execService.execProcessInstance(loginUser, request.getProjectCode(),
                request.getProcessDefinitionCode(),
                request.getScheduleTime(), request.getExecType(), request.getFailureStrategy(),
                request.getStartNodeList(), request.getTaskDependType(), request.getWarningType(),
                request.getWarningGroupId(),
                request.getRunMode(), request.getProcessInstancePriority(),
                request.getWorkerGroup(), request.getTenantCode(), request.getEnvironmentCode(), request.getTimeout(),
                request.getStartParamMap(), request.getExpectedParallelismNumber(), request.getDryRun(),
                request.getTestFlag(),
                request.getComplementDependentMode(), request.getVersion(), request.isAllLevelDependent(),
                request.getExecutionOrder(), request.isDelayStart());

        return Result.success(result);
    }

    private void tryAddPlatformClusterParam(CreateExecutorRequest request) {
        String appId = request.getTuhuAppId();
        Map<String, String> inputParams = request.getStartParamMap();

        if (StringUtils.isNotEmpty(appId)) {
            if (inputParams == null) {
                inputParams = new HashMap<>();
                request.setStartParamMap(inputParams);
            }
            String inputPlatformClusterId = inputParams.getOrDefault("platform.cluster", null);
            if (StringUtils.isEmpty(inputPlatformClusterId)) {
                Result<ProjectCluster> cluster = projectClusterService.queryClusterByAppId(request.getProjectCode(),
                        appId);
                if (cluster.isSuccess()) {
                    ProjectCluster projectCluster = cluster.getData();
                    if (projectCluster != null) {
                        String clusterId = projectCluster.getClusterId();
                        inputParams.put("platform.cluster", clusterId);
                    }
                }

            }

        }
    }
}
