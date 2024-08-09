/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.dolphinscheduler.api.service.impl;

import static org.apache.dolphinscheduler.api.service.impl.ProjectServiceImpl.checkDesc;

import java.util.Date;
import java.util.List;

import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.platform.PlatformRestService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils.CodeGenerateException;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ProjectClusterMapper;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;

import lombok.extern.slf4j.Slf4j;

/**
 * project service impl
 **/
@Service
@Slf4j
public class ProjectClusterServiceImpl extends BaseServiceImpl implements ProjectClusterService {

    @Autowired
    private ProjectClusterMapper projectClusterMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    ProjectService projectService;

    @Autowired
    PlatformRestService platformRestService;

    @Override
    public Result<ProjectCluster> createCluster(User loginUser, String from, long projectCode, String name,
            String clusterId,
            String desc) {
        Result<ProjectCluster> result = new Result<>();

        checkDesc(result, desc);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            return result;
        }

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectCluster project = projectClusterMapper
                .selectOne(new QueryWrapper<ProjectCluster>().eq("cluster_id", clusterId).eq("project_code",
                        projectCode));

        if (project != null) {
            log.warn("Project {} cluster_id {} already exists.", projectCode, clusterId);
            putMsg(result, Status.PROJECT_CLUSTER_ALREADY_EXISTS, projectCode, clusterId);
            return result;
        }

        Date now = new Date();

        try {
            project = ProjectCluster
                    .builder()
                    .clusterId(clusterId)
                    .clusterName(name)
                    .projectCode(projectCode)
                    .description(desc)
                    .dataFrom(from)
                    .userId(loginUser.getId())
                    .userName(loginUser.getUserName())
                    .createTime(now)
                    .updateTime(now)
                    .build();
        } catch (CodeGenerateException e) {
            log.error("Generate process definition code error.", e);
            putMsg(result, Status.CREATE_PROJECT_CLUSTER_ERROR);
            return result;
        }

        if (projectClusterMapper.insert(project) > 0) {
            log.info("Project is created and id is :{}", project.getId());
            result.setData(project);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Cluster create error, projectClusterName:{}.", project.getClusterName());
            putMsg(result, Status.CREATE_PROJECT_CLUSTER_ERROR);
        }
        return result;
    }

    @Override
    public Result<List<ProjectCluster>> queryClusterListPaging(long projectCode) {
        Result<List<ProjectCluster>> result = new Result<>();
        List<ProjectCluster> projectList = projectClusterMapper
                .selectList(new QueryWrapper<ProjectCluster>().eq("project_code", projectCode));
        result.setData(projectList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<Boolean> deleteCluster(long projectCode, Integer clusterCode) {
        Result<Boolean> result = new Result<>();

        if (projectClusterMapper.deleteById(clusterCode) > 0) {
            putMsg(result, Status.SUCCESS);
        } else {
            putMsg(result, Status.DELETE_PROJECT_CLUSTER_ERROR, projectCode, clusterCode);
        }
        return result;
    }

    @Override
    public Result<ProjectCluster> update(User loginUser, long projectCode,
            Integer clusterCode, String clusterName,
            String clusterId, String desc,
            String appId) {
        Result<ProjectCluster> result = new Result<>();

        checkDesc(result, desc);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            return result;
        }

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        ProjectCluster projectCluster = projectClusterMapper
                .selectOne(new QueryWrapper<ProjectCluster>().eq("cluster_id", clusterId).eq("project_code",
                        projectCode));
        if (projectCluster.getId() != clusterCode) {
            log.warn("Project {} cluster_id {} already exists.", projectCode, clusterId);
            putMsg(result, Status.PROJECT_CLUSTER_ALREADY_EXISTS, projectCode, clusterId);
            return result;
        }

        projectCluster.setAppId(appId);
        projectCluster.setClusterName(clusterName);
        projectCluster.setClusterId(clusterId);
        projectCluster.setDescription(desc);
        projectCluster.setUpdateTime(new Date());
        projectCluster.setUserId(user.getId());

        int update = projectClusterMapper.updateById(projectCluster);

        if (update > 0) {
            log.info("Project Cluster is updated and id is :{}", projectCluster.getId());
            result.setData(projectCluster);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Cluster update error, project cluster Id:{}, clusterName:{}.", clusterCode,
                    projectCluster.getClusterName());
            putMsg(result, Status.UPDATE_PROJECT_ERROR);
        }
        return result;
    }
}
