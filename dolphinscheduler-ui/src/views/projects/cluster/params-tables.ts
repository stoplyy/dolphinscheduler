import {
  COLUMN_WIDTH_CONFIG
} from '@/common/column-width-config';
import { CommonParameter } from "@/service/modules/project-platform/platform";
import { DeleteOutlined, EditOutlined } from '@vicons/antd';
import { NButton, NEllipsis, NInput, NPopconfirm, NSpace, NTag, NTooltip } from "naive-ui";
import { h } from "vue";
import { DataFromEnum, type DataTableRowOper } from './types';

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
        sorter: (rowA: CommonParameter, rowB: CommonParameter) => rowA.paramName.localeCompare(rowB.paramName),
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
        ...COLUMN_WIDTH_CONFIG['note'],
        render: (row: CommonParameter) => {
          return row.from === DataFromEnum.AUTO ?
            h(NTag, { type: 'warning' }, { default: '导入参数' }) :
            h(NEllipsis, { lineClamp: 1 }, { default: row.description })
        }
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
                        disabled: row.from === DataFromEnum.AUTO,
                        onClick: () => {
                          handleEdit(row)
                        }
                      },
                      {
                        icon: () => h(EditOutlined)
                      }
                    ),
                  default: () => row.from === DataFromEnum.AUTO ? '导入不可修改' : '修改'
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
                              disabled: row.from === DataFromEnum.AUTO,
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
  
  return columns
}