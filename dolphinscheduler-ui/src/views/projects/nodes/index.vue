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
          <NButton type="primary" @click="nodesRefresh()" :disabled="loadingRef || !hasSelectedSyncCluster()"
            class='btn-create-project'>刷新</NButton>
        </NSpace>
        <NSpace>
          <NButton type="primary" @click="addNodeLogic" :disabled="!hasSelectedSyncCluster()" round
            class='btn-create-project'>
            <template #icon>
              <NIcon>
                <PlusOutlined />
              </NIcon>
            </template>添加节点
          </NButton>
          <NTooltip>
            <template #trigger>
              <NButton type="success" class='btn-create-project' @click="syncHalleyNode" round
                :disabled="loadingRef || !hasSelectedSyncClusterAppId()">
                <template #icon>
                  <NIcon>
                    <SyncOutlined />
                  </NIcon>
                </template>Halley同步
              </NButton>
            </template>
            <span>通过Halley接口同步节点（需要集群关联AppId;会删除同为halley导入但是不在halley接口中的节点）</span>
          </NTooltip>
          <NPopconfirm @positive-click="syncAllNode">
            <template #trigger>
              <NTooltip>
                <template #trigger>
                  <NButton type="success" class='btn-create-project' round
                    :disabled="loadingRef || !hasSelectedSyncCluster()">
                    <template #icon>
                      <NIcon>
                        <SyncOutlined />
                      </NIcon>
                    </template>API同步
                  </NButton>
                </template>
                <span>通过配置的API接口地址同步节点（需要项目配置platform_xx;会删除同为auto导入但是不在API接口中的节点）</span>
              </NTooltip>
            </template>
            是否同步所有节点？
          </NPopconfirm>
          <NButton type="primary" @click="testConnectivity()" round
            :disabled="loadingRef || !hasSelectedSyncCluster() || testComploading" class='btn-create-project'>
            <template #icon>
              <NIcon>
                <DisconnectOutlined />
              </NIcon>
            </template>连接检测
          </NButton>
          <NPopconfirm @positive-click="createALlNodeSource">
            <template #trigger>
              <NTooltip>
                <template #trigger>
                  <NButton type="warning" class='btn-create-project' round
                    :disabled="loadingRef || !hasSelectedSyncCluster() || !hasAnyNoSyncSourceNode()">
                    <template #icon>
                      <NIcon>
                        <DisconnectOutlined />
                      </NIcon>
                    </template>创建源
                  </NButton>
                </template>
                <span>使用IP调用SRE接口，添加平台公钥，快速创建SSH源。（ip选取策略: 节点参数 platform_sre_ip >> nodeKey）</span>
              </NTooltip>
            </template>
            是否确认快速创建源？
          </NPopconfirm>
        </NSpace>
      </NSpace>
    </NCard>
    <NCard>
      <NDataTable :data="nodesList" :columns="nodeColumns" :loading="loadingRef" :width="tableWidth">
        <template #empty>
          <NEmpty description="你什么也找不到">
            <template #extra>
              <NButton size="small">
                看看别的
              </NButton>
            </template>
          </NEmpty>
        </template>
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
  import { createProjectNode, createProjectNodeParameter, deleteProjectNode, deleteProjectNodeParameter, testConnectivityByHalley, createSourceWithAllNode, getPlatformRestByProject, queryProjectNodeList, queryProjectNodeALlParameters, syncAllNodeData, syncNodesByHalley, syncSourceNodeData, updateProjectNode, updateProjectNodeParameter, getHalleyParamsByNode } from '@/service/modules/project-platform';
  import { PlatformRestEnum, ProjectNodeParameter } from '@/service/modules/project-platform/platform';
  import { NButton, NCard, NEmpty, NDataTable, NDialog, NDialogProvider, NPopconfirm, NDynamicTags, NIcon, NForm, NFormItem, NFormItemGi, NFormItemRow, NGrid, NGridItem, NInput, NModal, NPagination, NSelect, NSpace, NTable, NTooltip, SelectOption, NDropdown, NMenu } from 'naive-ui';
  import { computed, defineComponent, getCurrentInstance, h, reactive, ref, VNode, watch } from 'vue';
  import type { Router } from 'vue-router';
  import { useRouter } from 'vue-router';
  import { getClusterData } from '../cluster/cluster-tables';
  import { useParamsTable } from '../cluster/params-tables';
  import { DataFromEnum, OpsClusterInfo } from '../cluster/types';
  import { OpsNodeInfo } from './types';
  import { useNodeTable } from './use-tables';
  import { CopyOutlined, DisconnectOutlined, PlusOutlined, SyncOutlined, MoreOutlined } from '@vicons/antd'

  export default defineComponent({
    name: 'ProjectsNodes',
    components: {
      NButton,
      NTooltip,
      NEmpty,
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
      NPagination,
      MoreOutlined,
      NIcon, DisconnectOutlined, SyncOutlined, PlusOutlined, CopyOutlined
    },
    setup() {
      const router: Router = useRouter()
      const trim = getCurrentInstance()?.appContext.config.globalProperties.trim
      const clusterListData = ref<OpsClusterInfo[]>();
      const clusterTags = ref<string[]>([]);
      const tableWidth = ref<number>(1600);
      const testComploading = ref(false);

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
          value: cluster.clusterId,
          // disabled: !(cluster.id && cluster.id >= 0)
        })) || [];
      });

      const renderOption = ({ node, option }: { node: VNode; option: SelectOption; }) => h(NTooltip, {
        placement: 'left',
      }, {
        trigger: () => node,
        default: () => {
          if (clusterListData === undefined || clusterListData.value === undefined) {
            return '';
          }
          let cur = clusterListData.value.findLast((item) => item.clusterId === option.value);
          if (cur === undefined || cur === null) {
            return '';
          }
          return cur.clusterName + " " + cur.description;
        }
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

      const hasSelectedSyncCluster = () => {
        return selectedCluster.value && selectedCluster.value.id > 0;
      }

      const hasSelectedSyncClusterAppId = () => {
        return selectedCluster.value && selectedCluster.value.appId && selectedCluster.value.appId !== '';
      }

      const hasAnyNoSyncSourceNode = () => {
        return nodesList.value.filter((node) => node.dataSourceCode === null || node.dataSourceCode === 0).length > 0;
      }

      const nodesRefresh = async () => {
        await clusterChanged(projectClusterCode.value);
      }

      const disabledPubKey = () => {
        return nodesList.value.filter((node) => node.isConnected === false).length === 0;
      }

      const testConnectivity = async () => {
        loadingRef.value = true;
        await testConnectivityByHalley(projectCode.value, selectedCluster.value.id).then((res: Map<String, Boolean>) => {
          //objec 转map
          const resMap = new Map(Object.entries(res));
          nodesList.value.forEach((node) => {
            if (resMap.has(node.id.toString())) {
              node.isConnected = resMap.get(node.id.toString()) == true;
            }
          });
        }).finally(() => {
          loadingRef.value = false;
        });
      }

      const createALlNodeSource = async () => {
        loadingRef.value = true;
        await createSourceWithAllNode(projectCode.value, selectedCluster.value.id).then(() => {
          console.log('创建成功');
          nodesRefresh();
        }).finally(() => {
          loadingRef.value = false;
        });
      }

      const clusterChanged = async (val: string) => {
        loadingRef.value = true;

        selectedCluster.value = clusterListData.value?.find((item) => item.clusterId === val) || {} as OpsClusterInfo;
        const nodeListDataTmp = [] as OpsNodeInfo[];

        const manuallyNodes = await queryProjectNodeList(projectCode.value, selectedCluster.value.id);
        manuallyNodes.map((item) => {
          if (!nodeListDataTmp.find((node) => node.nodeId === item.nodeId)) {
            nodeListDataTmp.push({
              ...item,
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

      const nodeColumns = useNodeTable(
        async (row: OpsNodeInfo) => {
          if (modalNodeInfo.id != row.id) {
            currentRow.value = row;
            currentRow.value.stellaropsClusterName = selectedCluster.value.clusterName;
            Object.assign(modalNodeInfo, currentRow.value);
            await refreshParamList();
          }
          modalMode.value = '修改';
          showModalRef.value = true;
        },
        async (row: OpsNodeInfo) => {
          await deleteProjectNode(projectCode.value, row.clusterCode, row.id).then(() => {
            console.log('删除成功');
            if (row.from === DataFromEnum.AUTO) {
              nodesRefresh();
            } else {
              nodesList.value.splice(nodesList.value.findIndex((item) => item.id === row.id), 1);
            }
          });
        },
        async (row) => {
          if (!hasSelectedSyncCluster()) {
            window.$message.error('集群尚未同步，无法操作节点信息！');
            return;
          }
          await createProjectNode(
            DataFromEnum.AUTO,
            projectCode.value,
            selectedCluster.value.id,
            row.nodeKey,
            row.nodeName,
            row.nodeId,
            '节点同步').then(() => {
              console.log('同步成功');
              nodesRefresh();
            });
        },
        async (row) => {
          if (!hasSelectedSyncCluster()) {
            window.$message.error('集群尚未同步，无法操作节点信息！');
            return;
          }
          await syncSourceNodeData(projectCode.value, selectedCluster.value.id, row.id).then(() => {
            console.log('源创建成功');
            nodesRefresh();
          });
        },
      )

      const syncHalleyNode = async () => {
        loadingRef.value = true;
        //ToDo：通过Halley接口同步节点
        await syncNodesByHalley(projectCode.value, selectedCluster.value.id).then((res: boolean) => {
          if (res) {
            console.log('同步成功');
            nodesRefresh();
          }
          loadingRef.value = false;
        });
      }

      const syncAllNode = async () => {
        loadingRef.value = true;

        await syncAllNodeData(projectCode.value, selectedCluster.value.id).then((res: boolean) => {
          if (res) {
            console.log('同步成功');
            nodesRefresh();
          }
        });

        loadingRef.value = false;
      };

      async function refreshParamList() {
        loadingParamRef.value = true;
        if (modalNodeInfo.id && modalNodeInfo.id > 0) {
          nodeParams.value = [];
          //TODO: 提供选项，是否展示 halley、api、system 参数
          await queryProjectNodeALlParameters(projectCode.value, modalNodeInfo.clusterCode, modalNodeInfo.id).then((res) => {
            res.forEach((val) => {
              // debugger
              if (val.description === "FROM_HALLEY") {
                val.from = DataFromEnum.HALLEY;
              } else if (val.description === "FROM_API") {
                val.from = DataFromEnum.AUTO;
              } else if (val.description === "SYSTEM") {
                val.from = DataFromEnum.SYSTEM;
              } else {
                val.from = DataFromEnum.MANUAL;
              }
              nodeParams.value.push(val);
            });
            //排序 MANUAL>SYSTEM>HALLEY>API
            nodeParams.value.sort((a, b) => {
              if (a.from === b.from) {
                return 0;
              } else if (a.from === DataFromEnum.MANUAL) {
                return -1;
              } else if (a.from === DataFromEnum.SYSTEM) {
                return b.from === DataFromEnum.MANUAL ? 1 : -1;
              } else if (a.from === DataFromEnum.HALLEY) {
                return b.from === DataFromEnum.MANUAL || b.from === DataFromEnum.SYSTEM ? 1 : -1;
              } else {
                return 1;
              }
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
      }

      const cancelModal = () => {
        console.log('cancelModal');
        showModalRef.value = false;
      }

      const confirmModal = async () => {
        if (modalMode.value === '创建') {
          await createProjectNode(
            DataFromEnum.MANUAL,
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
              DataFromEnum.MANUAL,
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
        hasSelectedSyncCluster,
        hasSelectedSyncClusterAppId,
        refreshClusterData,
        nodesRefresh,
        testConnectivity,
        testComploading,
        renderOption,
        addNodeLogic,
        hasAnyNoSyncSourceNode,
        syncAllNode,
        modalNodeInfo,
        modalMode,
        showModalRef,
        showParamModalRef, cancelModal,
        refreshParamList,
        confirmModal, trim, SyncOutlined,
        DataFromEnum, nodeParmas: nodeParams, paramsColumns,
        loadingParamRef, addParamLogic, modalParamMode, confirmParamModal, modalParamInfo, cancelParamModal,
        disabledPubKey,
        syncHalleyNode, createALlNodeSource
      }
    },
    mounted() {
      this.refreshClusterData();
    }
  });
</script>