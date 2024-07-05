import { h, reactive, vShow } from "vue";
import { NButton, NTag, NSpace, NTooltip, NPopconfirm } from "naive-ui";
import type { OpsNodeInfo, DataTableRowOper } from "./types";
import { DeleteOutlined, EditOutlined, SyncOutlined } from '@vicons/antd'
import {
  COLUMN_WIDTH_CONFIG
} from '@/common/column-width-config'
import { DataFromEnum } from "../cluster/types";

export function useNodeTable(object: DataTableRowOper) {

  const handleEdit = (row: OpsNodeInfo) => {
    object.editLogic(row);
  }

  const handleDelete = (row: OpsNodeInfo) => {
    object.deleteLogic(row);
  }

  const handleSync = (row: OpsNodeInfo) => {
    object.syncLogic(row);
  }

  const hasSync = (row: OpsNodeInfo) => {
    return (row.id && row.id > 0) as boolean;
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
        ...COLUMN_WIDTH_CONFIG['index']
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
      {
        title: '更新时间',
        key: 'updateTime',
        ...COLUMN_WIDTH_CONFIG['time']
      },
      {
        title: "来源",
        key: "from",
        ...COLUMN_WIDTH_CONFIG['type'],
        render: (row: OpsNodeInfo) => {
          return h(NTag, { type: row.from === DataFromEnum.AUTO ? 'success' : 'info' }, {
            default: () => row.from === DataFromEnum.AUTO ? '接口导入' : '手动添加'
          })
        }
      },
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
                    handleSync(row)
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
                              disabled: hasSync(row),
                              circle: true,
                              type: 'success',
                              size: 'small'
                            },
                            {
                              icon: () => h(SyncOutlined)
                            }
                          ),
                        default: () => hasSync(row) ? '已同步' : '同步(同步到当前系统)'
                      }
                    ),
                  default: () => '同步'
                }
              )
            ]
          })
        }
      }
    ]

  return nodeColumns
}