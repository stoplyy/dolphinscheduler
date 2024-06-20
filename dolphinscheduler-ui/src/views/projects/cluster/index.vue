<template>
  <NSpace vertical>
    <NCard>
      <NSpace justify='space-between'>
        <NButton type="primary" @click="refreshClusterData" class='btn-create-project'>刷新</NButton>
        <NButton type="primary" @click="addClusterLogic" class='btn-create-project'>手动添加集群</NButton>
      </NSpace>
    </NCard>
    <NCard>
      <NDataTable :data="clusterListData" :columns="projectColumns" :loading="loadingRef" :width="tableWidth">
      </NDataTable>
    </NCard>
    <NModal v-model:show="showParamModalRef" :show-icon="false" class="custom-card" preset="dialog"
      :title="modalParamMode" @positive-click="confirmParamModal" @negative-click="cancelParamModal" positive-text="确认"
      negative-text="取消">
      <NForm :model="modalParamInfo" ref="formParamRef">
        <NFormItem label="参数名" path="paramName" :allowInput="trim">
          <NInput v-model:value="modalParamInfo.paramName" :disabled="modalParamMode === '查看'" />
        </NFormItem>
        <NFormItem label="参数默认值" path="paramValue" :allowInput="trim">
          <NInput v-model:value="modalParamInfo.paramValue" :disabled="modalParamMode === '查看'" />
        </NFormItem>
        <NFormItem label="描述" path="description" :allowInput="trim" v-show="modalParamMode !== '创建'">
          <NInput type="textarea" v-model:value="modalParamInfo.description" :disabled="modalParamMode === '查看'" />
        </NFormItem>
      </NForm>
    </NModal>
    <NModal v-model:show="showModalRef" :show-icon="false" class="custom-card" style="width: 50%" preset="dialog"
      :title="modalMode" @positive-click="confirmModal" @negative-click="cancelModal" positive-text="确认"
      negative-text="算了">
      <NCard>
        <NForm :model="modalClusterInfo" ref="formRef" :rules="formRules">
          <NFormItem label="集群Id" path="clusterId" :allowInput="trim">
            <NInput v-model:value="modalClusterInfo.clusterId"
              :disabled="modalMode === '查看' || modalClusterInfo.from === DataFromEnum.AUTO" />
          </NFormItem>
          <NFormItem label="集群名称" path="clusterName" :allowInput="trim">
            <NInput v-model:value="modalClusterInfo.clusterName" :disabled="modalMode === '查看'" />
          </NFormItem>
          <NFormItem label="AppId" path="appId" :allowInput="trim" v-show="modalMode !== '创建'">
            <NInput v-model:value="modalClusterInfo.appId" :disabled="modalMode === '查看'" />
          </NFormItem>
          <NFormItem label="描述" path="description" :allowInput="trim">
            <NInput type="textarea" v-model:value="modalClusterInfo.description" :disabled="modalMode === '查看'" />
          </NFormItem>
        </NForm>
      </NCard>
      <NCard v-show="modalMode !== '创建'">
        <NSpace justify='space-between'>
          <NButton size="small" type="primary" @click="addParamLogic" class='btn-create-project'>添加参数</NButton>
        </NSpace>
        <NDataTable :data="clusterParmas" :columns="paramsColumns" :loading="loadingParamRef">
        </NDataTable>
      </NCard>
    </NModal>
  </NSpace>
</template>

