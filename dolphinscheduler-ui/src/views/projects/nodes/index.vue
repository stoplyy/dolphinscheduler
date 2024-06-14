<template>
  <NSpace vertical>
    <Card>
      <NSpace justify='space-between'>
        <NSelect v-model:value="projectClusterCode" filterable placeholder="请选择集群" :options="selectOptions" />
        <n-dynamic-tags v-model:value="clusterTags" />
        <NButton type="primary" @click="refreshClusterData" class='btn-create-project'>刷新</NButton>
      </NSpace>
    </Card>
    <Card>
      <NDataTable :data="nodesList" :columns="columns" :loading="loadingRef" :width="tableWidth">
      </NDataTable>
    </Card>
  </NSpace>
</template>

<script lang="ts">
  import { defineComponent, ref, watch, computed } from 'vue';
  import { NButton, NTable, NDialog, NForm, NFormItem, NInput, NDialogProvider, NSpace, NDataTable, NPagination, NSelect, NDynamicTags } from 'naive-ui';
  import { getPlatformClusterList, getPlatformNodeList } from '@/service/modules/project-platform';
  import type { Router } from 'vue-router'
  import { useRouter } from 'vue-router'
  import { StellarOpsClusterInfo, StellarOpsNodeInfo } from '@/service/modules/project-platform/platform';
  import { useTable } from './use-tables';

  export default defineComponent({
    name: 'ProjectsNodes',
    components: {
      NButton,
      NDynamicTags,
      NTable,
      NDialog,
      NForm,
      NFormItem,
      NInput,
      NDialogProvider,
      NSelect,
      NSpace,
      NDataTable,
      NPagination
    },
    setup() {
      const router: Router = useRouter()
      const { columns } = useTable()

      const loadingRef = ref(false);
      const nodesList = ref<StellarOpsNodeInfo[]>();
      const projectClusterCode = ref<string | undefined>(undefined);
      const projectCode = ref(Number(router.currentRoute.value.params.projectCode));
      const projectName = ref(String(router.currentRoute.value.query.projectName));

      const clusterListData = ref<StellarOpsClusterInfo[]>();
      const clusterTags = ref<string[]>([]);
      const tableWidth = ref<number>(1600);

      const refreshClusterData = async () => {
        loadingRef.value = true;
        try {
          const res = await getPlatformClusterList(projectName.value);
          clusterListData.value = res;
        } catch (error) {
          console.error('Failed to refresh cluster data:', error);
        } finally {
          loadingRef.value = false;
        }
      };

      const clusterChanged = (val: string) => {
        loadingRef.value = true;
        getPlatformNodeList(projectName.value, val).then((res) => {
          nodesList.value = res;
          loadingRef.value = false;
        });

        const selectedCluster = clusterListData.value?.find((item) => item.clusterId === val);
        if (selectedCluster && selectedCluster.expMap) {
          clusterTags.value = Object.entries(Object.fromEntries(selectedCluster.expMap)).map(([key, value]) => `${key}:${value}`);
        }
        loadingRef.value = false;
      };

      watch(projectClusterCode, (val) => {
        if (val) {
          clusterChanged(val);
        }
      });

      const selectOptions = computed(() => {
        return clusterListData.value?.map(cluster => ({
          label: cluster.clusterName,
          value: cluster.clusterId
        })) || [];
      });

      // computed(() => );

      return {
        loadingRef,
        selectOptions,
        nodesList,
        projectClusterCode,
        tags: ref(['教师', '程序员']),
        projectCode,
        projectName,
        clusterListData,
        clusterTags,
        tableWidth,
        columns,
        refreshClusterData,
        clusterChanged
      }
    },
    mounted() {
      this.refreshClusterData();
    }
  });
</script>