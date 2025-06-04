package org.apache.dolphinscheduler.api.service.impl;

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.service.ProjectClusterParameterService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectClusterParameter;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ProjectClusterMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectClusterParameterMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.google.common.collect.Lists;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class ProjectClusterParameterServiceImpl extends BaseServiceImpl implements ProjectClusterParameterService {

    @Autowired
    private ProjectClusterParameterMapper parameterMapper;
    @Autowired
    private ProjectClusterMapper clusterMapper;
    @Autowired
    ProjectService projectService;

    @Override
    public Result<ProjectClusterParameter> createParameter(User loginUser, long projectCode, Integer clusterCode,
            String parameterName, String parameterValue) {
        Result<ProjectClusterParameter> result = new Result<>();

        // check if user have write perm for project
        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null || projectCode != cluster.getProjectCode()) {
            log.error("Project Cluster does not exist, code:{}.", clusterCode);
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, projectCode, String.valueOf(clusterCode));
            return result;
        }

        // check if project parameter name exists
        ProjectClusterParameter projectParameter = parameterMapper.selectOne(new QueryWrapper<ProjectClusterParameter>()
                .lambda()
                .eq(ProjectClusterParameter::getClusterCode, clusterCode)
                .eq(ProjectClusterParameter::getParamName, parameterName));

        if (projectParameter != null) {
            log.warn("ProjectClusterParameter {} already exists.",
                    projectParameter.getParamName());
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_ALREADY_EXISTS,
                    projectParameter.getParamName());
            return result;
        }

        Date now = new Date();

        try {
            projectParameter = ProjectClusterParameter
                    .builder()
                    .clusterId(cluster.getClusterId())
                    .clusterName(cluster.getClusterName())
                    .projectCode(projectCode)
                    .paramName(parameterName)
                    .paramValue(parameterValue)
                    .clusterCode(clusterCode)
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
    public Result<ProjectClusterParameter> updateParameter(long projectCode,
            Integer clusterCode,
            Integer code,
            String parameterName, String parameterValue, String des) {
        Result<ProjectClusterParameter> result = new Result<>();

        ProjectClusterParameter parameter = parameterMapper.selectById(code);
        // check project parameter exists
        if (parameter == null || projectCode != parameter.getProjectCode()) {
            log.error("Project Cluster parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST, projectCode, clusterCode, String.valueOf(code));
            return result;
        }
        if (parameter.getClusterCode() != clusterCode) {
            log.error("Project Cluster parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST, projectCode, clusterCode, String.valueOf(code));
            return result;
        }

        // check if project parameter name exists
        ProjectClusterParameter tempProjectParameter = parameterMapper
                .selectOne(new QueryWrapper<ProjectClusterParameter>()
                        .lambda()
                        .eq(ProjectClusterParameter::getClusterCode, clusterCode)
                        .eq(ProjectClusterParameter::getParamName, parameterName));

        if (tempProjectParameter != null && !tempProjectParameter.getId().equals(code)) {
            log.error("Project cluster parameter name {} already exists", parameterName);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_ALREADY_EXISTS,
                    tempProjectParameter.getProjectCode(), tempProjectParameter.getClusterCode(),
                    parameterName);
            return result;
        }

        parameter.setParamName(parameterName);
        parameter.setParamValue(parameterValue);
        parameter.setDescription(des);

        if (parameterMapper.updateById(parameter) > 0) {
            log.info("Project Cluster parameter is updated and id is :{}",
                    parameter.getId());
            result.setData(parameter);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Cluster parameter update error, {}.", parameterName);
            putMsg(result, Status.UPDATE_PROJECT_CLUSTER_PARAMETER_ERROR);
        }
        return result;
    }

    @Override
    public Result<Boolean> deleteParametersByCode(long projectCode, Integer clusterCode, Integer code) {
        Result<Boolean> result = new Result<>();

        ProjectClusterParameter parameter = parameterMapper.selectById(code);
        // check project parameter exists
        if (parameter == null || projectCode != parameter.getProjectCode()) {
            log.error("Project Cluster parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST, projectCode, clusterCode, String.valueOf(code));
            return result;
        }

        if (parameter.getClusterCode() != clusterCode) {
            log.error("Project Cluster parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST, projectCode, clusterCode, String.valueOf(code));
            return result;
        }

        if (parameterMapper.deleteById(parameter.getId()) > 0) {
            log.info("Project Cluster parameter is deleted and id is :{}.",
                    parameter.getId());
            result.setData(Boolean.TRUE);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Cluster parameter delete error, {}.",
                    parameter.getParamName());
            putMsg(result, Status.DELETE_PROJECT_CLUSTER_PARAMETER_ERROR);
        }
        return result;
    }

    @Override
    public Result<Boolean> batchDeleteParametersByCodes(long projectCode, Integer clusterCode, String codes) {
        Result<Boolean> result = new Result<>();

        if (StringUtils.isEmpty(codes)) {
            log.error("Project Cluster parameter codes is empty, projectCode is {}.",
                    projectCode);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_CODE_EMPTY);
            return result;
        }

        Set<Integer> requestCodeSet = Lists.newArrayList(codes.split(Constants.COMMA)).stream().map(Integer::parseInt)
                .collect(Collectors.toSet());
        List<ProjectClusterParameter> parameterList = parameterMapper.selectBatchIds(requestCodeSet);
        Set<Integer> actualCodeSet = parameterList.stream().map(ProjectClusterParameter::getId)
                .collect(Collectors.toSet());
        // requestCodeSet - actualCodeSet
        Set<Integer> diffCode = requestCodeSet.stream().filter(code -> !actualCodeSet.contains(code))
                .collect(Collectors.toSet());

        String diffCodeString = diffCode.stream().map(String::valueOf).collect(Collectors.joining(Constants.COMMA));
        if (CollectionUtils.isNotEmpty(diffCode)) {
            log.error("Project Cluster parameter does not exist, codes:{}.", diffCodeString);
            throw new ServiceException(Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST,
                    projectCode,
                    clusterCode,
                    diffCodeString);
        }

        for (ProjectClusterParameter projectParameter : parameterList) {
            try {
                this.deleteParametersByCode(projectCode, clusterCode, projectParameter.getId());
            } catch (Exception e) {
                throw new ServiceException(Status.DELETE_PROJECT_CLUSTER_PARAMETER_ERROR,
                        e.getMessage());
            }
        }

        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<List<ProjectClusterParameter>> queryParameterList(long projectCode,
            Integer projectClusterId) {

        Result<List<ProjectClusterParameter>> result = new Result<>();

        List<ProjectClusterParameter> paramList = parameterMapper
                .selectList(new QueryWrapper<ProjectClusterParameter>()
                        .lambda().eq(ProjectClusterParameter::getProjectCode, projectCode)
                        .eq(ProjectClusterParameter::getClusterCode, projectClusterId));

        result.setData(paramList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<ProjectClusterParameter> queryParameterByCode(long projectCode, Integer code) {
        Result<ProjectClusterParameter> result = new Result<>();

        ProjectClusterParameter projectParameter = parameterMapper.selectById(code);
        if (projectParameter == null || projectCode != projectParameter.getProjectCode()) {
            log.error("Project Cluster parameter does not exist, code:{}.", code);
            putMsg(result, Status.PROJECT_CLUSTER_PARAMETER_NOT_EXIST, projectCode, "", String.valueOf(code));
            return result;
        }

        result.setData(projectParameter);
        putMsg(result, Status.SUCCESS);
        return result;
    }
}
