
import { axios } from '@/service/service'
import { PlatformRestEnum, ProjectCluster, ProjectClusterParameter, ProjectNode, ProjectNodeParameter, StellarOpsClusterInfo, StellarOpsNodeInfo } from './platform'
import { DataFromEnum } from '@/views/projects/cluster/types';


//查询平台公钥
export async function getPlatformPublicKey(projectCode: number): Promise<string> {
  const data = await axios({
    url: `/platform/${projectCode}/publicKey`,
    method: 'get'
  });
  return data as unknown as string;
}

// 查询项目群集列表
export async function getPlatformClusterListByProject(projectCode: number): Promise<StellarOpsClusterInfo[]> {
  const data = await axios({
    url: `/platform/${projectCode}/clusterlist`,
    method: 'get'
  });
  return data as unknown as StellarOpsClusterInfo[];
}

export async function getPlatformClusterList1(platform: String): Promise<StellarOpsClusterInfo[]> {
  const response = await axios({
    url: `/platform/${platform}/cluster/list`,
    method: 'get'
  })
  return response as unknown as StellarOpsClusterInfo[];
}

export async function getPlatformNodeListByProject(projectCode: number, clusterId: String, taskName?: String): Promise<StellarOpsNodeInfo[]> {
  const response = await axios({
    url: `/platform/${projectCode}/nodelist/${clusterId}`,
    method: 'get',
    params: {
      taskName
    }
  })
  return response as unknown as StellarOpsNodeInfo[];
}

export async function getPlatformNodeList1(platform: String, clusterId: String, taskName?: String): Promise<StellarOpsNodeInfo[]> {
  const response = await axios({
    url: `/platform/${platform}/node/list/${clusterId}`,
    method: 'get',
    params: {
      taskName
    }
  })
  return response as unknown as StellarOpsNodeInfo[];
}

export async function getPlatformRestByProject(projectCode: number, rest: PlatformRestEnum, clusterId?: String, nodeId?: String, taskType?: String): Promise<Map<string, Object>> {
  const response = await axios({
    url: `/platform/${projectCode}/rest`,
    method: 'get',
    params: {
      rest,
      clusterId,
      nodeId,
      taskType
    }
  })
  return new Map(Object.entries(response == null ? {} : response));
}


export async function getPlatformRest1(platform: String, rest: PlatformRestEnum, clusterId?: String, nodeId?: String, taskType?: String): Promise<Map<string, Object>> {
  const response = await axios({
    url: `/platform/${platform}/rest/${rest}`,
    method: 'get',
    params: {
      clusterId,
      nodeId,
      taskType
    }
  })
  return new Map(Object.entries(response == null ? {} : response));
}

// 创建项目群集
export async function createProjectCluster(from: DataFromEnum, projectCode: number, clusterName: string, clusterId: string, description?: string): Promise<ProjectCluster> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster`,
    method: 'post',
    data: {
      from,
      clusterName,
      clusterId,
      description
    }
  });
  return data as unknown as ProjectCluster;
}

// 更新项目群集
export async function updateProjectCluster(projectCode: number, clusterInfo?: ProjectCluster): Promise<ProjectCluster> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterInfo?.id}`,
    method: 'put',
    data: clusterInfo
  });
  return data as unknown as ProjectCluster;
}

// 查询项目群集列表
export async function queryProjectClusterList(projectCode: number): Promise<ProjectCluster[]> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/querylist`,
    method: 'get'
  });
  return data as unknown as ProjectCluster[];
}


// 删除项目群集
export async function deleteProjectCluster(projectCode: number, clusterCode: number): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterCode}`,
    method: 'delete'
  });
  return data as unknown as boolean;
}

// 创建项目群集参数
export async function createClusterParameter(projectCode: number, clusterCode: number, paramName: string, paramValue: string): Promise<ProjectClusterParameter> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterCode}/parameters`,
    method: 'post',
    data: {
      paramName,
      paramValue
    }
  });
  return data as unknown as ProjectClusterParameter;
}

// 更新项目群集参数
export async function updateClusterParameter(projectCode: number, param: ProjectClusterParameter): Promise<ProjectClusterParameter> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${param.clusterCode}/parameters/${param.id}`,
    method: 'put',
    data: param
  });
  return data as unknown as ProjectClusterParameter;
}

// 删除项目群集参数
export async function deleteClusterParameter(projectCode: number, clusterCode: number, code: number): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterCode}/parameters/${code}`,
    method: 'delete'
  });
  return data as unknown as boolean;
}

// 批量删除项目群集参数
export async function batchDeleteClusterParameters(projectCode: number, clusterCode: number, codes: string): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterCode}/batch/parameters`,
    method: 'delete',
    data: {
      codes
    }
  });
  return data as unknown as boolean;
}

