package org.apache.dolphinscheduler.api.platform;

import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_PROPS_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.DATASOURCE_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.NODE_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.NODE_PARAM_PROPS_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.PARAM_VALUE_SEPARATOR;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.dolphinscheduler.api.service.ProjectClusterParameterService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectClusterParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class PlatformTaskParamHelper {

    @Autowired
    private ProjectNodeService projectNodeService;

    @Autowired
    private ProjectClusterService projectClusterService;

    @Autowired
    private ProjectClusterParameterService pProjectClusterService;

    @Autowired
    private ProjectNodeParameterService pProjectNodeService;

    public void tryFillPlatformParams(Map<String, String> startParams, final long projectCode) {
        if (startParams == null) {
            startParams = new HashMap<>();
        }

        if (!(startParams.containsKey(NODE_PARAM_NAME) || startParams.containsKey(CLUSTER_PARAM_NAME))) {
            return;
        }

        final List<ProjectNode> projectAllNodes = projectNodeService.queryNodesByProjectCode(projectCode);
        final List<ProjectCluster> projectAllClusters = projectClusterService.queryClusterListPaging(projectCode)
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
        final Map<Integer, ProjectCluster> taskClusters = tryCollectorCluster(clusterIdsInt, projectAllClusters);

        Map<Integer, List<ProjectClusterParameter>> taskClusterParamsMap = new HashMap<>();
        Map<Integer, List<ProjectNodeParameter>> taskNodeParamsMap = new HashMap<>();
        fillParamsMap(projectCode, taskNodes, new ArrayList<>(taskClusters.values()),
                taskClusterParamsMap,
                taskNodeParamsMap);
        // append datasource params
        List<Map<String, String>> appendNodeParamsList = new ArrayList<>();
        List<Map<String, String>> appendClusterParamsList = new ArrayList<>();

        taskNodes.stream()
                .map(node -> collectorNodeParam(taskClusters.get(node.getClusterCode()), taskNodeParamsMap, node))
                .forEach(appendNodeParamsList::add);

        taskClusters.values().stream()
                .map(cluster -> collectorClusterParam(taskClusterParamsMap, cluster.getId(), cluster))
                .forEach(appendClusterParamsList::add);

        if (nodeIdsInt.size() > 0) {
            // 一个节点时 value为 object
            if (nodeIdsInt.size() == 1) {
                Map<String, String> nodeParam = appendNodeParamsList.size() > 0
                        ? appendNodeParamsList.get(0)
                        : new HashMap<>();
                startParams.put(NODE_PARAM_PROPS_NAME, JSONUtils.toJsonString(nodeParam));
            } else {
                // 多个节点时 value为 array
                startParams.put(NODE_PARAM_PROPS_NAME, JSONUtils.toJsonString(appendNodeParamsList));
            }
        }

        if (clusterIdsInt.size() > 0) {
            // 一个集群时 value为 object
            if (clusterIdsInt.size() == 1) {
                Map<String, String> clusterParam = appendClusterParamsList.size() > 0
                        ? appendClusterParamsList.get(0)
                        : new HashMap<>();
                startParams.put(CLUSTER_PARAM_PROPS_NAME, JSONUtils.toJsonString(clusterParam));
            } else {
                // 多个集群时 value为 array
                startParams.put(CLUSTER_PARAM_PROPS_NAME, JSONUtils.toJsonString(appendClusterParamsList));
            }
        }
    }

    private Map<String, String> collectorClusterParam(
            Map<Integer, List<ProjectClusterParameter>> taskClusterParamsMap,
            Integer clusterId, ProjectCluster cluster) {
        Map<String, String> params = new HashMap<>();
        // 注意顺序 集群自定义参数 最高优先级最后put
        // 1. 集群默认参数
        params.put("cluster_name", cluster.getClusterName());
        params.put("cluster_id", cluster.getClusterId());
        params.put("appid", cluster.getAppId());
        // 2. 集群自定义参数
        params.putAll(taskClusterParamsMap.containsKey(clusterId)
                ? taskClusterParamsMap.get(clusterId).stream()
                        .collect(Collectors.toMap(ProjectClusterParameter::getParamName,
                                ProjectClusterParameter::getParamValue))
                : new HashMap<>());
        // 3. id 必须为 cluster.id
        params.put("id", String.valueOf(cluster.getId()));
        return params;
    }

    private Map<String, String> collectorNodeParam(
            ProjectCluster cluster,
            Map<Integer, List<ProjectNodeParameter>> taskNodeParamsMap,
            ProjectNode node) {
        Map<String, String> params = new HashMap<>();
        // 注意顺序 节点自定义参数 最高优先级最后put
        // 1. 集群默认参数
        params.put("cluster_name", cluster.getClusterName());
        params.put("cluster_id", cluster.getClusterId());
        params.put("appid", cluster.getAppId());
        params.put("cluster_code", String.valueOf(cluster.getId()));
        // 2. 节点默认参数
        params.put("node_name", node.getNodeName());
        params.put("node_id", String.valueOf(node.getNodeId()));
        params.put("node_key", node.getNodeKey());
        // 3. 节点自定义参数
        params.putAll(taskNodeParamsMap.containsKey(node.getId())
                ? taskNodeParamsMap.get(node.getId()).stream()
                        .collect(Collectors.toMap(ProjectNodeParameter::getParamName,
                                ProjectNodeParameter::getParamValue))
                : new HashMap<>());
        // 4. id 必须为 node.id
        params.put("id", String.valueOf(node.getId()));
        return params;
    }

    private void fillParamsMap(final long projectCode,
            final List<ProjectNode> taskNodes, final List<ProjectCluster> taskClusters,
            Map<Integer, List<ProjectClusterParameter>> taskClusterParamsMap,
            Map<Integer, List<ProjectNodeParameter>> taskNodeParamsMap) {

        Set<ProjectNode> taskSourceNodesAll = new HashSet<>(taskNodes);

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

    private Map<Integer, ProjectCluster> tryCollectorCluster(List<Integer> clusterIdsInt,
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