<script lang="ts">
  import { NButton, NCard, NDataTable, NDialog, NDialogProvider, NDynamicTags, NForm, NFormItem, NFormItemGi, NFormItemRow, NGrid, NGridItem, NInput, NModal, NPagination, NSelect, NSpace, NTable } from 'naive-ui';
  import { defineComponent, getCurrentInstance, reactive, ref } from 'vue';
  import type { Router } from 'vue-router';
  import { useRouter } from 'vue-router';
  import { useClusterTable, getClusterData } from './cluster-tables';
  import { DataFromEnum, OpsClusterInfo } from './types';

  import { createClusterParameter, createProjectCluster, deleteClusterParameter, deleteProjectCluster, getPlatformClusterList, getPlatformRest, queryClusterParametersList, queryProjectClusterList, updateClusterParameter, updateProjectCluster } from '@/service/modules/project-platform';
  import { PlatformRestEnum, ProjectClusterParameter } from '@/service/modules/project-platform/platform';
  import { useParamsTable } from './params-tables';

  export default defineComponent({
    name: 'ProjectsCluster',
    components: {
      NButton,
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
      const columns = ref([
        { title: '集群ID', key: 'clusterId' },
        { title: '集群名称', key: 'clusterName' },
        { title: '关联AppId', key: 'appid' },
        // 更多列的配置
      ]);
      const formRules = {
        clusterId: [
          { required: true, message: '请输入集群Id', trigger: 'blur' }
        ],
        clusterName: [
          { required: true, message: '请输入集群名称', trigger: 'blur' }
        ]
      }

      const trim = getCurrentInstance()?.appContext.config.globalProperties.trim
      const router: Router = useRouter()
      const showParamModalRef = ref(false);
      const loadingRef = ref(false);
      const loadingParamRef = ref(false);
      const projectClusterCode = ref<string | undefined>(undefined);
      const projectCode = ref(Number(router.currentRoute.value.params.projectCode));
      const projectName = ref(String(router.currentRoute.value.query.projectName));
      const clusterListData = ref<OpsClusterInfo[]>();
      const clusterParmas = ref<ProjectClusterParameter[]>([] as ProjectClusterParameter[]);
      const clusterTags = ref<string[]>([]);
      const tableWidth = ref<number>(1600);

      const currentRowParam = ref<ProjectClusterParameter>({} as ProjectClusterParameter);
      const currentRowCluster = ref<OpsClusterInfo>({
        clusterName: '',
        clusterId: '',
        from: DataFromEnum.MANUAL
      } as OpsClusterInfo);

      const modalClusterInfo = reactive<OpsClusterInfo>({} as OpsClusterInfo);
      const modalParamInfo = ref<ProjectClusterParameter>({} as ProjectClusterParameter);

      const modalMode = ref<string>('创建'); // 创建、修改、查看、同步
      const modalParamMode = ref<string>('创建'); // 创建、修改、查看
      const showModalRef = ref(false);

      const editClusterLogic = async (row: OpsClusterInfo) => {
        currentRowCluster.value = row;
        Object.assign(modalClusterInfo, row);

        if (row.id) {
          await refreshParamList();
        } else {
          clusterParmas.value = [];
        }

        modalMode.value = '修改';
        showModalRef.value = true;
      }

      async function refreshParamList() {
        await queryClusterParametersList(projectCode.value, modalClusterInfo.id).then((res) => {
          clusterParmas.value = res;
        });
        await getPlatformRest(projectName.value, PlatformRestEnum.CLUSTER_PARAMS, modalClusterInfo.clusterId, '', '').then((res) => {
          res.forEach((val, key) => {
            clusterParmas.value.push({
              paramName: key,
              paramValue: val,
              from: DataFromEnum.AUTO,
            } as ProjectClusterParameter);
          });
        });
      }

      const deleteClusterLogic = async (row: OpsClusterInfo) => {
        await deleteProjectCluster(projectCode.value, row.id).then(() => {
          console.log('删除成功');
          refreshClusterData();
        });
      }

      const addClusterLogic = () => {
        currentRowCluster.value = reactive<OpsClusterInfo>({ from: DataFromEnum.MANUAL } as OpsClusterInfo);
        Object.assign(modalClusterInfo, currentRowCluster.value);
        modalMode.value = '创建';
        showModalRef.value = true;

        console.log('clusterInfo:', currentRowCluster);
      }

      const syncClusterLogic = async (row: OpsClusterInfo) => {
        await createProjectCluster(projectCode.value, row.clusterName, row.clusterId, row.description).then(() => {
          //弹窗提示成功
          refreshClusterData();
        });
      }



      const projectColumns = useClusterTable({
        deleteLogic: deleteClusterLogic,
        editLogic: editClusterLogic,
        syncLogic: syncClusterLogic
      });

      const cancelModal = () => {
        console.log('cancelModal');
        showModalRef.value = false;
      }

      const confirmModal = async () => {
        Object.assign(currentRowCluster.value, modalClusterInfo);
        if (modalMode.value === '创建') {
          await createProjectCluster(projectCode.value, modalClusterInfo.clusterName, modalClusterInfo.clusterId, modalClusterInfo.description).then(() => {
            console.log('创建成功');
            showModalRef.value = false;
            refreshClusterData();
          });
        } else if (modalMode.value === '修改') {
          if (!modalClusterInfo.id) {
            await createProjectCluster(projectCode.value, modalClusterInfo.clusterName, modalClusterInfo.clusterId, modalClusterInfo.description).then(() => {
              console.log('修改成功');
              showModalRef.value = false;
              refreshClusterData();
            });
            return;
          } else {
            await updateProjectCluster(projectCode.value, modalClusterInfo).then(() => {
              console.log('修改成功');
              showModalRef.value = false;
              refreshClusterData();
            });
          }
        }
      }

      const deleteParamLogic = async (row: ProjectClusterParameter) => {
        await deleteClusterParameter(projectCode.value, row.clusterCode, row.id).then(() => {
          console.log('删除成功');
          refreshParamList();
        });
      }

      const editParamLogic = async (row: ProjectClusterParameter) => {
        currentRowParam.value = row;
        Object.assign(modalParamInfo.value, row);

        modalParamMode.value = '修改';
        showParamModalRef.value = true;
      }

      const addParamLogic = () => {
        modalParamInfo.value = { clusterCode: currentRowCluster.value.id } as ProjectClusterParameter;
        modalParamMode.value = '创建';
        showParamModalRef.value = true;
      }

      const confirmParamModal = async () => {
        if (modalParamMode.value === '创建') {
          await createClusterParameter(projectCode.value, modalClusterInfo.id, modalParamInfo.value.paramName, modalParamInfo.value.paramValue).then(() => {
            console.log('创建成功');
            showParamModalRef.value = false;
            refreshParamList();
          });
        } else {
          await updateClusterParameter(projectCode.value, modalParamInfo.value).then(() => {
            console.log('修改成功');
            showParamModalRef.value = false;
            refreshParamList();
          });
        }
      }

      const cancelParamModal = () => {
        console.log('cancelParamModal');
        showParamModalRef.value = false;
      }

      const paramsColumns = useParamsTable({
        deleteLogic: deleteParamLogic,
        editLogic: editParamLogic,
        syncLogic: (row: ProjectClusterParameter) => { }
      })

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


      return {
        loadingRef,
        projectClusterCode,
        projectCode,
        projectName,
        clusterListData,
        loadingParamRef,
        clusterParmas,
        paramsColumns,
        clusterTags,
        tableWidth,
        clusterInfo: currentRowCluster,
        modalMode,
        cancelModal,
        confirmModal,
        showModalRef,
        columns,
        formRules,
        addClusterLogic,
        trim,
        DataFromEnum,
        refreshClusterData,
        addParamLogic,
        modalParamInfo,
        modalClusterInfo,
        projectColumns,
        showParamModalRef,
        modalParamMode, confirmParamModal, cancelParamModal
      }
    },

    mounted() {
      this.refreshClusterData();
    }
  });
</script>