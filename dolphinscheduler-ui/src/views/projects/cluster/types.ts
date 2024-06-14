import { ProjectCluster, ProjectNodeParameter, StellarOpsClusterInfo } from "@/service/modules/project-platform/platform"

enum DataFromEnum {
    AUTO = 'auto',
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
    deleteLogic: (row: any) => void;
    editLogic: (row: any) => void;
}

export {
    OpsClusterInfo, DataFromEnum, DataTableRowOper
}