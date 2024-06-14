package org.apache.dolphinscheduler.api.service.impl;

import static org.apache.dolphinscheduler.api.constants.ApiFuncIdentificationConstant.PROJECT;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.AuthorizationType;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ProjectNodeMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectNodeParameterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProjectNodeParameterServiceImpl extends BaseServiceImpl implements ProjectNodeParameterService {

    @Autowired
    private ProjectNodeParameterMapper parameterMapper;

    @Autowired
    private ProjectNodeMapper nodeMapper;

    @Autowired
    private ProjectService projectService;

    @Override
    public Result<ProjectNodeParameter> createParameter(User loginUser, long projectCode, Integer nodeCode,
            String parameterName, String parameterValue) {
        Result<ProjectNodeParameter> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectNode node = nodeMapper.selectById(nodeCode);
        if (node == null || projectCode != node.getProjectCode()) {
            log.error("Project node does not exist, code:{}.", nodeCode);
            putMsg(result, Status.PROJECT_NODE_NOT_EXIST, projectCode, String.valueOf(nodeCode));
            return result;
        }

        // check if project parameter name exists
        ProjectNodeParameter projectParameter = parameterMapper.selectOne(new QueryWrapper<ProjectNodeParameter>()
                .lambda()
                .eq(ProjectNodeParameter::getNodeCode, nodeCode)
                .eq(ProjectNodeParameter::getParamName, parameterName));

        if (projectParameter != null) {
            log.warn("ProjectNodeParameter {} already exists.",
                    projectParameter.getParamName());
            putMsg(result, Status.PROJECT_NODE_PARAMETER_ALREADY_EXISTS,
                    projectParameter.getParamName());
            return result;
        }

        Date now = new Date();

        try {
            projectParameter = ProjectNodeParameter
                    .builder()
                    .nodeCode(nodeCode)
                    .projectCode(projectCode)
                    .paramName(parameterName)
                    .paramValue(parameterValue)
                    .clusterCode(nodeCode)
                    .nodeId(node.getNodeId())
                    .clusterCode(node.getClusterCode())
                    .userId(loginUser.getId())
                    .createTime(now)
                    .updateTime(now)
                    .build();
        } catch (CodeGenerateUtils.CodeGenerateException e) {
            log.error("Generate project parameter code error.", e);
            putMsg(result, Status.CREATE_PROJECT_NODE_PARAMETER_ERROR);
            return result;
        }

        if (parameterMapper.insert(projectParameter) > 0) {
            log.info("Project parameter is created and id is :{}",
                    projectParameter.getId());
            result.setData(projectParameter);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project parameter create error, projectName:{}.",
                    projectParameter.getParamName());
            putMsg(result, Status.CREATE_PROJECT_NODE_PARAMETER_ERROR);
        }
        return result;
    }

    @Override
    public Result<ProjectNodeParameter> updateParameter(User loginUser, long projectCode,
            Integer nodeCode,
            Integer code,
            String parameterName, String parameterValue, String des) {
        Result<ProjectNodeParameter> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectNode node = nodeMapper.selectById(nodeCode);
        if (node == null || projectCode != node.getProjectCode()) {
            log.error("Project node does not exist, code:{}.", nodeCode);
            putMsg(result, Status.PROJECT_NODE_NOT_EXIST, projectCode, String.valueOf(nodeCode));
            return result;
        }

        ProjectNodeParameter parameter = parameterMapper.selectById(code);
        // check project parameter exists
        if (parameter == null || projectCode != parameter.getProjectCode()) {
            log.error("Project Node parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_NOT_EXIST, projectCode, nodeCode, String.valueOf(code));
            return result;
        }

        // check if project parameter name exists
        ProjectNodeParameter tempProjectParameter = parameterMapper
                .selectOne(new QueryWrapper<ProjectNodeParameter>()
                        .lambda()
                        .eq(ProjectNodeParameter::getNodeCode, nodeCode)
                        .eq(ProjectNodeParameter::getParamName, parameterName));

        if (tempProjectParameter != null && !tempProjectParameter.getId().equals(code)) {
            log.error("Project Node parameter name {} already exists", parameterName);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_ALREADY_EXISTS,
                    tempProjectParameter.getProjectCode(), tempProjectParameter.getNodeCode(),
                    parameterName);
            return result;
        }

        parameter.setParamName(parameterName);
        parameter.setParamValue(parameterValue);
        parameter.setDescription(des);

        if (parameterMapper.updateById(parameter) > 0) {
            log.info("Project Node parameter is updated and id is :{}",
                    parameter.getId());
            result.setData(parameter);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Node parameter update error, {}.", parameterName);
            putMsg(result, Status.UPDATE_PROJECT_NODE_PARAMETER_ERROR);
        }
        return result;
    }

    @Override
    public Result<Boolean> deleteParametersByCode(User loginUser, long projectCode, Integer nodeCode, Integer code) {
        Result<Boolean> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectNodeParameter parameter = parameterMapper.selectById(code);
        // check project parameter exists
        if (parameter == null || parameter.getProjectCode() != projectCode) {
            log.error("Project Node parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_NOT_EXIST, projectCode, nodeCode, String.valueOf(code));
            return result;
        }

        if (parameter.getNodeCode() != nodeCode) {
            log.error("Project Node parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_NOT_EXIST, projectCode, nodeCode, String.valueOf(code));
            return result;
        }

        if (parameterMapper.deleteById(parameter.getId()) > 0) {
            log.info("Project Node parameter is deleted and id is :{}.",
                    parameter.getId());
            result.setData(Boolean.TRUE);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Node parameter delete error, {}.",
                    parameter.getParamName());
            putMsg(result, Status.DELETE_PROJECT_NODE_PARAMETER_ERROR);
        }
        return result;
    }

    @Override
    public Result<Boolean> batchDeleteParametersByCodes(User loginUser, long projectCode, Integer nodeCode,
            String codes) {
        Result<Boolean> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        if (StringUtils.isEmpty(codes)) {
            log.error("Project Node parameter codes is empty, projectCode is {}.",
                    projectCode);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_CODE_EMPTY);
            return result;
        }

        Set<Integer> requestCodeSet = Lists.newArrayList(codes.split(Constants.COMMA)).stream().map(Integer::parseInt)
                .collect(Collectors.toSet());
        List<ProjectNodeParameter> parameterList = parameterMapper.selectBatchIds(requestCodeSet);
        Set<Integer> actualCodeSet = parameterList.stream().map(ProjectNodeParameter::getId)
                .collect(Collectors.toSet());
        // requestCodeSet - actualCodeSet
        Set<Integer> diffCode = requestCodeSet.stream().filter(code -> !actualCodeSet.contains(code))
                .collect(Collectors.toSet());

        String diffCodeString = diffCode.stream().map(String::valueOf).collect(Collectors.joining(Constants.COMMA));
        if (CollectionUtils.isNotEmpty(diffCode)) {
            log.error("Project Node parameter does not exist, codes:{}.", diffCodeString);
            throw new ServiceException(Status.PROJECT_NODE_PARAMETER_NOT_EXIST,
                    projectCode, nodeCode, diffCodeString);
        }

        for (ProjectNodeParameter projectParameter : parameterList) {
            try {
                this.deleteParametersByCode(loginUser, projectCode, nodeCode, projectParameter.getId());
            } catch (Exception e) {
                throw new ServiceException(Status.DELETE_PROJECT_NODE_PARAMETER_ERROR,
                        e.getMessage());
            }
        }

        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<List<ProjectNodeParameter>> queryParameterList(User loginUser, long projectCode, Integer nodeCode) {

        Result<List<ProjectNodeParameter>> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        List<ProjectNodeParameter> paramList = parameterMapper
                .selectList(new QueryWrapper<ProjectNodeParameter>()
                        .lambda().eq(ProjectNodeParameter::getProjectCode, projectCode)
                        .eq(ProjectNodeParameter::getNodeCode, nodeCode));

        result.setData(paramList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<ProjectNodeParameter> queryParameterByCode(User loginUser, long projectCode, Integer code) {
        Result<ProjectNodeParameter> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectNodeParameter projectParameter = parameterMapper.selectById(code);

        if (projectParameter == null || projectCode != projectParameter.getProjectCode()) {
            log.error("Project NODE parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_NODE_PARAMETER_NOT_EXIST, projectCode, "", String.valueOf(code));
            return result;
        }

        result.setData(projectParameter);
        putMsg(result, Status.SUCCESS);
        return result;
    }
}
