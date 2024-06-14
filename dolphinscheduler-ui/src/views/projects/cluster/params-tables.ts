import {
  COLUMN_WIDTH_CONFIG
} from '@/common/column-width-config';
import { CommonParameter } from "@/service/modules/project-platform/platform";
import { DeleteOutlined, EditOutlined } from '@vicons/antd';
import { NButton, NPopconfirm, NSpace, NTooltip } from "naive-ui";
import { h } from "vue";
import type { DataTableRowOper } from './types';

export function useParamsTable(object: DataTableRowOper) {

  const handleEdit = <T extends CommonParameter>(row: T) => {
    object.editLogic(row);
  }

  const handleDelete = <T extends CommonParameter>(row: T) => {
    object.deleteLogic(row);
  }

  const columns =
    [
      {
        title: '#',
        key: 'id-index',
        ...COLUMN_WIDTH_CONFIG['index'],
        render: (row: any, index: number) => index + 1
      },
      {
        title: 'Name',
        key: 'paramName',
        ...COLUMN_WIDTH_CONFIG['type']
      },
      {
        title: 'Value',
        key: 'paramValue',
        ...COLUMN_WIDTH_CONFIG['type']
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
        title: '操作',
        key: 'operation',
        ...COLUMN_WIDTH_CONFIG['operation'](3),
        render: (row: CommonParameter) => {
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
                              circle: true,
                              type: 'error',
                              size: 'small'
                            },
                            {
                              icon: () => h(DeleteOutlined)
                            }
                          ),
                        default: () => '删除'
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

  return columns
}