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
import { useTenant } from '@/views/projects/preference/components/use-tenant'
import { NAlert } from 'naive-ui'
import { computed, h } from 'vue'
import { useI18n } from 'vue-i18n'
import { useCustomParams, useResources } from '.'
import type { IJsonItem } from '../types'

export function useStorage(model: { [field: string]: any }): IJsonItem[] {

  const { t } = useI18n()

  const resourceItemsName = computed(() => {
    if (!model.resourceItems) {
      model.resourceItems = []
      return '请添加资源项'
    }
    model.resourceItems.forEach((item: any) => {
      if (!item.isSelected) {
        item.isSelected = hasFileContext(item)
      }
    })

    return '资源项 (' + model.resourceItems.length + ')'
  })

  return [
    {
      type: 'custom',
      field: 'custom-title-source',
      span: 22,
      widget: h(
        NAlert,
        {
          type: 'info',
          size: 'small',
        }, {
        default: () => resourceItemsName.value + ' 文件内容 优先级：脚本 >> 文件参数 >> 动态资源 >> 选中资源'
      }
      )
    },
    {
      type: 'custom-parameters',
      field: 'resourceItems',
      children: [
        {
          type: 'input',
          class: 'input-url-name',
          field: 'fileName',
          itemProps: {
            labelPlacement: "top",
          },
          name: '输出文件名',
          span: 22,
          props: {
            placeholder: '请输入输出的文件名，支持参数替换'
          },
        },
        {
          type: 'select',
          field: 'operMethod',
          span: 11,
          name: '文件是否上传',
          options: POSITIONS,
          itemProps: {
            labelPlacement: "top",
          },
          props: {
            placeholder: '脚本文件是否上传/删除'
          },
        },
        {
          type: 'select',
          field: 'parseMethod',
          span: 11,
          name: '文件解析方式',
          options: ParseMethods,
          itemProps: {
            labelPlacement: "top",
          },
          props: {
            placeholder: '是否参数替换，支持freemark以及简单参数替换'
          },
        },
        useResources(22, false, 1, undefined,
          {
            labelPlacement: "top",
          }),
        {
          type: 'input',
          class: 'input-url-name',
          field: 'dynamicResource',
          name: '资源路径',
          itemProps: {
            labelPlacement: "top",
          },
          span: 16,
          props: {
            placeholder: '手动输入资源路径 支持参数替换'
          },
        },
        useTenant(6, {
          labelPlacement: "top",
        }),
        (i = 0) => (
          {
            type: 'switch',
            field: 'isInputFileParam',
            span: 11,
            name: '使用InputFile参数',
            itemProps: {
              labelPlacement: "left",
            },
            path: `resourceItems.${i}.isInputFileParam`
          }),
        (i = 0) => (
          {
            type: 'input',
            field: 'inputFileParam',
            span: computed(() => {
              let item = model.resourceItems[i].isInputFileParam
              item = item || model.resourceItems[i].isInputFileParam
              return true == item ? 11 : 0
            }),
            name: 'InputFile参数名：',
            itemProps: {
              labelPlacement: "left",
            },
          }),
        (i = 0) => (
          {
            type: 'switch',
            field: 'isSelected',
            span: 22,
            name: '输入脚本/模板',
            itemProps: {
              labelPlacement: "left",
            },
            path: `resourceItems.${i}.isSelected`,
            props: {
              'on-update:value': (val: boolean) => {
                let hasContext = hasFileContext(model.resourceItems[i])
                if (hasContext && !val) {
                  const isConfirmed = confirm('是否清空输入的内容？')
                  if (isConfirmed) {
                    model.resourceItems[i].fileContext = ''
                  }
                }
                model.resourceItems[i].isSelected = val
              }
            },
          }),
        (i = 0) => (
          {
            type: 'editor',
            field: 'fileContext',
            span: computed(() => {
              let item = model.resourceItems[i].isSelected
              item = item || model.resourceItems[i].fileContext
              return true == item ? 22 : 0
            }),
            name: t('project.node.script'),
            itemProps: {
              labelPlacement: "top",
            },
            validate: {
              trigger: ['input', 'trigger'],
              required: false,
              message: t('project.node.script_tips')
            }
          }),
      ]
    },
    ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]

  function hasFileContext(item: any): any {
    return item.fileContext && item.fileContext.length > 0
  }
}

//NONE, UPLOAD_FORCE, UPLOAD, DELETE
const POSITIONS = [
  { label: 'None', value: 'NONE' },
  { label: 'Upload Force', value: 'UPLOAD_FORCE' },
  { label: 'Upload', value: 'UPLOAD' },
  { label: 'Delete', value: 'DELETE' }
]

const ParseMethods = [
  { label: 'None', value: 'NONE' },
  { label: 'Freemark', value: 'FREEMARK' },
  { label: 'Simple', value: 'SIMPLE' }
]
