package org.apache.dolphinscheduler.api.platform;

import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_NAME;
import static org.apache.dolphinscheduler.common.constants.PlatformConstant.CLUSTER_PARAM_PROPS_NAME;
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

import org.apache.dolphinscheduler.api.platform.common.RestParamEntry;
import org.apache.dolphinscheduler.api.service.ProjectClusterParameterService;
import org.apache.dolphinscheduler.api.service.ProjectClusterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeParameterService;
import org.apache.dolphinscheduler.api.service.ProjectNodeService;
import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.dao.entity.ProjectCluster;
import org.apache.dolphinscheduler.dao.entity.ProjectClusterParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.ProjectNodeParameter;
import org.apache.dolphinscheduler.dao.entity.ProjectParameter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

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

    public void tryFillPlatformParams(final Map<String, String> startParams, final long projectCode) {
        if (startParams == null ||
                !(startParams.containsKey(NODE_PARAM_NAME) || startParams.containsKey(CLUSTER_PARAM_NAME))) {
            return;
        }

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
        fillParamsMap(projectCode, taskNodes, new ArrayList<>(clusterInfos.values()),
                taskClusterParamsMap,
                taskNodeParamsMap);

        List<Map<String, String>> appendNodeParamsList = new ArrayList<>();
        List<Map<String, String>> appendClusterParamsList = new ArrayList<>();
        clusterInfos.values().stream()
                .map(cluster -> collectorClusterParam(taskClusterParamsMap.get(cluster.getId()), cluster))
                .forEach(appendClusterParamsList::add);
        taskNodes.stream()
                .map(node -> collectorNodeParam(nodeClusterInfos.get(node.getClusterCode()), taskNodeParamsMap, node))
                .forEach(appendNodeParamsList::add);
        //移除所有value为空的参数
        appendNodeParamsList.forEach(
            map -> map.entrySet().removeIf(entry -> entry.getValue() == null )
        );
        appendClusterParamsList.forEach(
            map -> map.entrySet().removeIf(entry -> entry.getValue() == null )
        );

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
            List<ProjectClusterParameter> taskClusterParams,
            ProjectCluster cluster) {
        Map<String, String> params = new HashMap<>();
        final String clusterId = cluster.getClusterId();
        // 注意顺序 集群自定义参数 最高优先级最后put
        // 1. 集群默认参数
        params.put("cluster_name", cluster.getClusterName());
        params.put("cluster_id", clusterId);
        params.put("appid", cluster.getAppId());
        // 2. Rest 接口参数
        Result<Map<String, Object>> result = platformRestService.getRest(RestParamEntry.newEntry()
                .build(cluster.getClusterId(), null, null)
                .buildRestParamEntiy(taskClusterParams.stream().map(clusterPamra -> {
                    ProjectParameter projectParameter = new ProjectParameter();
                    projectParameter.setParamName(clusterPamra.getParamName());
                    projectParameter.setParamValue(clusterPamra.getParamValue());
                    return projectParameter;
                }).collect(Collectors.toList())), PathEnum.CLUSTER_PARAMS);
        if (result.getData() != null) {
            params.putAll(result.getData().entrySet().stream()
                    .collect(Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        } else {
            log.warn("get cluster params error, clusterId: {}", clusterId);
        }

        // 3. 集群自定义参数
        params.putAll(taskClusterParams.stream()
                .collect(Collectors.toMap(ProjectClusterParameter::getParamName,
                        ProjectClusterParameter::getParamValue)));

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
        // 3. Rest 接口参数
        Result<Map<String, Object>> result = platformRestService.getRest(RestParamEntry.newEntry()
                .build(cluster.getClusterId(), String.valueOf(node.getNodeId()), null)
                .buildRestParamEntiy(taskNodeParamsMap.get(node.getId()).stream().map(nodePamra -> {
                    ProjectParameter projectParameter = new ProjectParameter();
                    projectParameter.setParamName(nodePamra.getParamName());
                    projectParameter.setParamValue(nodePamra.getParamValue());
                    return projectParameter;
                }).collect(Collectors.toList())), PathEnum.NODE_PARAMS);
        if (result.getData() != null) {
            params.putAll(result.getData().entrySet().stream().collect(
                    Collectors.toMap(Map.Entry::getKey, e -> e.getValue().toString())));
        } else {
            log.warn("get node params error, clusterId: {}, nodeId: {}", cluster.getClusterId(), node.getId());
        }

        // 4. 节点自定义参数
        params.putAll(taskNodeParamsMap.containsKey(node.getId())
                ? taskNodeParamsMap.get(node.getId()).stream()
                        .collect(Collectors.toMap(ProjectNodeParameter::getParamName,
                                ProjectNodeParameter::getParamValue))
                : new HashMap<>());
        // 5. id 必须为 node.id
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
