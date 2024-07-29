import { ProjectCluster, ProjectNodeParameter, StellarOpsClusterInfo } from "@/service/modules/project-platform/platform"

enum DataFromEnum {
  AUTO = 'Auto',
  HALLEY = 'Halley',
  MANUAL = 'manual'
}

interface OpsClusterInfo extends ProjectCluster, StellarOpsClusterInfo {

  paramList?: ProjectNodeParameter[]

  from: DataFromEnum,

  clusterId: string; // Rename the clusterId property to match the other interface
  clusterName: string; // Rename the clusterId property to match the other interface

  stellaropsClusterName: string;
}

interface DataTableRowOper {
  syncLogic(row: any): void;
  deleteLogic: (row: any) => void;
  editLogic: (row: any) => void;
}

export {
  OpsClusterInfo, DataFromEnum, DataTableRowOper
}