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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import org.apache.dolphinscheduler.api.controller.PlatformOpenApiController;
import org.apache.dolphinscheduler.api.enums.Status;
import org.apache.dolphinscheduler.api.exceptions.ServiceException;
import org.apache.dolphinscheduler.api.platform.PlatformRestService;
import org.apache.dolphinscheduler.api.platform.dto.halley.AssetsInfo;
import org.apache.dolphinscheduler.api.platform.dto.halley.HalleyServerInfo;
import org.apache.dolphinscheduler.api.platform.enums.DataFrom;
import org.apache.dolphinscheduler.api.platform.service.HalleyAccessService;
import org.apache.dolphinscheduler.api.platform.service.SreAccessService;
import org.apache.dolphinscheduler.api.service.DataSourceService;
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.api.service.ProjectService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.utils.CodeGenerateUtils.CodeGenerateException;
import org.apache.dolphinscheduler.dao.entity.Project;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.User;
import org.apache.dolphinscheduler.dao.mapper.ProjectClusterMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectMapper;
import org.apache.dolphinscheduler.dao.mapper.ProjectNodeMapper;
import org.apache.dolphinscheduler.dao.mapper.UserMapper;
import org.apache.dolphinscheduler.plugin.datasource.api.datasource.BaseDataSourceParamDTO;
import org.apache.dolphinscheduler.plugin.datasource.api.utils.DataSourceUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.alibaba.fastjson.JSONObject;
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
    PlatformOpenApiController platformOpenApi;

    @Autowired
    private ProjectMapper projectMapper;

    @Autowired
    PlatformRestService platformRestService;

    @Autowired
    HalleyAccessService halleyAccessService;

    @Autowired
    ProjectNodeParameterService projectNodeParameterService;

    @Autowired
    SreAccessService sreAccessService;

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
                .dataFrom(DataFrom.of(from).getValue())
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
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> delete(User loginUser, long projectCode, Integer clusterCode, Integer code) {
        Result<Boolean> result = new Result<>();

        projectService.checkHasProjectWritePermissionThrowException(loginUser, projectCode);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, clusterCode);
            return result;
        }
        ProjectNode node = projectNodeMapper.selectById(code);

        if (node == null) {
            putMsg(result, Status.PROJECT_NODE_NOT_EXIST, code);
            return result;
        }

        if (projectNodeMapper.deleteById(code) > 0) {
            projectNodeParameterService.deleteParametersByNodeCode(loginUser, projectCode, code);
            if (node.getDataSourceCode() != null && node.getDataSourceCode() != 0) {
                dataSourceService.delete(loginUser, node.getDataSourceCode());
            }
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

    @Override
    public Result<Boolean> syncNodes(User loginUser, long projectCode, int clusterCode) {
        Result<Boolean> result = new Result<>();
        result.setData(false);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, clusterCode);
            return result;
        }

        Result<List<StellarOpsNodeInfo>> platFormNodes = platformOpenApi.queryNodeListByPlatform(loginUser,
                project.getCode(), cluster.getClusterId());

        if (platFormNodes.getCode() != Status.SUCCESS.getCode()) {
            putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, platFormNodes.getMsg());
            return result;
        }

        List<StellarOpsNodeInfo> platNodes = platFormNodes.getData();
        if (platNodes == null || platNodes.isEmpty()) {
            putMsg(result, Status.INTERNAL_SERVER_ERROR_ARGS, "No nodes found in platform.");
            return result;
        }

        List<ProjectNode> projectAutoNodeList = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>().lambda()
                        .eq(ProjectNode::getClusterCode, clusterCode)
                        .eq(ProjectNode::getDataFrom, DataFrom.AUTO.getValue()));

        List<ProjectNode> needInserList = new java.util.ArrayList<>();
        List<ProjectNode> needDeleteList = new java.util.ArrayList<>();
        if (projectAutoNodeList == null) {
            projectAutoNodeList = new java.util.ArrayList<>();
        }
        for (StellarOpsNodeInfo node : platNodes) {
            ProjectNode projectNode = projectAutoNodeList.stream()
                    .filter(p -> p.getNodeId().equals(node.getNodeId()))
                    .findFirst()
                    .orElse(null);

            if (projectNode == null) {
                needInserList.add(
                        buildNewProjectNode(loginUser, projectCode, clusterCode, node.getNodeName(), node.getNodeKey(),
                                node.getNodeId(), DataFrom.AUTO.getValue(), "一键同步-AUTO", cluster));
            }
        }

        for (ProjectNode projectNode : projectAutoNodeList) {
            StellarOpsNodeInfo node = platNodes.stream()
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

        return Result.success(true);
    }

    @Override
    public Result<List<ProjectNodeParameter>> getHalleyParams(String ip) {
        List<HalleyServerInfo> detailInfo = halleyAccessService.getAssetsInfoByIps(Arrays.asList(ip));
        if (detailInfo == null || detailInfo.isEmpty()) {
            return Result.success(new ArrayList<>());
        }

        HalleyServerInfo info = detailInfo.get(0);
        List<ProjectNodeParameter> params = new ArrayList<>();
        params.add(ProjectNodeParameter.builder()
                .paramName("halley_hostName")
                .paramValue(info.getHostName())
                .build());
        params.add(ProjectNodeParameter.builder()
                .paramName("halley_cpu")
                .paramValue(info.getCpu().toString())
                .build());
        params.add(ProjectNodeParameter.builder()
                .paramName("halley_memory")
                .paramValue(info.getMemory().toString())
                .build());
        params.add(ProjectNodeParameter.builder()
                .paramName("halley_zone")
                .paramValue(info.getZone())
                .build());

        return Result.success(params);
    }

    @Override
    public Result<Boolean> syncNodesByHalley(User loginUser, long projectCode, int clusterCode) {
        Result<Boolean> result = new Result<>();
        result.setData(false);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, project.getName(), clusterCode);
            return result;
        }

        if (cluster.getAppId() == null || cluster.getAppId().isEmpty()) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST_APPID, project.getName(), cluster.getClusterName());
            return result;
        }

        List<AssetsInfo> halleyAssertNodes = halleyAccessService.getAssetsInfoByAppId(cluster.getAppId());
        if (halleyAssertNodes == null || halleyAssertNodes.isEmpty()) {
            putMsg(result, Status.PROJECT_NODE_SOURCE_NO_HALLEY, project.getName(), cluster.getClusterName(),
                    cluster.getAppId());
            return result;
        }

        List<ProjectNode> projectHalleyNodeList = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>().lambda()
                        .eq(ProjectNode::getClusterCode, clusterCode)
                        .eq(ProjectNode::getDataFrom, DataFrom.HALLEY.getValue()));

        Integer maxId = 0;
        if (projectHalleyNodeList == null) {
            projectHalleyNodeList = new java.util.ArrayList<>();
        }
        if (projectHalleyNodeList.size() > 0) {
            maxId = projectHalleyNodeList.stream().max((a, b) -> a.getId().compareTo(b.getId())).get().getId();
        }

        List<ProjectNode> needInserList = new java.util.ArrayList<>();
        for (AssetsInfo assertNode : halleyAssertNodes) {
            ProjectNode node = projectHalleyNodeList.stream()
                    .filter(p -> p.getNodeKey().equals(assertNode.getIp())
                            && p.getDataFrom().equalsIgnoreCase(DataFrom.HALLEY.getValue()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                maxId = maxId + 1;
                ProjectNode projectNode = new ProjectNode();
                projectNode.setProjectCode(projectCode);
                projectNode.setClusterId(cluster.getClusterId());
                projectNode.setClusterCode(clusterCode);
                projectNode.setNodeId(cluster.getId() + "-" + maxId.toString());
                projectNode.setNodeKey(assertNode.getIp());
                projectNode.setNodeName(assertNode.getHostName());
                projectNode.setDataFrom(DataFrom.HALLEY.getValue());
                projectNode.setDescription("一键同步-Halley");
                projectNode.setUserId(loginUser.getId());
                projectNode.setUserName(loginUser.getUserName());
                projectNode.setCreateTime(new Date());
                projectNode.setUpdateTime(new Date());
                needInserList.add(projectNode);
            }
        }

        List<ProjectNode> needDeleteList = new java.util.ArrayList<>();
        for (ProjectNode halleyNode : projectHalleyNodeList) {
            AssetsInfo node = halleyAssertNodes.stream()
                    .filter(p -> p.getIp().equalsIgnoreCase(halleyNode.getNodeKey()))
                    .findFirst()
                    .orElse(null);

            if (node == null) {
                needDeleteList.add(halleyNode);
            }
        }

        log.info("Sync project node from halley, projectCode:{}, clusterCode:{}, needInserList:{} needDeleteList:{}",
                projectCode, clusterCode, needInserList.size(), needDeleteList.size());

        if (!needInserList.isEmpty()) {
            needInserList.forEach(p -> projectNodeMapper.insert(p));
        }

        if (!needDeleteList.isEmpty()) {
            needDeleteList.forEach(p -> projectNodeMapper.deleteById(p.getId()));
        }

        return Result.success(true);

    }

    @Override
    public Result<Map<String, Boolean>> testConnect(User loginUser, long projectCode, int clusterCode) {
        Result<Map<String, Boolean>> result = new Result<>();
        final Map<String, Boolean> resultMap = new ConcurrentHashMap<>();
        result.setData(resultMap);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, project.getName(), clusterCode);
            return result;
        }

        if (cluster.getAppId() == null || cluster.getAppId().isEmpty()) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST_APPID, project.getName(), cluster.getClusterName());
            return result;
        }

        List<ProjectNode> projectNodes = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>().lambda().eq(ProjectNode::getClusterCode, clusterCode));
        if (projectNodes == null || projectNodes.isEmpty()) {
            putMsg(result, Status.PROJECT_NODE_NOT_EXIST, project.getName(), cluster.getClusterName());
            return result;
        }
        projectNodes.removeIf(p -> p.getDataSourceCode() == null || p.getDataSourceCode() == 0);
        if (projectNodes.isEmpty()) {
            return Result.success();
        }

        asyncExector(resultMap, projectNodes);

        return Result.success(resultMap);
    }

    private void asyncExector(final Map<String, Boolean> resultMap, List<ProjectNode> projectNodes) {
        final Integer poolSize = projectNodes.size() > 10 ? 10 : projectNodes.size();

        ExecutorService executorService = Executors.newFixedThreadPool(poolSize); // 创建一个固定大小的线程池
        try {
            List<CompletableFuture<Void>> futures = projectNodes.stream()
                    .map(node -> CompletableFuture.runAsync(() -> {
                        try {
                            dataSourceService.connectionTest(node.getDataSourceCode());
                            resultMap.put(node.getId() + "", true);
                        } catch (ServiceException e) {
                            resultMap.put(node.getId() + "", false);
                        }
                    }, executorService))
                    .collect(Collectors.toList());

            // 等待所有任务完成
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
        } finally {
            executorService.shutdown();
            try {
                if (!executorService.awaitTermination(10, TimeUnit.SECONDS)) {
                    executorService.shutdownNow();
                }
            } catch (InterruptedException e) {
                executorService.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }
    }

    @Override
    public Result<Boolean> createSourceWithAllNode(User loginUser, long projectCode, int clusterCode) {
        Result<Boolean> result = new Result<>();
        result.setData(false);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, project.getName(), clusterCode);
            return result;
        }

        if (cluster.getAppId() == null || cluster.getAppId().isEmpty()) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST_APPID, project.getName(), cluster.getClusterName());
            return result;
        }

        List<ProjectNode> projectNodes = projectNodeMapper
                .selectList(new QueryWrapper<ProjectNode>().lambda().eq(ProjectNode::getClusterCode, clusterCode));
        projectNodes.removeIf(p -> p.getDataSourceCode() != null && p.getDataSourceCode() != 0);
        List<String> errorIp = new ArrayList<>();
        if (!projectNodes.isEmpty()) {
            for (ProjectNode node : projectNodes) {

                String ip = tryGetNodeIp(node);
                boolean isSuccess = "success".equalsIgnoreCase(sreAccessService.pastePubRsa(ip));
                BaseDataSourceParamDTO dataSourceParam = convertSystemDataSource(project, cluster, node);
                Integer dsId = dataSourceService.createDataSource(loginUser, dataSourceParam).getId();
                node.setDataSourceCode(dsId);
                projectNodeMapper.updateById(node);

                if (!isSuccess) {
                    errorIp.add(dataSourceParam.getName());
                }
            }
        }

        if (!errorIp.isEmpty()) {
            putMsg(result, Status.PROJECT_NODE_SOURCE_CREATE_ERROR, String.join(";", errorIp));
            return result;
        }

        return Result.success(true);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Result<Boolean> createSourceWithNode(User loginUser, long projectCode, int clusterCode, int code) {
        Result<Boolean> result = new Result<>();
        result.setData(false);

        Project project = projectMapper.queryByCode(projectCode);

        projectService.checkHasProjectWritePermissionThrowException(loginUser, project);

        ProjectCluster cluster = clusterMapper.selectById(clusterCode);
        if (cluster == null) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST, project.getName(), clusterCode);
            return result;
        }

        if (cluster.getAppId() == null || cluster.getAppId().isEmpty()) {
            putMsg(result, Status.PROJECT_CLUSTER_NOT_EXIST_APPID, project.getName(), cluster.getClusterName());
            return result;
        }

        ProjectNode node = projectNodeMapper.selectById(code);
        if (node == null) {
            putMsg(result, Status.PROJECT_NODE_NOT_EXIST, code);
            return result;
        }

        if (node.getDataSourceCode() != null && node.getDataSourceCode() != 0) {
            putMsg(result, Status.PROJECT_NODE_SOURCE_EXIST, project.getName(), cluster.getClusterName(),
                    node.getNodeName());
            return result;
        }

        String ip = tryGetNodeIp(node);

        boolean pasteSuccess = "success".equalsIgnoreCase(sreAccessService.pastePubRsa(ip));
        String errorMsg = "Paste public rsa failed. Please paste platform public rsa to node.";

        BaseDataSourceParamDTO dataSourceParam = convertSystemDataSource(project, cluster, node);
        Integer dsId = dataSourceService.createDataSource(loginUser, dataSourceParam).getId();
        node.setDataSourceCode(dsId);
        projectNodeMapper.updateById(node);

        if (!pasteSuccess) {
            putMsg(result, Status.PROJECT_NODE_SOURCE_CREATE_ERROR, dataSourceParam.getName());
            return result;
        }
        return Result.success(true);
    }

    private String tryGetNodeIp(ProjectNode node) {
        //
        List<ProjectNodeParameter> params = projectNodeParameterService.queryParameterList(node.getProjectCode(),
                node.getId()).getData();
        if (!params.isEmpty()) {
            for (ProjectNodeParameter param : params) {
                if (param.getParamName().equals("platform_sre_ip")) {
                    return param.getParamValue();
                }
            }
        }
        return node.getNodeKey();
    }

    public static BaseDataSourceParamDTO convertSystemDataSource(Project project, ProjectCluster cluster,
            ProjectNode node) {
        Map<String, String> paramMap = new HashMap<>();
        paramMap.put("type", "SSH");
        paramMap.put("label", "SSH");
        // name 不能重复
        paramMap.put("name", node.getId() + "-" + node.getNodeName());
        paramMap.put("note", "AUTO CREATE. ProjcetName:" + project.getName()
                + ", ClusterName:" + cluster.getClusterName()
                + ", NodeName:" + node.getNodeName());
        paramMap.put("host", node.getNodeKey());
        paramMap.put("port", "22");
        paramMap.put("principal", "");
        paramMap.put("javaSecurityKrb5Conf", "");
        paramMap.put("loginUserKeytabUsername", "");
        paramMap.put("loginUserKeytabPath", "");
        paramMap.put("mode", "");
        paramMap.put("userName", "root");
        paramMap.put("password", "");
        paramMap.put("database", "");
        paramMap.put("connectType", "");
        paramMap.put("other", null);
        paramMap.put("endpoint", "");
        paramMap.put("MSIClientId", "");
        paramMap.put("dbUser", "");
        paramMap.put("datawarehouse", "");
        paramMap.put("publicKey", Constants.DATASOURCE_PUBLIC_KEY_FLAG);
        String jsonStr = JSONObject.toJSONString(paramMap);
        return DataSourceUtils.buildDatasourceParam(jsonStr);
    }

    @Override
    public List<ProjectNode> queryNodeListByDataSourceCodes(Integer... dataSourceCodes) {
        QueryWrapper<ProjectNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().in(ProjectNode::getDataSourceCode, (Object[]) dataSourceCodes);
        return projectNodeMapper.selectList(queryWrapper);
    }

    @Override
    public List<ProjectNode> queryNodesByProjectCode(long projectCode) {
        QueryWrapper<ProjectNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectNode::getProjectCode, projectCode);
        return projectNodeMapper.selectList(queryWrapper);
    }

    @Override
    public Result<ProjectNode> queryNodeByCode(long projectCode, Integer code) {

        QueryWrapper<ProjectNode> queryWrapper = new QueryWrapper<>();
        queryWrapper.lambda().eq(ProjectNode::getProjectCode, projectCode).eq(ProjectNode::getId, code);
        ProjectNode projectNode = projectNodeMapper.selectOne(queryWrapper);

        return Result.success(projectNode);
    }
}
