package org.apache.dolphinscheduler.api.controller;

import static org.apache.dolphinscheduler.api.enums.Status.CREATE_PROJECT_CLUSTER_PARAMETER_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.CREATE_PROJECT_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.DELETE_PROJECT_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.DELETE_PROJECT_PARAMETER_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_PROJECT_DETAILS_BY_CODE_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.QUERY_PROJECT_PARAMETER_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.UPDATE_PROJECT_ERROR;
import static org.apache.dolphinscheduler.api.enums.Status.UPDATE_PROJECT_PARAMETER_ERROR;

import java.util.List;

import org.apache.dolphinscheduler.api.exceptions.ApiException;
import org.apache.dolphinscheduler.api.service.ProjectClusterParameterService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectClusterParameter;
import org.apache.dolphinscheduler.dao.entity.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Parameters;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * project cluster controller
 */
@Tag(name = "TUHU-PLATFORM-PROJECT", description = "Tuhu Platform Cluster")
@RestController
@RequestMapping("projects/{projectCode}/project-cluster")
public class ProjectClusterController extends BaseController {
    @Autowired
    private ProjectClusterService service;

    @Autowired
    private ProjectClusterParameterService paramService;

    @Operation(summary = "createCluster", description = "CREATE_PROJECT_CLUSTER")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "clusterName", description = "CLUSTER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "clusterId", description = "CLUSTER_ID", schema = @Schema(implementation = String.class)),
            @Parameter(name = "description", description = "CLUSTER_DESC", schema = @Schema(implementation = String.class))
    })
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_PROJECT_ERROR)
    public Result<ProjectCluster> create(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @RequestParam("clusterName") String clusterName,
            @RequestParam("clusterId") String clusterId,
            @RequestParam(value = "description", required = false) String description) {
        return service.createCluster(loginUser, projectCode, clusterName, clusterId, description);
    }

    @Operation(summary = "updateCluster", description = "UPDATE_PROJECT_CLUSTER")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "clusterName", description = "CLUSTER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "clusterId", description = "CLUSTER_ID", schema = @Schema(implementation = String.class)),
            @Parameter(name = "appId", description = "APP_ID", schema = @Schema(implementation = String.class)),
            @Parameter(name = "description", description = "CLUSTER_DESC", schema = @Schema(implementation = String.class))
    })
    @PutMapping(value = "/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(UPDATE_PROJECT_ERROR)
    public Result<ProjectCluster> update(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable("code") Integer clusterCode,
            @RequestParam("clusterName") String clusterName,
            @RequestParam("clusterId") String clusterId,
            @RequestParam("appId") String appId,
            @RequestParam(value = "description", required = false) String description) {
        return service.update(loginUser, projectCode, clusterCode, clusterName, clusterId, description, appId);
    }

    @Operation(summary = "queryClusterList", description = "QUERY_PROJECT_CLUSTER_LIST")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_DETAILS_BY_CODE_ERROR)
    public Result<List<ProjectCluster>> queryList(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode) {
        return service.queryProjectListPaging(loginUser, projectCode);
    }

    @Operation(summary = "deleteCluster", description = "DELETE_PROJECT_BY_ID_NOTES")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "code", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @DeleteMapping(value = "/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_PROJECT_ERROR)
    public Result<Boolean> delete(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable("code") Integer clusterCode) {
        return service.deleteCluster(loginUser, projectCode, clusterCode);
    }

    @Operation(summary = "createClusterParameter", description = "CREATE_PROJECT_CLUSTER_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "paramName", description = "PROJECT_PARAMETER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "paramValue", description = "PROJECT_PARAMETER_VALUE", schema = @Schema(implementation = String.class))
    })
    @PostMapping("/{clusterCode}/parameters")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_PROJECT_CLUSTER_PARAMETER_ERROR)
    public Result<ProjectClusterParameter> createParameter(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("clusterCode") Integer clusterCode,
            @RequestParam("paramName") String paramName,
            @RequestParam(value = "paramValue") String paramValue) {
        return paramService.createParameter(loginUser, projectCode, clusterCode, paramName, paramValue);
    }

    @Operation(summary = "updateClusterParameter", description = "UPDATE_PROJECT_CLUSTER_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "paramName", description = "PROJECT_PARAMETER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "paramValue", description = "PROJECT_PARAMETER_VALUE", schema = @Schema(implementation = String.class)),
            @Parameter(name = "description", description = "PROJECT_PARAMETER_DESC", schema = @Schema(implementation = String.class))
    })
    @PutMapping(value = "/{clusterCode}/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(UPDATE_PROJECT_PARAMETER_ERROR)
    public Result<ProjectClusterParameter> updateParameter(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("clusterCode") Integer clusterCode,
            @PathVariable("code") Integer code,
            @RequestParam("paramName") String paramName,
            @RequestParam(value = "paramValue") String paramValue,
            @RequestParam(value = "description", required = false) String des) {
        return paramService.updateParameter(loginUser, projectCode, clusterCode, code, paramName, paramValue,
                des);
    }

    @Operation(summary = "deleteClusterParametersByCode", description = "DELETE_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_PARAMETER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @DeleteMapping(value = "/{clusterCode}/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_PROJECT_PARAMETER_ERROR)
    public Result<Boolean> deleteParametersByCode(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("clusterCode") Integer clusterCode,
            @PathVariable("code") Integer code) {
        return paramService.deleteParametersByCode(loginUser, projectCode, clusterCode, code);
    }

    @Operation(summary = "batchDeleteClusterParametersByCodes", description = "DELETE_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "codes", description = "PROJECT_PARAMETER_CODES", schema = @Schema(implementation = String.class))
    })
    @DeleteMapping(value = "/{clusterCode}/batch/parameters")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_PROJECT_PARAMETER_ERROR)
    public Result<Boolean> batchDeleteParametersByCodes(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("clusterCode") Integer clusterCode,
            @RequestParam("codes") String codes) {
        return paramService.batchDeleteParametersByCodes(loginUser, projectCode, clusterCode, codes);
    }

    @Operation(summary = "queryClusterParameterList", description = "QUERY_PROJECT_PARAMETER_LIST_PAGING")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping("/{clusterCode}/parameters")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_PARAMETER_ERROR)
    public Result<List<ProjectClusterParameter>> queryParameterList(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("clusterCode") Integer clusterCode) {
        return paramService.queryParameterList(loginUser, projectCode, clusterCode);
    }

    @Operation(summary = "queryClusterParameterByCode", description = "QUERY_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_PARAMETER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping(value = "/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_PARAMETER_ERROR)
    public Result<ProjectClusterParameter> queryParameterByCode(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("code") Integer parameterCode) {
        return paramService.queryParameterByCode(loginUser, projectCode, parameterCode);
    }
}
