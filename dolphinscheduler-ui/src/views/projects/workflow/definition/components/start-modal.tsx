/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import Modal from '@/components/modal'
import {
  ArrowDownOutlined,
  ArrowUpOutlined,
  DeleteOutlined,
  LinkOutlined,
  PlusCircleOutlined
} from '@vicons/antd'
import {
  CascaderOption,
  NButton,
  NCascader,
  NCheckbox,
  NDatePicker,
  NForm,
  NFormItem,
  NIcon,
  NInput,
  NRadio,
  NRadioButton,
  NRadioGroup,
  NSelect,
  NSpace,
  NSwitch,
  NTag,
  NText,
  NTooltip
} from 'naive-ui'
import {
  computed,
  defineComponent,
  getCurrentInstance,
  h,
  onMounted,
  PropType,
  ref,
  toRefs,
  watch
} from 'vue'
import { useI18n } from 'vue-i18n'
import { useRoute } from 'vue-router'
import { platformDef } from './platform-source'
import styles from '../index.module.scss'
import { IDefinitionData } from '../types'
import { IParam } from './types'
import { useForm } from './use-form'
import { useModal } from './use-modal'
import { PlatformConst } from '@/service/modules/project-platform/platform'

const props = {
  row: {
    type: Object as PropType<IDefinitionData>,
    default: {}
  },
  show: {
    type: Boolean as PropType<boolean>,
    default: false
  },
  taskCode: {
    type: String
  }
}

