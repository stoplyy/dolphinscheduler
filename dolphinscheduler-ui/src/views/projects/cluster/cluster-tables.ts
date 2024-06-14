import { h, reactive, vShow } from "vue";
import { NButton, NTag, NSpace, NTooltip, NPopconfirm } from "naive-ui";
import { DataFromEnum } from './types'
import type { OpsClusterInfo, DataTableRowOper } from './types'
import { DeleteOutlined, EditOutlined } from '@vicons/antd'
import {
  COLUMN_WIDTH_CONFIG
} from '@/common/column-width-config'

export function useClusterTable(object: DataTableRowOper) {

  const handleEdit = (row: OpsClusterInfo) => {
    object.editLogic(row);
  }

  const handleDelete = (row: OpsClusterInfo) => {
    object.deleteLogic(row);
  }

  const projectColumns =
    [
      {
        title: '#',
        key: 'id-index',
        ...COLUMN_WIDTH_CONFIG['index'],
        render: (row: any, index: number) => index + 1
      },
      // {
      //   title: 'code',
      //   key: 'id',
      //   vShow: false,
      //   ...COLUMN_WIDTH_CONFIG['index']
      // },
      {
        title: 'ID',
        key: 'clusterId',
        ...COLUMN_WIDTH_CONFIG['index']
      },
      {
        title: 'Name',
        key: 'clusterName',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: '关联AppId',
        key: 'appid',
        ...COLUMN_WIDTH_CONFIG['name'],
        render: (row: OpsClusterInfo) => {
          return h(NTag, { type: (row.appId === null || row.appId === '') ? 'warning' : 'info' }, { default: () => (row.appId === null || row.appId === '') ? '未设置！' : row.appId })
        }
      },
      {
        title: "Tags",
        key: "tags",
        ...COLUMN_WIDTH_CONFIG['note']
      },
      {
        title: "描述",
        key: "description",
        ...COLUMN_WIDTH_CONFIG['note']
      },
      {
        title: '更新时间',
        key: 'updateTime',
        ...COLUMN_WIDTH_CONFIG['time']
      },
      {
        title: "来源",
        key: "from",
        ...COLUMN_WIDTH_CONFIG['type'],
        render: (row: OpsClusterInfo) => {
          return h(NTag, { type: row.from === DataFromEnum.AUTO ? 'success' : 'info' }, { default: () => row.from === DataFromEnum.AUTO ? '接口导入' : '手动添加' })
        }
      },
      {
        title: '操作',
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](3),
        render: (row: OpsClusterInfo) => {
          return h(NSpace, null, {
            default: () => [
              h(
                NTooltip,
                {},
                {
                  trigger: () =>
                    h(
                      NButton,
                      {
                        circle: true,
                        type: 'info',
                        size: 'small',
                        onClick: () => {
                          handleEdit(row)
                        }
                      },
                      {
                        icon: () => h(EditOutlined)
                      }
                    ),
                  default: () => '修改'
                }
              ),
              h(
                NPopconfirm,
                {
                  onPositiveClick: () => {
                    handleDelete(row)
                  }
                },
                {
                  trigger: () =>
                    h(
                      NTooltip,
                      {},
                      {
                        trigger: () =>
                          h(
                            NButton,
                            {
                              disabled: row.from === DataFromEnum.AUTO,
                              circle: true,
                              type: 'error',
                              size: 'small'
                            },
                            {
                              icon: () => h(DeleteOutlined)
                            }
                          ),
                        default: () => row.from === DataFromEnum.AUTO ? '导入不可删除' : '删除'
                      }
                    ),
                  default: () => '删除'
                }
              )
            ]
          })
        }
      }
    ]

  return projectColumns
}