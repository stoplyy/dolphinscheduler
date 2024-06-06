package org.apache.dolphinscheduler.api.platform.facade;

import java.util.List;
import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.tuhu.boot.common.facade.response.BizResponse;
import com.tuhu.stellarops.client.core.StellarOpsClusterInfo;
import com.tuhu.stellarops.client.core.StellarOpsNodeInfo;

import io.swagger.annotations.SwaggerDefinition;

/**
 * 示例
 */
@RequestMapping("/platform")
@SwaggerDefinition
public interface PlatformOpenApi {

    /**
     * 获取集群列表
     * 
     * @param platform
     * @return
     */
    @GetMapping("/{platform}/cluster/list")
    BizResponse<List<StellarOpsClusterInfo>> getPlatformClusterList(@PathVariable String platform);

    /**
     * 获取节点列表
     * 
     * @param platform
     * @param clusterId
     * @param taskName
     * @return
     */
    @GetMapping("/{platform}/node/list/{clusterId}")
    BizResponse<List<StellarOpsNodeInfo>> getPlatformNodeList(@PathVariable String platform,
            @RequestParam String clusterId,
            @RequestParam(required = false) String taskName);

    /**
     * 获取远端rest接口
     * 地址与枚举类PathEnum对应 例如：/platform/hubble/cluster/list =>
     * PathEnum.HUBBLE_CLUSTER_LIST
     * 
     * @param rest
     * @param platform
     * @return
     */
    @GetMapping("/{platform}/rest/{rest}")
    BizResponse<Map<String, Object>> getPlatformRest(@PathVariable String rest, @PathVariable String platform,
            @RequestParam(required = false) String clusterId, @RequestParam(required = false) String nodeId,
            @RequestParam(required = false) String taskType);

}
