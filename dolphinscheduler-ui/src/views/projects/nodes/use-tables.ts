import { h, reactive, vShow } from "vue";
import { NButton, NTag, NSpace, NTooltip, NPopconfirm } from "naive-ui";
import { OpsNodeInfo } from "./types";
import { DeleteOutlined, DisconnectOutlined, LoadingOutlined, EditOutlined, SyncOutlined } from '@vicons/antd'
import {
  COLUMN_WIDTH_CONFIG
} from '@/common/column-width-config'
import { DataFromEnum } from "../cluster/types";

export type operRow = (row: OpsNodeInfo) => void

export function useNodeTable(handleEdit: operRow, handleDelete: operRow, handleSync: operRow, handleSyncSourceLogic: operRow) {

  const hasSync = (row: OpsNodeInfo) => {
    return (row.id && row.id > 0) as boolean;
  }

  const notSyncDataSource = (row: OpsNodeInfo) => {
    return (row.dataSourceCode === null || row.dataSourceCode === undefined)
  }

  const nodeColumns =
    [
      {
        title: '#',
        key: 'id-index',
        ...COLUMN_WIDTH_CONFIG['index'],
        render: (row: any, index: number) => index + 1
      },
      {
        title: 'ID',
        key: 'nodeId',
        ...COLUMN_WIDTH_CONFIG['version']
      },
      {
        title: 'Key',
        key: 'nodeKey',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: 'Name',
        key: 'nodeName',
        ...COLUMN_WIDTH_CONFIG['name']
      },
      {
        title: "来源",
        key: "from",
        ...COLUMN_WIDTH_CONFIG['type'],
        render: (row: OpsNodeInfo) => {
          return h(NTag, { type: row.from === DataFromEnum.MANUAL ? 'info' : 'success' }, {
            default: () => row.from === DataFromEnum.MANUAL ? '手动添加' : (row.from + '导入')
          })
        }
      },
      // {
      //   title: "Tags",
      //   key: "tags",
      //   ...COLUMN_WIDTH_CONFIG['note']
      // },
      {
        title: "描述",
        key: "description",
        ...COLUMN_WIDTH_CONFIG['note']
      },
      // {
      //   title: '更新时间',
      //   key: 'updateTime',
      //   ...COLUMN_WIDTH_CONFIG['time']
      // },
      {
        title: '操作',
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](3),
        render: (row: OpsNodeInfo) => {
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
                        disabled: !hasSync(row),
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
                  default: () => !hasSync(row) ? '未落库不可编辑' : '编辑'
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
                              disabled: !(hasSync(row)),
                              circle: true,
                              type: 'error',
                              size: 'small'
                            },
                            {
                              icon: () => h(DeleteOutlined)
                            }
                          ),
                        default: () => hasSync(row) ? '删除' : '不可删除'
                      }
                    ),
                  default: () => '删除'
                }
              ),
              h(
                NPopconfirm,
                {
                  onPositiveClick: () => {
                    handleSyncSourceLogic(row)
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
                              disabled: !notSyncDataSource(row),
                              circle: true,
                              type: calculateButtonColor(row),
                              size: 'small'
                            },
                            {
                              icon: () => h(DisconnectOutlined)
                            }
                          ),
                        default: () => {
                          if (row.dataSourceCode === undefined || row.dataSourceCode === null) {
                            return '创建源(点击创建源）'
                          } else if (row.isConnected === undefined || row.isConnected === null) {
                            return "源联通状态未知（请进行连接测试）"
                          } else {
                            return row.isConnected ? '源已连通' : ('无法连接，请确认源是否正常！' + row.dataSourceCode)
                          }
                        }
                      }
                    ),
                  default: () => '创建源'
                }
              )
            ]
          })
        }
      }
    ]

  return nodeColumns

  function calculateButtonColor(row: OpsNodeInfo) {
    if (row.dataSourceCode === undefined || row.dataSourceCode === null) {
      return 'info'
    }
    return (row.isConnected === undefined || row.isConnected === null) ? 'warning' : (row.isConnected ? 'success' : "error");
  }
}