// 查询项目群集参数列表
export async function queryClusterParametersList(projectCode: number, clusterCode: number): Promise<ProjectClusterParameter[]> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/${clusterCode}/parameters`,
    method: 'get'
  });
  return data as unknown as ProjectClusterParameter[];
}

// 根据代码查询项目群集参数
export async function queryClusterParameterByCode(projectCode: number, parameterCode: number): Promise<ProjectClusterParameter> {
  const data = await axios({
    url: `/projects/${projectCode}/project-cluster/parameters/${parameterCode}`,
    method: 'get'
  });
  return data as unknown as ProjectClusterParameter;
}

export async function syncNodesByHalley(projectCode: number, clusterCode: number): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/all/sync/halley`,
    method: 'post'
  });
  return data as unknown as boolean;
}

export async function syncAllNodeData(projectCode: number, clusterCode: number): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/all/sync`,
    method: 'post'
  });
  return data as unknown as boolean;
}

// 创建项目节点
export async function createProjectNode(from: DataFromEnum, projectCode: number, clusterCode: number, nodeKey: string, nodeName: string, nodeId: string, description?: string): Promise<ProjectNode> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}`,
    method: 'post',
    data: {
      from,
      nodeKey,
      nodeName,
      nodeId,
      description
    }
  });
  return response as unknown as ProjectNode;
}

// 更新项目节点
export async function updateProjectNode(projectCode: number, clusterCode: number, code: number, nodeKey: string, nodeName: string, nodeId: string, description?: string): Promise<ProjectNode> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${code}`,
    method: 'put',
    data: {
      nodeKey,
      nodeName,
      nodeId,
      description
    }
  });
  return response as unknown as ProjectNode;
}

// 删除项目节点
export async function deleteProjectNode(projectCode: number, clusterCode: number, nodeCode: number): Promise<boolean> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}`,
    method: 'delete'
  });
  return response as unknown as boolean;
}

// 查询项目节点列表
export async function queryProjectNodeList(projectCode: number, clusterCode: number): Promise<ProjectNode[]> {
  if (!clusterCode || clusterCode == null) {
    return [];
  }
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/querylist`,
    method: 'get'
  });
  return response as unknown as ProjectNode[];
}

// 创建项目节点参数
export async function createProjectNodeParameter(projectCode: number, clusterCode: number, nodeCode: number, paramName: string, paramValue: string): Promise<ProjectNodeParameter> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/parameters`,
    method: 'post',
    data: {
      paramName,
      paramValue
    }
  });
  return response as unknown as ProjectNodeParameter;
}

// 更新项目节点参数
export async function updateProjectNodeParameter(projectCode: number, clusterCode: number, nodeCode: number, code: number, paramName: string, paramValue: string, description?: string): Promise<ProjectNodeParameter> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/parameters/${code}`,
    method: 'put',
    data: {
      paramName,
      paramValue,
      description
    }
  });
  return response as unknown as ProjectNodeParameter;
}

// 删除项目节点参数
export async function deleteProjectNodeParameter(projectCode: number, clusterCode: number, nodeCode: number, code: number): Promise<boolean> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/parameters/${code}`,
    method: 'delete'
  });
  return response as unknown as boolean;
}

// 批量删除项目节点参数
export async function batchDeleteProjectNodeParameters(projectCode: number, clusterCode: number, nodeCode: number, codes: string): Promise<boolean> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/batch/parameters`,
    method: 'delete',
    data: {
      codes
    }
  });
  return response as unknown as boolean;
}

// 查询项目节点参数列表
export async function queryProjectNodeParametersList(projectCode: number, clusterCode: number, nodeCode: number): Promise<ProjectNodeParameter[]> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/parameters`,
    method: 'get'
  });
  return response as unknown as ProjectNodeParameter[];
}

export async function testConnectivityByHalley(projectCode: number, clusterCode: number): Promise<Map<String, Boolean>> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/all/test/connect`,
    method: 'post'
  });
  return response as unknown as Map<String, Boolean>;
}

//createSourceWithAllNode
export async function createSourceWithAllNode(projectCode: number, clusterCode: number): Promise<boolean> {
  const response = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/all/create/source`,
    method: 'post'
  });
  return response as unknown as boolean;
}

export async function syncSourceNodeData(projectCode: number, clusterCode: number, nodeCode: number): Promise<boolean> {
  const data = await axios({
    url: `/projects/${projectCode}/project-node/${clusterCode}/${nodeCode}/create/source`,
    method: 'post'
  });
  return data as unknown as boolean;
}
