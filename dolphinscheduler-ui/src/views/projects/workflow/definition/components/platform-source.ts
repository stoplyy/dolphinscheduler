import { queryProjectClusterList, queryProjectNodeList } from "@/service/modules/project-platform";
import { ProjectCluster, ProjectNode } from "@/service/modules/project-platform/platform";
import {
  CascaderOption
} from "naive-ui";
import { ref } from "vue";


export const platformDef = () => {

  let projectSources: CascaderOption[];
  let clusterOptions: CascaderOption[];
  let nodeOptions: CascaderOption[];
  const loadingSource = ref(true);

  const generalProjectSources = (projectCode: number) => {
    if (loadingSource.value) {
      loadProjectSources(projectCode).then(res => {
        let { manuallyCluster, nodesAll } = res;

        intiOptions(nodesAll, manuallyCluster);
        loadingSource.value = false;
      }).catch(error => {
        console.error("Error loading project sources:", error);
        loadingSource.value = true;
      });
    }
    return { projectSources, clusterOptions, nodeOptions };
  }

  const intiOptions = (nodesAll: ProjectNode[][], manuallyCluster: ProjectCluster[]) => {
    projectSources = ([]);
    clusterOptions = ([])
    nodeOptions = ([])

    nodesAll.forEach((clusterNodes) => {
      const cluster = manuallyCluster.find((cluster) => cluster.id === clusterNodes[0].clusterCode);
      clusterOptions.push({ label: cluster?.clusterName, value: cluster?.id, key: cluster?.id });

      const index = projectSources.findIndex((source) => source.value === cluster?.id);
      const sourceChildren: CascaderOption[] = clusterNodes.map((node) => {
        let option: CascaderOption = (
          {
            label: node.nodeName,
            value: node.dataSourceCode,
            key: - node.id,
            disabled: node.dataSourceCode == undefined || node.dataSourceCode == null || node.dataSourceCode <= 0
          })
        return option;
      });
      if (index === -1) {
        projectSources.push({ label: cluster?.clusterName, value: cluster?.id, children: sourceChildren });
      } else {
        projectSources[index].children = sourceChildren;
      }

      const nodeChildren: CascaderOption[] = clusterNodes.map((node) => {
        let option: CascaderOption = (
          {
            label: node.nodeName,
            value: node.id,
            key: node
          })
        return option;
      });
      const nodeIndex = nodeOptions.findIndex((source) => source.value === cluster?.id);
      if (nodeIndex === -1) {
        nodeOptions.push({ label: cluster?.clusterName, value: cluster?.id, children: nodeChildren });
      } else {
        nodeOptions[nodeIndex].children = nodeChildren;
      }

    });
  }

  async function loadProjectSources(projectCode: number): Promise<any> {
    const manuallyCluster = await queryProjectClusterList(projectCode);

    if (manuallyCluster) {
      const promises = manuallyCluster.map(
        async (cluster) => await queryProjectNodeList(projectCode, cluster.id)
      );

      // 等待所有的异步操作完成
      const nodesAll = await Promise.all(promises);
      return { manuallyCluster, nodesAll };
    }
    return { manuallyCluster: [], nodesAll: [] };
  }


  return {
    generalProjectSources, loadingSource
  }
}

