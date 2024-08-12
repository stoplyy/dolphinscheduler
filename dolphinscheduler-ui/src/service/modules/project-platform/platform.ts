import { DataFromEnum } from "@/views/projects/cluster/types"

interface StellarOpsClusterInfo {
  clusterId?: string
  clusterName?: string
  expMap?: Map<string, Object>
}

interface StellarOpsNodeInfo {
  nodeId?: string
  nodeKey?: string
  nodeName?: string
  expMap?: Map<string, Object>
}

interface BizResponse {
  code: number;
  message: string;
  data: any;
}

interface ProjectCluster {
  id: number;
  projectCode: number;
  clusterId: string;
  clusterName: string;
  appId: string;
  userId: number;
  userName: string;
  description: string;
  createTime: Date;
  updateTime: Date;
}

interface CommonParameter {
  id: number;
  paramName: string;
  paramValue: string;
  description: string;
  createTime: Date;
  updateTime: Date;
  from: DataFromEnum | DataFromEnum.MANUAL;
}

interface ProjectClusterParameter extends CommonParameter {
  userId: number;
  projectCode: number;
  clusterId: string;
  clusterName: string;
  clusterCode: number;
}

interface ProjectNodeParameter extends CommonParameter {
  userId: number;
  clusterId: string;
  nodeId: string;
  projectCode: number;
  clusterCode: number;
  nodeCode: number;
}

interface ProjectNode {
  id: number;
  userId: number;
  userName: string;
  projectCode: number;
  clusterId: string;
  clusterCode: number;
  dataSourceCode: number;
  nodeId: string;
  nodeKey: string;
  nodeName: string;
  description: string;
  createTime: Date;
  updateTime: Date;
}

enum PlatformRestEnum {
  CLUSTER_LIST = "cluster.list.rest",
  CLUSTER_PARAMS = "cluster.params.rest",
  NODE_LIST = "node.list.rest",
  NODE_PARAMS = "node.params.rest",
  TASK_PARAMS = "task.params.rest",
  ENV_CHECK = "env.check.rest"
}

enum PlatformConst {
  P_DATASOURCE_PARAM_NAME = "platform.datasource",
  P_NODE_PARAM_NAME = "platform.node",
  P_CLUSTER_PARAM_NAME = "platform.cluster"
}

export {
  PlatformConst,
  PlatformRestEnum, BizResponse, StellarOpsClusterInfo,
  StellarOpsNodeInfo, ProjectCluster, ProjectNodeParameter, ProjectNode, ProjectClusterParameter, CommonParameter
}