package org.apache.dolphinscheduler.api.platform;

import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_PROPS_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.NODE_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.NODE_PARAM_PROPS_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.PARAM_VALUE_SEPARATOR;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.DATASOURCE_PARAM_NAME;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.dolphinscheduler.api.platform.common.RestParamEntry;
import org.apache.dolphinscheduler.api.service.ProjectClusterParameterService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.api.service.ProjectParameterService;
import org.apache.dolphinscheduler.api.utils.PageInfo;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.constants.PlatformConstant;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectClusterParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import autovalue.shaded.org.jetbrains.annotations.NotNull;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlatformTaskParamHelper {

    @Autowired
    private ProjectNodeService projectNodeService;

    @Autowired
    PlatformRestService platformRestService;

    @Autowired
    private ProjectClusterService projectClusterService;

    @Autowired
    private ProjectClusterParameterService pProjectClusterService;

    @Autowired
    private ProjectNodeParameterService pProjectNodeService;

    @Autowired
    private ProjectParameterService projectParameterService;

    public void tryFillPlatformParams(final Map<String, String> startParams, final long projectCode) {
        if (startParams == null ||
                !(startParams.containsKey(NODE_PARAM_NAME) || startParams.containsKey(CLUSTER_PARAM_NAME))) {
            return;
        }

        // TODO: 检查 startParams >> projectParams 中是否有禁止使用的参数
        // TODO: platform_api_params_disable 为true时不填充平台参数
        // TODO: platform_halley_params_disable 为true时不填充halley参数

        final List<ProjectNode> projectAllNodes = projectNodeService.queryNodesByProjectCode(projectCode);
        final List<ProjectCluster> projectClusters = projectClusterService.queryClusterListPaging(projectCode)
                .getData();

        final List<Integer> nodeIdsInt = startParams.containsKey(NODE_PARAM_NAME)
                ? Arrays.asList(startParams.get(NODE_PARAM_NAME).split(PARAM_VALUE_SEPARATOR))
                        .stream().map(this::parseInterger).sorted().collect(Collectors.toList())
                : new ArrayList<>();
        final List<ProjectNode> taskNodes = tryCollectorNode(nodeIdsInt, projectAllNodes);

        final List<Integer> clusterIdsInt = startParams.containsKey(CLUSTER_PARAM_NAME)
                ? Arrays.asList(startParams.get(CLUSTER_PARAM_NAME).split(PARAM_VALUE_SEPARATOR))
                        .stream().map(this::parseInterger).sorted().collect(Collectors.toList())
                : new ArrayList<>();

        final Map<Integer, ProjectCluster> clusterInfos = tryCollectorClusterInfo(clusterIdsInt, projectClusters);
        final Map<Integer, ProjectCluster> nodeClusterInfos = tryCollectorClusterInfo(
                taskNodes.stream().map(ProjectNode::getClusterCode).distinct().collect(Collectors.toList()),
                projectClusters);

        Map<Integer, List<ProjectClusterParameter>> taskClusterParamsMap = new HashMap<>();
        Map<Integer, List<ProjectNodeParameter>> taskNodeParamsMap = new HashMap<>();
        List<ProjectParameter> taskPlatformParams = new ArrayList<>();

        fillParamsMap(projectCode, taskNodes, new ArrayList<>(clusterInfos.values()),
                taskClusterParamsMap,
                taskNodeParamsMap,
                taskPlatformParams);

        List<Map<String, String>> appendNodeParamsList = new ArrayList<>();
        List<Map<String, String>> appendClusterParamsList = new ArrayList<>();

        clusterInfos.values().stream()
                .map(cluster -> collectorClusterParam(taskPlatformParams,
                        taskClusterParamsMap.get(cluster.getId()), cluster))
                .forEach(appendClusterParamsList::add);

        taskNodes.stream()
                .map(node -> collectorNodeParam(
                        nodeClusterInfos.get(node.getClusterCode()),
                        taskPlatformParams,
                        taskNodeParamsMap.containsKey(node.getId())
                                ? taskNodeParamsMap.get(node.getId())
                                : new ArrayList<>(),
                        node))
                .forEach(appendNodeParamsList::add);

        // 移除所有value为空的参数
        appendNodeParamsList.forEach(
                map -> map.entrySet().removeIf(entry -> entry.getValue() == null));
        appendClusterParamsList.forEach(
                map -> map.entrySet().removeIf(entry -> entry.getValue() == null));

        if (nodeIdsInt.size() > 0) {
            // 一个节点时 value为 object & 所有
            if (nodeIdsInt.size() == 1) {
                Map<String, String> nodeParam = appendNodeParamsList.size() > 0
                        ? appendNodeParamsList.get(0)
                        : new HashMap<>();
                nodeParam.forEach((k, v) -> startParams.put(NODE_PARAM_NAME + "." + k, v));
            } else {
                // 多个节点时 value为 array
                startParams.put(NODE_PARAM_PROPS_NAME, JSONUtils.toJsonString(appendNodeParamsList));
            }
        }

        // platform.datasource fill
        List<Integer> sourceIds = taskNodes.stream().map(ProjectNode::getDataSourceCode).distinct()
                .collect(Collectors.toList());
        if (sourceIds.size() > 0 && startParams.containsKey(DATASOURCE_PARAM_NAME)) {
            String ids = startParams.getOrDefault(DATASOURCE_PARAM_NAME, "");
            Set<String> setId = new HashSet<>(Arrays.asList(ids.split(PARAM_VALUE_SEPARATOR)));
            setId.addAll(sourceIds.stream().map(String::valueOf).collect(Collectors.toSet()));
            setId.removeIf(s -> s.isEmpty() || !s.matches("\\d+"));
            startParams.put(DATASOURCE_PARAM_NAME, String.join(PARAM_VALUE_SEPARATOR, setId));
        }

        if (clusterIdsInt.size() > 0) {
            // 一个集群时 value为 object
            if (clusterIdsInt.size() == 1) {
                Map<String, String> clusterParam = appendClusterParamsList.size() > 0
                        ? appendClusterParamsList.get(0)
                        : new HashMap<>();
                clusterParam.forEach((k, v) -> startParams.put(CLUSTER_PARAM_NAME + "." + k, v));
            } else {
                // 多个集群时 value为 array
                startParams.put(CLUSTER_PARAM_PROPS_NAME, JSONUtils.toJsonString(appendClusterParamsList));
            }
        }
    }

    private Map<String, String> collectorClusterParam(
            List<ProjectParameter> taskPlatformParams,
            List<ProjectClusterParameter> taskClusterParams,
            ProjectCluster cluster) {
        return collectorClusterParam(cluster, taskPlatformParams, taskClusterParams).stream()
                .collect(Collectors.toMap(ProjectClusterParameter::getParamName,
                        ProjectClusterParameter::getParamValue));
    }

    /**
     * 获取集群的完整参数列表
     * 
     * @param cluster
     * @return
     */
    public List<ProjectClusterParameter> collectorClusterParam(@NotNull ProjectCluster cluster) {
        return collectorClusterParam(cluster, null, null);
    }

    private List<ProjectClusterParameter> collectorClusterParam(@NotNull ProjectCluster cluster,
            List<ProjectParameter> taskPlatformParams,
            List<ProjectClusterParameter> taskClusterParams) {
        long projectCode = cluster.getProjectCode();
        Integer clusterCode = cluster.getId();
        List<ProjectClusterParameter> result = new ArrayList<>();

        if (taskClusterParams == null) {
            taskClusterParams = pProjectClusterService.queryParameterList(projectCode, clusterCode).getData();
        }

        // 平台参数用于构建rest请求
        if (taskPlatformParams == null) {
            taskPlatformParams = projectParameterService.queryProjectParameterListPagingWithOutUser(projectCode, 200, 1,
                    PlatformConstant.PLATFORM_PARAM_PRIFEX).getData().getTotalList();
        }

        Map<String, ProjectClusterParameter> paramterMap = new HashMap<>();
        // 注意顺序 集群自定义参数 最高优先级最后put
        // 1. Rest 接口参数
        Result<Map<String, Object>> restResult = platformRestService.getRest(RestParamEntry.newEntry()
                .build(cluster.getClusterId(), null, null)
                .buildRestParamEntiy(taskPlatformParams), PathEnum.CLUSTER_PARAMS);
        if (restResult.getData() != null) {
            paramterMap.putAll(restResult.getData().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e -> {
                        ProjectClusterParameter p = new ProjectClusterParameter();
                        p.setParamName(e.getKey());
                        p.setParamValue(e.getValue().toString());
                        p.setDescription("FROM_API");
                        return p;
                    })));
        }
        // 2. 自定义参数
        for (ProjectClusterParameter p : taskClusterParams) {
            paramterMap.put(p.getParamName(), p);
        }
        // 3. 配置在stellarops的设置参数 优先级最高
        Map<String, String> params = systemClusterParam(cluster);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            ProjectClusterParameter p = new ProjectClusterParameter();
            p.setParamName(entry.getKey());
            p.setParamValue(entry.getValue());
            p.setDescription("SYSTEM");
            paramterMap.put(entry.getKey(), p);
        }
        result.addAll(paramterMap.values());
        return result;
    }

    public static Map<String, String> systemClusterParam(ProjectCluster cluster) {
        Map<String, String> params = new HashMap<>();
        params.put("cluster_name", cluster.getClusterName());
        params.put("cluster_id", cluster.getClusterId());
        params.put("cluster_appid", cluster.getAppId());
        params.put("cluster_code", String.valueOf(cluster.getId()));
        return params;
    }

    /**
     * 获取节点的完整参数列表
     * 
     * @param node
     * @return
     */
    public List<ProjectNodeParameter> collectorNodeParam(Long projectCode, Integer nodeCode) {
        ProjectNode node = projectNodeService.queryNodeByCode(projectCode, nodeCode).getData();
        return collectorNodeParam(node, null, null, null);
    }

    /**
     * 获取节点的完整参数列表
     * 
     * @param node
     * @return
     */
    public List<ProjectNodeParameter> collectorNodeParam(@NotNull ProjectNode node) {
        return collectorNodeParam(node, null, null, null);
    }

    private List<ProjectNodeParameter> collectorNodeParam(
            @NotNull ProjectNode node,
            ProjectCluster cluster,
            List<ProjectParameter> taskPlatformParams,
            List<ProjectNodeParameter> taskNodeDBParams) {

        long projectCode = node.getProjectCode();
        Integer nodeId = node.getId();
        List<ProjectNodeParameter> result = new ArrayList<>();

        if (taskNodeDBParams == null) {
            taskNodeDBParams = pProjectNodeService.queryParameterList(projectCode, nodeId).getData();
        }
        if (cluster == null) {
            cluster = projectClusterService.queryClusterByCode(projectCode, node.getClusterCode()).getData();
        }
        if (taskPlatformParams == null) {
            taskPlatformParams = projectParameterService.queryProjectParameterListPagingWithOutUser(projectCode, 200, 1,
                    PlatformConstant.PLATFORM_PARAM_PRIFEX).getData().getTotalList();
        }

        Map<String, ProjectNodeParameter> paramterMap = new HashMap<>();
        // 注意顺序 节点自定义参数 最高优先级最后put
        // 1. Rest 接口参数
        Result<Map<String, Object>> restResult = platformRestService.getRest(RestParamEntry.newEntry()
                .build(cluster.getClusterId(), String.valueOf(node.getNodeId()), null)
                .buildRestParamEntiy(taskPlatformParams), PathEnum.NODE_PARAMS);
        if (restResult.getData() != null) {
            paramterMap.putAll(restResult.getData().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e -> {
                        ProjectNodeParameter p = new ProjectNodeParameter();
                        p.setParamName(e.getKey());
                        p.setParamValue(e.getValue().toString());
                        p.setDescription("FROM_API");
                        return p;
                    })));
        }
        // 2. 自定义参数
        for (ProjectNodeParameter p : taskNodeDBParams) {
            paramterMap.put(p.getParamName(), p);
        }
        // 3. halley 参数
        String ip = node.getNodeKey();
        if (paramterMap.containsKey("platform_sre_ip")) {
            ip = paramterMap.get("platform_sre_ip").getParamValue();
        }
        Result<List<ProjectNodeParameter>> halleyParams = projectNodeService.getHalleyParams(ip);
        if (halleyParams.getData() != null) {
            for (ProjectNodeParameter p : halleyParams.getData()) {
                p.setDescription("FROM_HALLEY");
                paramterMap.put(p.getParamName(), p);
            }
        }
        // 4. 配置在stellarops的设置参数 优先级最高
        Map<String, String> params = systemNodeParam(cluster, node);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            ProjectNodeParameter p = new ProjectNodeParameter();
            p.setParamName(entry.getKey());
            p.setParamValue(entry.getValue());
            p.setDescription("SYSTEM");
            paramterMap.put(entry.getKey(), p);
        }
        result.addAll(paramterMap.values());
        return result;
    }

    public static Map<String, String> systemNodeParam(@NotNull ProjectCluster cluster, @NotNull ProjectNode node) {
        Map<String, String> params = new HashMap<>();
        params.putAll(systemClusterParam(cluster));
        params.put(PlatformConstant.NODE_DATASOURCE_PARAM_NAME,
                node.getDataSourceCode() == null ? "-1" : String.valueOf(node.getDataSourceCode()));
        params.put("node_name", node.getNodeName());
        params.put("node_id", String.valueOf(node.getNodeId()));
        params.put("node_key", node.getNodeKey());
        params.put("node_code", String.valueOf(node.getId()));
        return params;
    }

    private Map<String, String> collectorNodeParam(
            @NotNull ProjectCluster cluster,
            @NotNull List<ProjectParameter> taskPlatformParams,
            @NotNull List<ProjectNodeParameter> taskNodeDBParams,
            @NotNull ProjectNode node) {
        Map<String, String> params = new HashMap<>();
        List<ProjectNodeParameter> nodeParams = collectorNodeParam(node, cluster, taskPlatformParams, taskNodeDBParams);
        for (ProjectNodeParameter p : nodeParams) {
            params.put(p.getParamName(), p.getParamValue());
        }
        return params;
    }

    private void fillParamsMap(final long projectCode,
            final List<ProjectNode> taskNodes, final List<ProjectCluster> taskClusters,
            Map<Integer, List<ProjectClusterParameter>> taskClusterParamsMap,
            Map<Integer, List<ProjectNodeParameter>> taskNodeParamsMap,
            List<ProjectParameter> taskPlatformParamsMap) {

        Set<ProjectNode> taskSourceNodesAll = new HashSet<>(taskNodes);

        Result<PageInfo<ProjectParameter>> platformParamList = projectParameterService
                .queryProjectParameterListPagingWithOutUser(projectCode, 100, 1,
                        PlatformConstant.PLATFORM_PARAM_PRIFEX);

        if (platformParamList.getData() != null) {
            taskPlatformParamsMap.addAll(platformParamList.getData().getTotalList());
        }

        taskClusters.forEach(cluster -> {
            taskClusterParamsMap.put(cluster.getId(),
                    pProjectClusterService.queryParameterList(projectCode, cluster.getId()).getData());
        });

        taskSourceNodesAll.stream().map(ProjectNode::getClusterCode).distinct()
                .forEach(clusterCode -> {
                    if (!taskClusterParamsMap.containsKey(clusterCode)) {
                        taskClusterParamsMap.put(clusterCode,
                                pProjectClusterService.queryParameterList(projectCode, clusterCode).getData());
                    }
                });

        taskSourceNodesAll.stream().map(ProjectNode::getId).distinct()
                .forEach(nodeId -> {
                    if (!taskNodeParamsMap.containsKey(nodeId)) {
                        taskNodeParamsMap.put(nodeId,
                                pProjectNodeService.queryParameterList(projectCode, nodeId).getData());
                    }
                });
    }

    private List<ProjectNode> tryCollectorNode(List<Integer> nodeIdsInt, final List<ProjectNode> projectNodes) {
        final List<Integer> projectNodeIds = projectNodes.stream().map(ProjectNode::getId).sorted()
                .collect(Collectors.toList());
        if (nodeIdsInt.size() == 0) {
            return Collections.emptyList();
        } else if (nodeIdsInt.size() > 0 && !projectNodeIds.containsAll(nodeIdsInt)) {
            String errorMsg = "some node id not in the project! "
                    + "projectNodeIds: " + projectNodeIds
                    + ", nodeIdsInt: " + nodeIdsInt;
            log.error(errorMsg);
            return Collections.emptyList();
        } else {
            return projectNodes.stream().filter(node -> nodeIdsInt.contains(node.getId())).collect(Collectors.toList());
        }
    }

    private Map<Integer, ProjectCluster> tryCollectorClusterInfo(List<Integer> clusterIdsInt,
            List<ProjectCluster> projectClusters) {
        final List<Integer> projectClusterIds = projectClusters.stream().map(ProjectCluster::getId).sorted()
                .collect(Collectors.toList());

        if (clusterIdsInt.size() == 0) {
            return Collections.emptyMap();
        } else if (clusterIdsInt.size() > 0 && !projectClusterIds.containsAll(clusterIdsInt)) {
            String errorMsg = "some cluster id not in the project! "
                    + "projectClusterIds: " + projectClusterIds
                    + ", clusterIdsInt: " + clusterIdsInt;
            log.error(errorMsg);
            return Collections.emptyMap();
        } else {
            return projectClusters.stream().filter(cluster -> clusterIdsInt.contains(cluster.getId()))
                    .collect(Collectors.toMap(ProjectCluster::getId, i -> i));
        }
    }

    private Integer parseInterger(String s) {
        try {
            return Integer.parseInt(s);
        } catch (Exception e) {
            log.error("parse datasource id error" + s, e);
            return -1;
        }
    }
}
