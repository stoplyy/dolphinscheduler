package org.apache.dolphinscheduler.api.platform.facade;

import java.util.List;
import java.util.Map;

import com.tuhu.boot.common.facade.response.BizResponse;
import com.tuhu.stellarops.client.core.StellarOpsClusterInfo;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;
import com.tuhu.stellarops.client.spring.endpoint.StellarOpsOpenApiEndpoint;

import feign.Param;
import feign.RequestLine;

public interface StellarOpsOpenApiEndpointFeign extends StellarOpsOpenApiEndpoint {

    /**
     * 获取集群列表
     *
     * @return 集群列表
     */
    @RequestLine("GET /stellarops/cluster/list")
    @Override
    BizResponse<List<StellarOpsClusterInfo>> getClusterList();

    /**
     * 获取集群通用参数
     *
     * @param clusterId 集群id
     * @param taskName  任务名称
     * @return 集群通用参数
     */
    @RequestLine("GET /stellarops/cluster/params/{clusterId}?taskName={taskName}")
    @Override
    BizResponse<Map<String, Object>> getClusterCommParam(
            @Param("clusterId") String clusterId,
            @Param("taskName") String taskName);

    /**
     * 获取集群节点列表
     *
     * @param clusterId 集群id
     * @param taskName  任务名称
     * @return 集群节点列表
     */
    @RequestLine("GET /stellarops/node/list/{clusterId}?taskName={taskName}")
    @Override
    BizResponse<List<StellarOpsNodeInfo>> getNodeList(
            @Param("clusterId") String clusterId,
            @Param("taskName") String taskName);

    /**
     * 获取集群节点通用参数
     *
     * @param clusterId 集群id
     * @param nodeId    节点id
     * @param taskName  任务名称
     * @return 集群节点通用参数
     */
    @RequestLine("GET /stellarops/node/params/{clusterId}/{nodeId}?taskName={taskName}")
    @Override
    BizResponse<Map<String, Object>> getNodeCommParam(
            @Param("clusterId") String clusterId, @Param("nodeId") String nodeId,
            @Param("taskName") String taskName);

    /**
     * 获取任务通用参数
     *
     * @param clusterId 集群id
     * @param taskName  任务名称
     * @return 任务通用参数
     */
    @RequestLine("GET /stellarops/task/params/{clusterId}?taskName={taskName}")
    @Override
    BizResponse<Map<String, Object>> getTaskCommParam(
            @Param("clusterId") String clusterId,
            @Param("taskName") String taskName);

    /**
     * 检查环境状态
     *
     * @return 环境状态检查结果
     */
    @RequestLine("GET /stellarops/env/check")
    @Override
    BizResponse<Map<String, Object>> checkEnv();
}
