<template>
  <NSpace vertical>
    <NCard>
      <NSpace justify='space-between'>
        <NSpace>
          <NSelect v-model:value="projectClusterCode" filterable placeholder="请选择集群" :options="selectOptions"
            :render-option="renderOption">
            <template #action>
              <NButton type="primary" size="small" @click="refreshClusterData" class='btn-create-project'>刷新集群</NButton>
            </template>
          </NSelect>
          <NButton type="primary" @click="nodesRefresh()" :disabled="loadingRef || !hasSelectCluster()"
            class='btn-create-project'>刷新节点</NButton>
        </NSpace>
        <NSpace>
          <NButton type="primary" @click="addNodeLogic" :disabled="!hasSelectCluster()" class='btn-create-project'>添加节点
          </NButton>
          <NPopconfirm @positive-click="syncAllNode">
            <template #trigger>
              <NTooltip>
                <template #trigger>
                  <NButton type="primary" class='btn-create-project'
                    :disabled="loadingRef || !hasSelectCluster() || hasAnyNoSyncNode()">一键同步
                  </NButton>
                </template>
                <span>完全同步节点列表接口拉取的节点，会清理本地已落库但未在节点列表中的数据！</span>
              </NTooltip>
            </template>
            是否同步所有节点？
          </NPopconfirm>
        </NSpace>
      </NSpace>
    </NCard>
    <NCard>
      <NDataTable :data="nodesList" :columns="nodeColumns" :loading="loadingRef" :width="tableWidth">
      </NDataTable>
    </NCard>
    <NModal v-model:show="showParamModalRef" :show-icon="false" class="custom-card" preset="dialog"
      :title="modalParamMode" @positive-click="confirmParamModal" @negative-click="cancelParamModal" positive-text="确认"
      negative-text="取消">
      <NForm :model="modalParamInfo" ref="formParamRef_node" label-placement="left" label-width="auto"
        require-mark-placement="right-hanging">
        <NFormItem label="参数名" path="paramName">
          <NInput v-model:value="modalParamInfo.paramName" :disabled="modalParamMode === '查看'" />
        </NFormItem>
        <NFormItem label="参数默认值" path="paramValue">
          <NInput v-model:value="modalParamInfo.paramValue" :disabled="modalParamMode === '查看'" />
        </NFormItem>
        <NFormItem label="描述" path="description" v-show="modalParamMode !== '创建'">
          <NInput type="textarea" v-model:value="modalParamInfo.description" :disabled="modalParamMode === '查看'" />
        </NFormItem>
      </NForm>
    </NModal>
    <NModal v-model:show="showModalRef" :show-icon="false" class="custom-card" style="width: 50%" preset="dialog"
      :title="modalMode" @positive-click="confirmModal" @negative-click="cancelModal" positive-text="确认"
      negative-text="算了">
      <NCard>
        <NForm :model="modalNodeInfo" ref="formRef_node" label-placement="left" label-width="auto"
          require-mark-placement="right-hanging">
          <NFormItem label="集群Id" path="clusterId">
            <NInput v-model:value="modalNodeInfo.clusterId" disabled />
          </NFormItem>
          <NFormItem label="集群名称" path="stellaropsClusterName">
            <NInput v-model:value="modalNodeInfo.stellaropsClusterName" disabled />
          </NFormItem>
          <NFormItem label="nodeId" path="nodeId" required>
            <NInput v-model:value="modalNodeInfo.nodeId" :allowInput="trim"
              :disabled="modalMode === '查看' || modalNodeInfo.from === DataFromEnum.AUTO" />
          </NFormItem>
          <NFormItem label="nodeKey" path="nodeKey" required>
            <NInput v-model:value="modalNodeInfo.nodeKey" :allowInput="trim"
              :disabled="modalMode === '查看' || modalNodeInfo.from === DataFromEnum.AUTO" />
          </NFormItem>
          <NFormItem label="nodeName" path="nodeName" required>
            <NInput v-model:value="modalNodeInfo.nodeName" :allowInput="trim" :disabled="modalMode === '查看'" />
          </NFormItem>
          <NFormItem label="描述" path="description" required>
            <NInput type="textarea" :allowInput="trim" v-model:value="modalNodeInfo.description"
              :disabled="modalMode === '查看'" />
          </NFormItem>
        </NForm>
      </NCard>
      <NCard v-show="modalMode !== '创建'">
        <NSpace justify='space-between'>
          <NButton size="small" type="primary" @click="addParamLogic" class='btn-create-project'>添加参数</NButton>
          <NButton size="small" type="info" @click="refreshParamList" :disabled="loadingParamRef"
            class='btn-create-project'>刷新</NButton>
        </NSpace>
        <NDataTable :data="nodeParmas" :columns="paramsColumns" :loading="loadingParamRef"
          :pagination="{ pageSize: 10 }">
        </NDataTable>
      </NCard>
    </NModal>
  </NSpace>
