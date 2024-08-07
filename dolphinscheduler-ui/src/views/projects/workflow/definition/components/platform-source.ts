import { CascaderOption } from "naive-ui";
import { queryProjectClusterList, queryProjectNodeList } from "@/service/modules/project-platform";
import { ProjectNode } from "@/service/modules/project-platform/platform";


export const platformDef = () => {

  let projectSources: CascaderOption[];

  const generalProjectSources = async () => {
    if (projectSources == null) {
      projectSources = await loadProjectSources(0);
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
          return { label: node.nodeName, value: node.id };
        });

        const cluster = manuallyCluster.find((cluster) => cluster.id === nodes[0].clusterCode);
        const index = cascaderOption.findIndex((source) => source.value === cluster?.id);
        if (index === -1) {
          cascaderOption[index].children = children;
        } else {
          cascaderOption.push({ label: cluster?.clusterName, value: cluster?.id, children: children });
        }
      });
    }
    return cascaderOption;
  }
}

