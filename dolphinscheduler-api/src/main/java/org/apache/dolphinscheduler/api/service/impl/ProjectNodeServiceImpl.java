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
import org.apache.dolphinscheduler.api.platform.enums.DataFrom;
import org.apache.dolphinscheduler.api.platform.facade.PlatformOpenApi;
import org.apache.dolphinscheduler.api.service.DataSourceService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils.CodeGenerateException;
import org.apache.dolphinscheduler.dao.entity.Project;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ProjectClusterMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectNodeMapper;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;

import lombok.extern.slf4j.Slf4j;

/**
 * project service impl
 **/
@Service
@Slf4j
public class ProjectNodeServiceImpl extends BaseServiceImpl implements ProjectNodeService {
    @Autowired
    private ProjectNodeMapper projectNodeMapper;

    @Autowired
    private UserMapper userMapper;

    @Autowired
    ProjectService projectService;

    @Autowired
    DataSourceService dataSourceService;

    @Autowired
    private ProjectClusterMapper clusterMapper;

    @Autowired
    PlatformOpenApi platformOpenApi;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    PlatformRestService platformRestService;

    @Override
    public Result<Boolean> syncNodeFromPlatform(User loginUser, long projectCode, Integer clusterCode) {
        Result<Boolean> result = new Result<>();
        result.setData(false);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, clusterCode);
            return result;
        }

        Result<List<StellarOpsNodeInfo>> platFormNodes = platformOpenApi.getPlatformNodeList(
                project.getName(), cluster.getClusterId(), "");

        if (platFormNodes.getCode() != Status.SUCCESS.getCode()) {
            putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, platFormNodes.getMsg());
            return result;
        }

        List<StellarOpsNodeInfo> nodes = platFormNodes.getData();
        if (nodes == null || nodes.isEmpty()) {
            putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, "No nodes found in platform.");
            return result;
        }

        List<ProjectNode> projectList = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>().lambda()
                        .eq(ProjectNode::getClusterCode, clusterCode)
                        .eq(ProjectNode::getFrom, DataFrom.AUTO.getValue()));
        List<ProjectNode> needInserList = new java.util.ArrayList<>();
        List<ProjectNode> needDeleteList = new java.util.ArrayList<>();
        for (StellarOpsNodeInfo node : nodes) {
            ProjectNode projectNode = projectList.stream()
                    .filter(p -> p.getNodeId().equals(node.getNodeId()))
                    .findFirst()
                    .orElse(null);

            if (projectNode == null) {
                needInserList.add(
                        buildNewProjectNode(loginUser, projectCode, clusterCode, node.getNodeName(), node.getNodeKey(),
                                node.getNodeId(), DataFrom.AUTO.getValue(), "一键同步", cluster));
            }
        }

        for (ProjectNode projectNode : projectList) {
            StellarOpsNodeInfo node = nodes.stream()
                    .filter(p -> p.getNodeId().equals(projectNode.getNodeId()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                needDeleteList.add(projectNode);
            }
        }

        log.info("Sync project node from platform, projectCode:{}, clusterCode:{}, needInserList:{}, needDeleteList:{}",
                projectCode, clusterCode, needInserList.size(), needDeleteList.size());

        if (!needInserList.isEmpty()) {
            needInserList.forEach(p -> projectNodeMapper.insert(p));
        }

        if (!needDeleteList.isEmpty()) {
            needDeleteList.forEach(p -> projectNodeMapper.deleteById(p.getId()));
        }

        result.setData(true);
        return result;
    }

    @Override
    public Result<ProjectNode> createNode(User loginUser, long projectCode, Integer clusterCode, String nodeName,
            String nodeKey, String nodeId, String from, String desc) {
        Result<ProjectNode> result = new Result<>();

        checkDesc(result, desc);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            return result;
        }

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectNode project = projectNodeMapper
                .selectOne(new QueryWrapper<ProjectNode>()
                        .eq("cluster_code", clusterCode)
                        .eq("project_code", projectCode)
                        .eq("node_id", nodeId));

        if (project != null) {
            log.warn("Project {} cluster_code {} node_id {} already exists.", projectCode, clusterCode, nodeId);
            putMsg(result, Status.PROJECT_NODE_ALREADY_EXISTS, projectCode, clusterCode, nodeId);
            return result;
        }

        ProjectCluster clsuter = clusterMapper.selectById(clusterCode);

        if (clsuter == null) {
            log.warn("Cluster {} not exists.", clusterCode);
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, clusterCode);
            return result;
        }

        try {
            project = buildNewProjectNode(loginUser, projectCode, clusterCode, nodeName, nodeKey, nodeId, from, desc,
                    clsuter);

        } catch (CodeGenerateException e) {
            log.error("Generate process definition code error.", e);
            putMsg(result, Status.CREATE_PROJECT_NODE_ERROR);
            return result;
        }

        if (projectNodeMapper.insert(project) > 0) {
            log.info("Project node is created and id is :{}", project.getId());
            result.setData(project);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Create project node error");
            putMsg(result, Status.CREATE_PROJECT_NODE_ERROR);
        }
        return result;
    }

    private ProjectNode buildNewProjectNode(User loginUser, long projectCode, Integer clusterCode, String nodeName,
            String nodeKey, String nodeId, String from, String desc, ProjectCluster clsuter) {

        Date now = new Date();
        ProjectNode project;
        project = ProjectNode
                .builder()
                .projectCode(projectCode)
                .clusterId(clsuter.getClusterId())
                .clusterCode(clusterCode)
                .nodeKey(nodeKey)
                .from(DataFrom.of(from).getValue())
                .nodeId(nodeId)
                .nodeName(nodeName)
                .description(desc)
                .userId(loginUser.getId())
                .userName(loginUser.getUserName())
                .createTime(now)
                .updateTime(now)
                .build();
        return project;
    }

    @Override
    public Result<List<ProjectNode>> queryList(User loginUser, long projectCode, Integer clusterCode) {
        Result<List<ProjectNode>> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        List<ProjectNode> projectList = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>()
                        .eq("cluster_code", clusterCode)
                        .eq("project_code", projectCode));

        result.setData(projectList);
        putMsg(result, Status.SUCCESS);
        return result;
    }

    @Override
    public Result<Boolean> delete(User loginUser, long projectCode, Integer clusterCode, Integer code) {
        Result<Boolean> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, clusterCode);
            return result;
        }

        if (projectNodeMapper.deleteById(code) > 0) {
            putMsg(result, Status.SUCCESS);
        } else {
            putMsg(result, Status.DELETE_PROJECT_NODE_ERROR);
        }
        return result;
    }

    @Override
    public Result<ProjectNode> update(User loginUser, long projectCode, Integer projectNodeId, String nodeName,
            String nodeKey, String nodeId, String desc) {
        Result<ProjectNode> result = new Result<>();

        checkDesc(result, desc);
        if (result.getCode() != Status.SUCCESS.getCode()) {
            return result;
        }

        ProjectNode projectNode = projectNodeMapper.selectById(projectNodeId);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        User user = userMapper.selectById(loginUser.getId());
        if (user == null) {
            log.error("user {} not exists", loginUser.getId());
            putMsg(result, Status.USER_NOT_EXIST, loginUser.getId());
            return result;
        }

        projectNode.setNodeName(nodeName);
        projectNode.setNodeId(nodeId);
        projectNode.setNodeKey(nodeKey);
        projectNode.setDescription(desc);
        projectNode.setUpdateTime(new Date());
        projectNode.setUserId(user.getId());

        int update = projectNodeMapper.updateById(projectNode);

        if (update > 0) {
            log.info("Project Node is updated and id is :{}", projectNode.getId());
            result.setData(projectNode);
            putMsg(result, Status.SUCCESS);
        } else {
            log.error("Project Node update error, project cluster node Id:{}, nodeName:{}.", projectNodeId, nodeName);
            putMsg(result, Status.UPDATE_PROJECT_ERROR);
        }
        return result;
    }
}