</template>

<script lang="ts">
  import { createProjectNode, createProjectNodeParameter, deleteProjectNode, deleteProjectNodeParameter, getPlatformNodeList, getPlatformRest, queryProjectNodeList, queryProjectNodeParametersList, updateProjectNode, updateProjectNodeParameter } from '@/service/modules/project-platform';
  import { PlatformRestEnum, ProjectNodeParameter } from '@/service/modules/project-platform/platform';
  import { NButton, NCard, NDataTable, NDialog, NDialogProvider, NPopconfirm, NDynamicTags, NForm, NFormItem, NFormItemGi, NFormItemRow, NGrid, NGridItem, NInput, NModal, NPagination, NSelect, NSpace, NTable, NTooltip, SelectOption } from 'naive-ui';
  import { computed, defineComponent, getCurrentInstance, h, reactive, ref, VNode, watch } from 'vue';
  import type { Router } from 'vue-router';
  import { useRouter } from 'vue-router';
  import { getClusterData } from '../cluster/cluster-tables';
  import { useParamsTable } from '../cluster/params-tables';
  import { DataFromEnum, OpsClusterInfo } from '../cluster/types';
  import { OpsNodeInfo } from './types';
  import { useNodeTable } from './use-tables';

  export default defineComponent({
    name: 'ProjectsNodes',
    components: {
      NButton,
      NTooltip,
      NPopconfirm,
      NFormItemGi,
      NFormItemRow,
      NDynamicTags,
      NTable,
      NGrid,
      NGridItem,
      NDialog,
      NModal,
      NCard,
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
      const trim = getCurrentInstance()?.appContext.config.globalProperties.trim
      const clusterListData = ref<OpsClusterInfo[]>();
      const clusterTags = ref<string[]>([]);
      const tableWidth = ref<number>(1600);

      const loadingRef = ref(false);
      const nodesList = ref<OpsNodeInfo[]>([] as OpsNodeInfo[]);
      const projectClusterCode = ref<string>('');
      const projectCode = ref(Number(router.currentRoute.value.params.projectCode));
      const projectName = ref(String(router.currentRoute.value.query.projectName));

      const selectedCluster = ref<OpsClusterInfo>({} as OpsClusterInfo);

      const showModalRef = ref(false);
      const currentRow = ref<OpsNodeInfo>({} as OpsNodeInfo);
      const modalNodeInfo = reactive<OpsNodeInfo>({} as OpsNodeInfo);
      const modalMode = ref<string>('创建'); // 创建、修改、查看、同步
      const modalParamMode = ref<string>('创建'); // 创建、修改、查看

      const showParamModalRef = ref(false);
      const loadingParamRef = ref(false);
      const nodeParams = ref<ProjectNodeParameter[]>([] as ProjectNodeParameter[]);
      const currentRowParam = ref<ProjectNodeParameter>({} as ProjectNodeParameter);
      const modalParamInfo = ref<ProjectNodeParameter>({} as ProjectNodeParameter);

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

      const renderOption = ({ node, option }: { node: VNode; option: SelectOption; }) => h(NTooltip, null, {
        trigger: () => node,
        default: () => clusterListData?.value?.find((item) => item.clusterId === option.value)?.description || ''
      });

      const refreshClusterData = async () => {
        loadingRef.value = true;
        try {
          clusterListData.value = await getClusterData(projectName.value, projectCode.value);
        } catch (error) {
          console.error('Failed to refresh cluster data:', error);
        } finally {
          loadingRef.value = false;
        }
      };

      const hasSelectCluster = () => {
        return selectedCluster.value && selectedCluster.value.id > 0;
      }

      const hasAnyNoSyncNode = () => {
        return nodesList.value.filter((node) => !node.id || node.id <= 0).length === 0;
      }

      const nodesRefresh = async () => {
        await clusterChanged(projectClusterCode.value);
      }

      const clusterChanged = async (val: string) => {
        loadingRef.value = true;

        selectedCluster.value = clusterListData.value?.find((item) => item.clusterId === val) || {} as OpsClusterInfo;

        const res = await getPlatformNodeList(projectName.value, val);
        const nodeListDataTmp = res.map((item) => {
          return {
            ...item,
            from: DataFromEnum.AUTO,
            stellaropsClusterName: selectedCluster.value.clusterName,
          } as OpsNodeInfo;
        });

        const manuallyNodes = await queryProjectNodeList(projectCode.value, selectedCluster.value.id);
        manuallyNodes.map((item) => {
          if (!nodeListDataTmp.find((node) => node.nodeId === item.nodeId)) {
            nodeListDataTmp.push({
              ...item,
              from: DataFromEnum.MANUAL,
              stellaropsClusterName: selectedCluster.value.clusterName,
            } as OpsNodeInfo);
          } else {
            const nodeIndex = nodeListDataTmp.findIndex((node) => node.nodeId === item.nodeId);
            const node = nodeListDataTmp[nodeIndex];
            nodeListDataTmp[nodeIndex] = {
              ...item,
              from: node.from,
              stellaropsClusterName: node.stellaropsClusterName,
              expMap: node.expMap,
            } as OpsNodeInfo;
          }
        });

        if (selectedCluster.value.expMap) {
          clusterTags.value = Object.entries(Object.fromEntries(selectedCluster.value.expMap)).map(([key, value]) => `${key}:${value}`);
        }
        nodesList.value = nodeListDataTmp;

        loadingRef.value = false;
      };

      const nodeColumns = useNodeTable({
        deleteLogic: async (row: OpsNodeInfo) => {
          await deleteProjectNode(projectCode.value, row.clusterCode, row.id).then(() => {
            console.log('删除成功');
            if (row.from === DataFromEnum.AUTO) {
              nodesRefresh();
            } else {
              nodesList.value.splice(nodesList.value.findIndex((item) => item.id === row.id), 1);
            }
          });
        },
        editLogic: async (row: OpsNodeInfo) => {
          if (modalNodeInfo.id != row.id) {
            currentRow.value = row;
            currentRow.value.stellaropsClusterName = selectedCluster.value.clusterName;
            Object.assign(modalNodeInfo, currentRow.value);
            await refreshParamList();
          }
          modalMode.value = '修改';
          showModalRef.value = true;
        },
        syncLogic: async (row) => {
          await createProjectNode(
            projectCode.value,
            selectedCluster.value.id,
            row.nodeKey,
            row.nodeName,
            row.nodeId,
            '节点同步').then(() => {
              console.log('同步成功');
              nodesRefresh();
            });
        }
      })

      const syncAllNode = async () => {
        loadingRef.value = true;

        nodesList.value.forEach(async (node) => {
          if (!(node.id && node.id > 0) as boolean) {
            await createProjectNode(
              projectCode.value,
              selectedCluster.value.id,
              node.nodeKey,
              node.nodeName,
              node.nodeId,
              '节点同步').then(() => {
                console.log(node.nodeName + '同步成功');
              });
          }
        }
        )
        await nodesRefresh();

        loadingRef.value = false;
      };

      async function refreshParamList() {
        loadingParamRef.value = true;
        if (modalNodeInfo.id && modalNodeInfo.id > 0) {
          // 人工创建的参数
          await queryProjectNodeParametersList(projectCode.value, modalNodeInfo.clusterCode, modalNodeInfo.id).then((res) => {
            nodeParams.value = res;
          });
          await getPlatformRest(projectName.value, PlatformRestEnum.NODE_PARAMS, modalNodeInfo.clusterId, modalNodeInfo.nodeId, '').then((res) => {
            res.forEach((val, key) => {
              nodeParams.value.push({
                paramName: key,
                paramValue: val,
                from: DataFromEnum.AUTO,
              } as ProjectNodeParameter);
            });
          });
        } else {
          nodeParams.value = [];
        }
        loadingParamRef.value = false;
      }

      const addNodeLogic = () => {
        currentRow.value = {
          id: -1,
          clusterId: selectedCluster.value.clusterId,
          projectCode: projectCode.value,
          clusterCode: selectedCluster.value.id,
          from: DataFromEnum.MANUAL,
          paramList: [],
          stellaropsClusterName: selectedCluster.value.clusterName,
        } as unknown as OpsNodeInfo;
        Object.assign(modalNodeInfo, currentRow.value);
        modalMode.value = '创建';
        showModalRef.value = true;
        console.log('node create init info:', modalNodeInfo);
      }

      const cancelModal = () => {
        console.log('cancelModal');
        showModalRef.value = false;
      }

      const confirmModal = async () => {
        if (modalMode.value === '创建') {
          await createProjectNode(
            modalNodeInfo.projectCode,
            modalNodeInfo.clusterCode,
            modalNodeInfo.nodeKey,
            modalNodeInfo.nodeName,
            modalNodeInfo.nodeId,
            modalNodeInfo.description).then(() => {
              console.log('创建成功');
              showModalRef.value = false;
              refreshClusterData();
            });
        } else if (modalMode.value === '修改') {
          if (!modalNodeInfo.id || modalNodeInfo.id <= 0) {
            await createProjectNode(
              modalNodeInfo.projectCode,
              modalNodeInfo.clusterCode,
              modalNodeInfo.nodeKey,
              modalNodeInfo.nodeName,
              modalNodeInfo.nodeId,
              modalNodeInfo.description).then(() => {
                console.log('修改成功');
                nodesRefresh();
                showModalRef.value = false;
              });
          } else {
            await updateProjectNode(
              modalNodeInfo.projectCode,
              modalNodeInfo.clusterCode,
              modalNodeInfo.id,
              modalNodeInfo.nodeKey,
              modalNodeInfo.nodeName,
              modalNodeInfo.nodeId,
              modalNodeInfo.description).then(() => {
                console.log('修改成功');
                showModalRef.value = false;
                Object.assign(currentRow, modalNodeInfo);
              });
          }
        }
      }

      const paramsColumns = useParamsTable({
        deleteLogic: async (row: ProjectNodeParameter) => {
          await deleteProjectNodeParameter(projectCode.value, row.clusterCode, row.nodeCode, row.id).then(() => {
            console.log('删除成功');
            nodeParams.value.splice(nodeParams.value.findIndex((item) => item.id === row.id), 1);
          });
        },
        editLogic: async (row: ProjectNodeParameter) => {
          currentRowParam.value = row;
          Object.assign(modalParamInfo.value, row);
          modalParamMode.value = '修改';
          showParamModalRef.value = true;
        },
        syncLogic: (row: ProjectNodeParameter) => {
        }
      })

      const addParamLogic = () => {
        modalParamInfo.value = { nodeCode: modalNodeInfo.id } as ProjectNodeParameter;
        modalParamMode.value = '创建';
        showParamModalRef.value = true;
      }

      const cancelParamModal = () => {
        console.log('cancelParamModal');
        showParamModalRef.value = false;
      }

      const confirmParamModal = async () => {
        if (modalParamMode.value === '创建') {
          await createProjectNodeParameter(
            projectCode.value,
            modalNodeInfo.clusterCode,
            modalParamInfo.value.nodeCode,
            modalParamInfo.value.paramName,
            modalParamInfo.value.paramValue).then(() => {
              console.log('创建成功');
              showParamModalRef.value = false;
              refreshParamList();
            });
        } else {
          await updateProjectNodeParameter(
            projectCode.value,
            modalNodeInfo.clusterCode,
            modalParamInfo.value.nodeCode,
            modalParamInfo.value.id,
            modalParamInfo.value.paramName,
            modalParamInfo.value.paramValue,
          ).then(() => {
            console.log('修改成功');
            showParamModalRef.value = false;
            refreshParamList();
          });
        }
      }

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
        nodeColumns,
        hasSelectCluster,
        refreshClusterData,
        nodesRefresh,
        renderOption,
        addNodeLogic,
        hasAnyNoSyncNode,
        syncAllNode,
        modalNodeInfo,
        modalMode,
        showModalRef,
        showParamModalRef, cancelModal,
        refreshParamList,
        confirmModal, trim,
        DataFromEnum, nodeParmas: nodeParams, paramsColumns,
        loadingParamRef, addParamLogic, modalParamMode, confirmParamModal, modalParamInfo, cancelParamModal
      }
    },
    mounted() {
      this.refreshClusterData();
    }
  });
</script>