export default defineComponent({
  name: 'workflowDefinitionStart',
  props,
  emits: ['update:show', 'update:row', 'updateList'],
  setup(props, ctx) {
    const parallelismRef = ref(false)
    const { t } = useI18n()
    const route = useRoute()

    const { startState } = useForm()

    const { generalProjectSources,loadingSource } = platformDef()

    const showSourceModal = ref(false)
    const sourceModalSelectedIds = ref<Array<string | number>>([])
    const taskPlatformClusters = ref<Array<string | number>>([])
    const taskPlatformNodes = ref<Array<string | number>>([])
    const setPlatsourceParams = ref<Map<IParam, (number | string)[]>>(new Map())
    let modelSourceParam: IParam;
    const taskPlatformSourceParam = ref("")

    const {
      variables,
      handleStartDefinition,
      getWorkerGroups,
      getTenantList,
      getAlertGroups,
      getEnvironmentList,
      getStartParamsList
    } = useModal(startState, ctx)

    const hideModal = () => {
      ctx.emit('update:show')
    }

    const handleStart = () => {
      handleStartDefinition(props.row.code, props.row.version)
    }

    const confirmSourceModal = () => {
      modelSourceParam.value = sourceModalSelectedIds.value.join(",")

      setPlatsourceParams.value.set(modelSourceParam, sourceModalSelectedIds.value);
      const selectedIds: (string | number)[] = []
      setPlatsourceParams.value.forEach((value, key) => { 
        value.forEach((item: string | number) => { 
          if (selectedIds.indexOf(item) === -1) {
            selectedIds.push(item)
          }
        })
      })
      taskPlatformSourceParam.value = selectedIds.join(",")
      
      if (startState.startForm.isPlatform) {
        startState.startForm.platformSource = taskPlatformSourceParam.value
      }
      if (startState.startForm.isPlatformCluster) { 
        startState.startForm.platformClusters = taskPlatformClusters.value.join(",")
      }
      if (startState.startForm.isPlatformNode) { 
        startState.startForm.platformNodes = taskPlatformNodes.value.join(",")
      }
      showSourceModal.value = false
    }

    const updateSourceModal = (value: Array<string|number>) => {
      sourceModalSelectedIds.value=value
    }

    const handleSourceModal = (param: IParam) => {
      console.log("handleSourceModal:" + param)
      modelSourceParam = param

      if (setPlatsourceParams.value.has(param)) { 
        sourceModalSelectedIds.value = setPlatsourceParams.value.get(param) || []
      } else {
        sourceModalSelectedIds.value = []
      }
      showSourceModal.value = true
    }

    const renderProjectSourcesOptions = (projectCode: number) => { 
      const { projectSources, clusterOptions, nodeOptions } = generalProjectSources(projectCode)
      return projectSources
    }

    const renderProjectClusterOptions = (projectCode: number) => {
      const { projectSources, clusterOptions, nodeOptions } = generalProjectSources(projectCode)
      return clusterOptions
    }

    const renderProjectNodeOptions = (projectCode: number) => {
      const { projectSources, clusterOptions, nodeOptions } = generalProjectSources(projectCode)
      return nodeOptions
    }

    const generalWarningTypeListOptions = () => [
      {
        value: 'NONE',
        label: t('project.workflow.none_send')
      },
      {
        value: 'SUCCESS',
        label: t('project.workflow.success_send')
      },
      {
        value: 'FAILURE',
        label: t('project.workflow.failure_send')
      },
      {
        value: 'ALL',
        label: t('project.workflow.all_send')
      }
    ]

    const generalPriorityList = () => [
      {
        value: 'HIGHEST',
        label: 'HIGHEST',
        color: '#ff0000',
        icon: ArrowUpOutlined
      },
      {
        value: 'HIGH',
        label: 'HIGH',
        color: '#ff0000',
        icon: ArrowUpOutlined
      },
      {
        value: 'MEDIUM',
        label: 'MEDIUM',
        color: '#EA7D24',
        icon: ArrowUpOutlined
      },
      {
        value: 'LOW',
        label: 'LOW',
        color: '#2A8734',
        icon: ArrowDownOutlined
      },
      {
        value: 'LOWEST',
        label: 'LOWEST',
        color: '#2A8734',
        icon: ArrowDownOutlined
      }
    ]

    const showTaskDependType = computed(
      () => route.name === 'workflow-definition-detail'
    )

    const renderLabel = (option: any) => {
      return [
        h(
          NIcon,
          {
            style: {
              verticalAlign: 'middle',
              marginRight: '4px',
              marginBottom: '3px'
            },
            color: option.color
          },
          {
            default: () => h(option.icon)
          }
        ),
        option.label
      ]
    }

    const updateWorkerGroup = () => {
      startState.startForm.environmentCode = null
    }

    const addStartParams = () => {
      variables.startParamsList.push({
        prop: '',
        value: ''
      })
    }

    const updateParamsList = (index: number, param: Array<string>) => {
      variables.startParamsList[index].prop = param[0]
      variables.startParamsList[index].value = param[1]
    }

    const removeStartParams = (index: number) => {
      let param = variables.startParamsList.splice(index, 1)
    }

    const trim = getCurrentInstance()?.appContext.config.globalProperties.trim

    onMounted(() => {
      getWorkerGroups()
      getTenantList()
      getAlertGroups()
      getEnvironmentList()
    })

    watch(
      () => props.show,
      () => {
        if (props.show) {
          getStartParamsList(props.row.code)
          if (props.taskCode)
            startState.startForm.startNodeList = props.taskCode
        }
      }
    )

    return {
      t,
      showTaskDependType,
      parallelismRef,
      hideModal,
      handleStart,
      generalWarningTypeListOptions,
      generalPriorityList,
      renderProjectSourcesOptions,
      renderProjectClusterOptions,
      renderProjectNodeOptions,
      loadingSource,
      renderLabel,
      updateWorkerGroup,
      removeStartParams,
      addStartParams,
      updateParamsList,
      ...toRefs(variables),
      ...toRefs(startState),
      ...toRefs(props),
      showSourceModal,
      handleSourceModal,
      confirmSourceModal,
      updateSourceModal,
      sourceModalSelectedIds,
      taskPlatformSourceParam,
      taskPlatformClusters,
      taskPlatformNodes,
      trim
    }
  },

  render() {
    const { t } = this
    return (
      <Modal
        show={this.show}
        title={t('project.workflow.set_parameters_before_starting')}
        onCancel={this.hideModal}
        onConfirm={this.handleStart}
        confirmLoading={this.saving}
      >
        <NForm label-placement="left" label-width="auto" ref='startFormRef' model={this.startForm} rules={this.rules}>
          <NFormItem
            label={t('project.workflow.workflow_name')}
            path='workflow_name'
          >
            {this.row.name}
          </NFormItem>
          <NFormItem
            label={t('project.workflow.failure_strategy')}
            path='failureStrategy'
          >
            <NRadioGroup v-model:value={this.startForm.failureStrategy}>
              <NSpace>
                <NRadio value='CONTINUE'>
                  {t('project.workflow.continue')}
                </NRadio>
                <NRadio value='END'>{t('project.workflow.end')}</NRadio>
              </NSpace>
            </NRadioGroup>
          </NFormItem>
          {this.showTaskDependType && (
            <NFormItem
              label={t('project.workflow.node_execution')}
              path='taskDependType'
            >
              <NRadioGroup v-model:value={this.startForm.taskDependType}>
                <NSpace>
                  <NRadio value='TASK_POST'>
                    {t('project.workflow.backward_execution')}
                  </NRadio>
                  <NRadio value='TASK_PRE'>
                    {t('project.workflow.forward_execution')}
                  </NRadio>
                  <NRadio value='TASK_ONLY'>
                    {t('project.workflow.current_node_execution')}
                  </NRadio>
                </NSpace>
              </NRadioGroup>
            </NFormItem>
          )}
          <NFormItem
            label={t('project.workflow.notification_strategy')}
            path='warningType'
          >
            <NSelect
              options={this.generalWarningTypeListOptions()}
              v-model:value={this.startForm.warningType}
            />
          </NFormItem>
          {this.startForm.warningType !== 'NONE' && (
            <NFormItem
              label={t('project.workflow.alarm_group')}
              path='warningGroupId'
              required
            >
              <NSelect
                options={this.alertGroups}
                placeholder={t('project.workflow.please_choose')}
                v-model:value={this.startForm.warningGroupId}
                clearable
                filterable
              />
            </NFormItem>
          )}
          <NFormItem
            label={t('project.workflow.workflow_priority')}
            path='processInstancePriority'
          >
            <NSelect
              options={this.generalPriorityList()}
              renderLabel={this.renderLabel}
              v-model:value={this.startForm.processInstancePriority}
            />
          </NFormItem>
          <NFormItem
            label={t('project.workflow.worker_group')}
            path='workerGroup'
          >
            <NSelect
              options={this.workerGroups}
              onUpdateValue={this.updateWorkerGroup}
              v-model:value={this.startForm.workerGroup}
              filterable
            />
          </NFormItem>
          <NFormItem
            label={t('project.workflow.tenant_code')}
            path='tenantCode'
          >
            <NSelect
              options={this.tenantList}
              v-model:value={this.startForm.tenantCode}
              filterable
            />
          </NFormItem>

          <NFormItem
            label={t('project.workflow.environment_name')}
            path='environmentCode'
          >
            <NSelect
              options={this.environmentList.filter((item: any) =>
                item.workerGroups?.includes(this.startForm.workerGroup)
              )}
              v-model:value={this.startForm.environmentCode}
              clearable
              filterable
            />
          </NFormItem>
          <NFormItem
            label={t('project.workflow.complement_data')}
            path='complement_data'
          >
            <NCheckbox
              checkedValue={'COMPLEMENT_DATA'}
              uncheckedValue={'START_PROCESS'}
              v-model:checked={this.startForm.execType}
            >
              {t('project.workflow.whether_complement_data')}
            </NCheckbox>
          </NFormItem>
          {this.startForm.execType &&
            this.startForm.execType !== 'START_PROCESS' && (
              <NSpace vertical class={styles['width-100']}>
                <NFormItem
                  label={t('project.workflow.mode_of_dependent')}
                  path='complementDependentMode'
                >
                  <NRadioGroup
                    v-model:value={this.startForm.complementDependentMode}
                  >
                    <NSpace>
                      <NRadio value={'OFF_MODE'}>
                        {t('project.workflow.close')}
                      </NRadio>
                      <NRadio value={'ALL_DEPENDENT'}>
                        {t('project.workflow.open')}
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
                {this.startForm.complementDependentMode === 'ALL_DEPENDENT' && (
                  <NFormItem
                    label={t('project.workflow.all_level_dependent')}
                    path='allLevelDependent'
                  >
                    <NRadioGroup
                      v-model:value={this.startForm.allLevelDependent}
                    >
                      <NSpace>
                        <NRadio value={'false'}>
                          {t('project.workflow.close')}
                        </NRadio>
                        <NRadio value={'true'}>
                          {t('project.workflow.open')}
                        </NRadio>
                      </NSpace>
                    </NRadioGroup>
                  </NFormItem>
                )}
                <NFormItem
                  label={t('project.workflow.mode_of_execution')}
                  path='runMode'
                >
                  <NRadioGroup v-model:value={this.startForm.runMode}>
                    <NSpace>
                      <NRadio value={'RUN_MODE_SERIAL'}>
                        {t('project.workflow.serial_execution')}
                      </NRadio>
                      <NRadio value={'RUN_MODE_PARALLEL'}>
                        {t('project.workflow.parallel_execution')}
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
                {this.startForm.runMode === 'RUN_MODE_PARALLEL' && (
                  <NFormItem
                    label={t('project.workflow.parallelism')}
                    path='expectedParallelismNumber'
                  >
                    <NCheckbox v-model:checked={this.parallelismRef}>
                      {t('project.workflow.custom_parallelism')}
                    </NCheckbox>
                    <NInput
                      allowInput={this.trim}
                      disabled={!this.parallelismRef}
                      placeholder={t(
                        'project.workflow.please_enter_parallelism'
                      )}
                      v-model:value={this.startForm.expectedParallelismNumber}
                    />
                  </NFormItem>
                )}
                <NFormItem
                    label={t('project.workflow.order_of_execution')}
                    path='executionOrder'
                >
                  <NRadioGroup v-model:value={this.startForm.executionOrder}>
                    <NSpace>
                      <NRadio value={'DESC_ORDER'}>
                        {t('project.workflow.descending_order')}
                      </NRadio>
                      <NRadio value={'ASC_ORDER'}>
                        {t('project.workflow.ascending_order')}
                      </NRadio>
                    </NSpace>
                  </NRadioGroup>
                </NFormItem>
                <NFormItem
                  label={t('project.workflow.schedule_date')}
                  path={
                    this.startForm.dataDateType === 1
                      ? 'startEndTime'
                      : 'scheduleTime'
                  }
                >
                  <NSpace vertical class={styles['width-100']}>
                    <NRadioGroup
                      name='data-date'
                      v-model:value={this.startForm.dataDateType}
                    >
                      {[
                        { label: t('project.workflow.select_date'), value: 1 },
                        { label: t('project.workflow.enter_date'), value: 2 }
                      ].map((item) => (
                        <NRadioButton {...item} key={item.value} />
                      ))}
                    </NRadioGroup>

                    {this.startForm.dataDateType === 1 ? (
                      <NDatePicker
                        type='datetimerange'
                        clearable
                        v-model:value={this.startForm.startEndTime}
                        placement='top'
                      />
                    ) : (
                      <NInput
                        allowInput={this.trim}
                        clearable
                        type='textarea'
                        v-model:value={this.startForm.scheduleTime}
                        placeholder={t('project.workflow.schedule_date_tips')}
                      />
                    )}
                  </NSpace>
                </NFormItem>
              </NSpace>
            )}
          <NFormItem
              label="项目源"
              path='isPlatform'>
            <NSpace vertical>
              <NCheckbox
                checkedValue={true}
                uncheckedValue={false}
                v-model:checked={this.startForm.isPlatform}>
                使用项目源
              </NCheckbox>
              {this.startForm.isPlatform && (
                <NSpace inline align="center">
                  <NTag type="info">{PlatformConst.P_DATASOURCE_PARAM_NAME}</NTag> :
                  <NInput
                    disabled
                    placeholder="选择源时 自动填充."
                    value={this.taskPlatformSourceParam}/>
              </NSpace>)}
              </NSpace>
          </NFormItem>
          <NFormItem
              label="项目集群"
              path='isPlatformCluster'>
            <NSpace vertical>
              <NCheckbox
                checkedValue={true}
                uncheckedValue={false}
                v-model:checked={this.startForm.isPlatformCluster}
                >
                选择项目集群
              </NCheckbox>
              {this.startForm.isPlatformCluster && (
                <NSpace inline align="center">
                  {h(NTooltip, {}, {
                    trigger: () =>
                      <NTag type="info">{PlatformConst.P_CLUSTER_PARAM_NAME}</NTag>,
                    default: () => '值会被替换为集群参数. 注意：如果只有一个集群值类型为Object,如果多个集群值类型Array! '
                  })}
                   :
                  <NCascader multiple clearable
                    onLoad={() => this.loadingSource}
                    checkStrategy="parent"
                    value={this.taskPlatformClusters}
                    filterable
                    maxTagCount={5}
                    placeholder="选择集群，会被替换为集群参数"
                    themeOverrides={{ columnWidth: "300px", optionFontSize: "14px" }}
                    renderLabel={(option: CascaderOption, checked: boolean) =>
                      h(NTooltip, {},
                        {
                          trigger: () => h(NText, { type: checked ? 'primary' : 'default', style: "min-width:400px" }, { default: () => option.label }),
                          default: () => option.value + ":" + option.label
                        })
                    }
                    options={this.renderProjectClusterOptions(this.projectCode)}
                    onUpdateValue={(value: Array<string | number>) => this.taskPlatformClusters = value}
                    />
              </NSpace>)}
            </NSpace>
          </NFormItem>

          <NFormItem
              label="项目节点"
              path='isPlatformNode'>
            <NSpace vertical>
              <NCheckbox
                checkedValue={true}
                uncheckedValue={false}
                v-model:checked={this.startForm.isPlatformNode}
                >
                选择项目节点
              </NCheckbox>
              {this.startForm.isPlatformNode && (
                <NSpace inline align="center">
                  {h(NTooltip, {}, {
                    trigger: () =>
                      <NTag type="info">{PlatformConst.P_NODE_PARAM_NAME}</NTag>,
                    default: () => '值会被替换为节点参数. 注意：如果只有一个节点 值类型为Object,如果多个节点 值类型Array! '
                  })}
                  :
                  <NCascader multiple clearable
                    onLoad={() => this.loadingSource}
                    checkStrategy="child"
                    value={this.taskPlatformNodes}
                    filterable
                    maxTagCount={5}
                    placeholder="选择项目节点，会被替换为节点参数"
                    themeOverrides={{ columnWidth: "300px", optionFontSize: "14px" }}
                    renderLabel={(option: CascaderOption, checked: boolean) =>
                      h(NTooltip, {},
                        {
                          trigger: () => h(NText, { type: checked ? 'primary' : 'default', style: "min-width:400px" }, { default: () => option.label }),
                          default: () => option.value + ":" + option.label
                        })
                    }
                    options={this.renderProjectNodeOptions(this.projectCode)}
                    onUpdateValue={(value: Array<string | number>) => this.taskPlatformNodes = value}
                  />
                </NSpace>)}
            </NSpace>
          </NFormItem>
          <NFormItem
            label={t('project.workflow.startup_parameter')}
            path='startup_parameter'
          >
            {this.startParamsList.length === 0 ? (
              <NButton text type='primary' onClick={this.addStartParams}>
                <NIcon>
                  <PlusCircleOutlined />
                </NIcon>
              </NButton>
            ) : (
              <NSpace vertical>
                {this.startParamsList.map((item, index) => (
                  <NSpace class={styles.startup} key={Date.now() + index}>
                    <NInput
                      allowInput={this.trim}
                      pair
                      separator=':'
                      placeholder={['prop', 'value']}
                      defaultValue={[item.prop, item.value]}
                      onUpdateValue={(param: Array<string>) =>
                        this.updateParamsList(index, param)
                      }
                    />
                    <NButton
                      text
                      type='error'
                      onClick={() => this.removeStartParams(index)}
                      class='btn-delete-custom-parameter'
                    >
                      <NIcon>
                        <DeleteOutlined />
                      </NIcon>
                    </NButton>
                    <NButton
                      text
                      type='primary'
                      onClick={this.addStartParams}
                      class='btn-create-custom-parameter'
                    >
                      <NIcon>
                        <PlusCircleOutlined />
                      </NIcon>
                    </NButton>
                    <NButton
                      text
                      type='primary'
                      disabled={!this.startForm.isPlatform}
                      onClick={() => this.handleSourceModal(item)}
                      class='btn-create-custom-parameter'
                    >
                      {h(NTooltip, {}, {
                        trigger: () =>
                          <NIcon>
                            <LinkOutlined />
                          </NIcon>,
                        default: () => '选择Platform源  将源id使用,连接作为参数值'
                      })}
                    </NButton>
                  </NSpace>
                ))}
              </NSpace>
            )}
          </NFormItem>
          <NFormItem
            label={t('project.workflow.whether_dry_run')}
            path='dryRun'
          >
            <NSwitch
              checkedValue={1}
              uncheckedValue={0}
              v-model:value={this.startForm.dryRun}
            />
          </NFormItem>
          <NFormItem label={t('project.workflow.whether_test')} path='testFlag'>
            <NSwitch
              checkedValue={1}
              uncheckedValue={0}
              v-model:value={this.startForm.testFlag}
            />
          </NFormItem>
        </NForm>
        <Modal
          show={this.showSourceModal}
          title="选择项目源"
          onCancel={() => { this.showSourceModal = false }}
          onConfirm={this.confirmSourceModal}>
          <NCascader multiple clearable
            onLoad={()=>this.loadingSource}
            checkStrategy="child"
            value={this.sourceModalSelectedIds}
            filterable
            maxTagCount={5}
            themeOverrides={{ columnWidth: "400px", optionFontSize: "13px" }}
            renderLabel={(option: CascaderOption, checked: boolean) => 
              h(NTooltip, {}, 
                {
                  trigger: () => h(NText, { type: checked ? 'primary' : 'default', style: "min-width:400px" }, { default: () => option.label }),
                  default: () => option.disabled ? (option.label + ' 节点未关联源，不可选择！') : (option.value + ":" + option.label)
                })
            }
            options={this.renderProjectSourcesOptions(this.projectCode)}
            onUpdateValue={this.updateSourceModal}
            placeholder="Load Source from project" />
        </Modal>
      </Modal>
    )
  }
})
