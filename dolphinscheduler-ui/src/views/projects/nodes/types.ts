import { ProjectNode, ProjectNodeParameter, StellarOpsNodeInfo } from "@/service/modules/project-platform/platform"
import { DataFromEnum } from "../cluster/types";


interface OpsNodeInfo extends ProjectNode, StellarOpsNodeInfo {
  nodeKey: string; // Rename the nodeId property to match the other interface
  nodeId: string; // Rename the nodeId property to match the other interface
  nodeName: string; // Rename the nodeName property to match the other interface

  stellaropsClusterName: string;
  from: DataFromEnum;
  paramList?: ProjectNodeParameter[]

  isConnected?: boolean;
}

interface DataTableRowOper {
  syncLogic(row: any): void;
  deleteLogic: (row: any) => void;
  editLogic: (row: any) => void;
}

export {
  OpsNodeInfo, DataTableRowOper
}