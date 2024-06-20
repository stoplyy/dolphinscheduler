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
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
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
@Tag(name = "TUHU-PLATFORM-PROJECT", description = "Tuhu Platform Node")
@RestController
@RequestMapping("projects/{projectCode}/project-node/{clusterCode}")
public class ProjectNodeController extends BaseController {
    @Autowired
    private ProjectNodeService service;

    @Autowired
    private ProjectNodeParameterService paramService;

    @Operation(summary = "createNode", description = "CREATE_PROJECT_CLUSTER")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "clusterName", description = "CLUSTER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "clusterId", description = "CLUSTER_ID", schema = @Schema(implementation = String.class)),
            @Parameter(name = "description", description = "CLUSTER_DESC", schema = @Schema(implementation = String.class))
    })
    @PostMapping("")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_PROJECT_ERROR)
    public Result<ProjectNode> create(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @RequestParam("nodeName") String nodeName,
            @RequestParam("nodeId") String nodeId,
            @RequestParam(value = "description", required = false) String description) {
        return service.createNode(loginUser, projectCode, clusterCode, nodeName, nodeId, description);
    }

    @Operation(summary = "updateNode", description = "UPDATE_PROJECT_CLUSTER")
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
    public Result<ProjectNode> update(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("code") int nodeCode,
            @RequestParam("nodeName") String nodeName,
            @RequestParam("nodeId") String nodeId,
            @RequestParam(value = "description", required = false) String description) {
        return service.update(loginUser, projectCode, nodeCode, nodeName, nodeId, description);
    }

    @Operation(summary = "queryNodeList", description = "QUERY_PROJECT_CLUSTER_LIST")
    @Parameters({
            @Parameter(name = "projectCode", description = "PROJECT_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_DETAILS_BY_CODE_ERROR)
    public Result<List<ProjectNode>> queryList(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @PathVariable long projectCode,
            @PathVariable int clusterCode) {
        return service.queryList(loginUser, projectCode, clusterCode);
    }

    @Operation(summary = "deleteNode", description = "DELETE_PROJECT_BY_ID_NOTES")
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
            @PathVariable int clusterCode,
            @PathVariable("code") Integer nodeCode) {
        return service.delete(loginUser, projectCode, clusterCode, nodeCode);
    }

    @Operation(summary = "createNodeParameter", description = "CREATE_PROJECT_CLUSTER_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "paramName", description = "PROJECT_PARAMETER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "paramValue", description = "PROJECT_PARAMETER_VALUE", schema = @Schema(implementation = String.class))
    })
    @PostMapping("/{nodeCode}/parameters")
    @ResponseStatus(HttpStatus.CREATED)
    @ApiException(CREATE_PROJECT_CLUSTER_PARAMETER_ERROR)
    public Result<ProjectNodeParameter> createParameter(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("nodeCode") Integer nodeCode,
            @RequestParam("paramName") String paramName,
            @RequestParam(value = "paramValue") String paramValue) {
        return paramService.createParameter(loginUser, projectCode, nodeCode, paramName, paramValue);
    }

    @Operation(summary = "updateNodeParameter", description = "UPDATE_PROJECT_CLUSTER_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456")),
            @Parameter(name = "paramName", description = "PROJECT_PARAMETER_NAME", schema = @Schema(implementation = String.class)),
            @Parameter(name = "paramValue", description = "PROJECT_PARAMETER_VALUE", schema = @Schema(implementation = String.class)),
            @Parameter(name = "description", description = "PROJECT_PARAMETER_DESC", schema = @Schema(implementation = String.class))
    })
    @PutMapping(value = "/{nodeCode}/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(UPDATE_PROJECT_PARAMETER_ERROR)
    public Result<ProjectNodeParameter> updateParameter(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("nodeCode") Integer nodeCode,
            @PathVariable("code") Integer code,
            @RequestParam("paramName") String paramName,
            @RequestParam(value = "paramValue") String paramValue,
            @RequestParam(value = "description", required = false) String des) {
        return paramService.updateParameter(loginUser, projectCode, nodeCode, code, paramName, paramValue, des);
    }

    @Operation(summary = "deleteNodeParametersByCode", description = "DELETE_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_PARAMETER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @DeleteMapping(value = "/{nodeCode}/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_PROJECT_PARAMETER_ERROR)
    public Result<Boolean> deleteParametersByCode(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable("nodeCode") Integer nodeCode,
            @PathVariable("code") Integer code) {
        return paramService.deleteParametersByCode(loginUser, projectCode, nodeCode, code);
    }

    @Operation(summary = "batchDeleteNodeParametersByCodes", description = "DELETE_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "codes", description = "PROJECT_PARAMETER_CODES", schema = @Schema(implementation = String.class))
    })
    @DeleteMapping(value = "/{nodeCode}/batch/parameters")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(DELETE_PROJECT_PARAMETER_ERROR)
    public Result<Boolean> batchDeleteParametersByCodes(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("nodeCode") Integer nodeCdoe,
            @RequestParam("codes") String codes) {
        return paramService.batchDeleteParametersByCodes(loginUser, projectCode, nodeCdoe, codes);
    }

    @Operation(summary = "queryNodeParameterList", description = "QUERY_PROJECT_PARAMETER_LIST_PAGING")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_CLUSTER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping("/{nodeCode}/parameters")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_PARAMETER_ERROR)
    public Result<List<ProjectNodeParameter>> queryParameterList(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("nodeCode") Integer nodeCode) {
        return paramService.queryParameterList(loginUser, projectCode, nodeCode);
    }

    @Operation(summary = "queryNodeParameterByCode", description = "QUERY_PROJECT_PARAMETER_NOTES")
    @Parameters({
            @Parameter(name = "code", description = "PROJECT_PARAMETER_CODE", schema = @Schema(implementation = long.class, example = "123456"))
    })
    @GetMapping(value = "/parameters/{code}")
    @ResponseStatus(HttpStatus.OK)
    @ApiException(QUERY_PROJECT_PARAMETER_ERROR)
    public Result<ProjectNodeParameter> queryParameterByCode(
            @Parameter(hidden = true) @RequestAttribute(value = Constants.SESSION_USER) User loginUser,
            @Parameter(name = "projectCode", description = "PROJECT_CODE", required = true) @PathVariable long projectCode,
            @PathVariable int clusterCode,
            @PathVariable("code") Integer parameterCode) {
        return paramService.queryParameterByCode(loginUser, projectCode, parameterCode);
    }
}
