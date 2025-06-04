
import {
    BizResponse,
    StellarOpsClusterInfo,
    StellarOpsNodeInfo

} from './platform'

interface ClusterListResponse extends BizResponse {
    data: StellarOpsClusterInfo[];
}

interface NodeListResponse extends BizResponse {
    data: StellarOpsNodeInfo[];
}

interface RestParamsResponse extends BizResponse {
    data: Map<string, Object>;
}

interface clusterCreateRequest {
    projectCode: number;
    clusterName: String;
    clusterId: String;
    description: String;
}

export {
    ClusterListResponse, NodeListResponse, RestParamsResponse, clusterCreateRequest
}