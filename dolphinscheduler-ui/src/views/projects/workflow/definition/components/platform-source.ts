import { queryProjectClusterList, queryProjectNodeList } from "@/service/modules/project-platform";
import {
  CascaderOption,
  NTag
} from "naive-ui";
import { h, ref } from "vue";


export const platformDef = () => {

  let projectSources: CascaderOption[];

  const loadingSource = ref(true);

  const generalProjectSources = (projectCode: number) => {
    if (loadingSource.value) {
      loadProjectSources(projectCode).then(res => {
        projectSources = res;
        loadingSource.value = false;
      }).catch(error => {
        console.error("Error loading project sources:", error);
        loadingSource.value = true;
      });
    }
    return projectSources;
  }

  async function loadProjectSources(projectCode: number): Promise<CascaderOption[]> {
    let cascaderOption: CascaderOption[] = [];

    const manuallyCluster = await queryProjectClusterList(projectCode);
    if (manuallyCluster) {
      const promises = manuallyCluster.map(
        async (cluster) => await queryProjectNodeList(projectCode, cluster.id)
      );

      // 等待所有的异步操作完成
      const results = await Promise.all(promises);
      results.forEach((nodes) => {
        const children: CascaderOption[] = nodes.map((node) => {
          let option: CascaderOption = (
            {
              label: node.nodeName,
              value: node.dataSourceCode,
              key: - node.id,
              disabled: node.dataSourceCode == undefined || node.dataSourceCode == null || node.dataSourceCode <= 0
            })
          return option;
        });

        const cluster = manuallyCluster.find((cluster) => cluster.id === nodes[0].clusterCode);
        const index = cascaderOption.findIndex((source) => source.value === cluster?.id);
        if (index === -1) {
          cascaderOption.push({ label: cluster?.clusterName, value: cluster?.id, children: children });
        } else {
          cascaderOption[index].children = children;
        }
      });
    }
    return cascaderOption;
  }


  return {
    generalProjectSources, loadingSource
  }
}

