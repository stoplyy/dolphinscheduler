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
import { useI18n } from 'vue-i18n'
import { computed, h, ref, vShow } from 'vue'
import { useCustomParams, useResources } from '.'
import type { IJsonItem } from '../types'
import screenfull from 'screenfull'
import { validate } from 'webpack'
import { DeleteOutlined, EditOutlined } from '@vicons/antd'
import { NAlert, NButton, NIcon } from 'naive-ui'

export function useStorage(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  const resourceItemsName = computed(() => {
    return '资源项 (' + model.resourceItems.length + ')'
  })
  const isFullscreen = ref(false)
  const closeInfo = ref(false)
  return [
    {
      type: 'custom',
      field: 'custom-title-source',
      span: 20,
      widget: h(
        NAlert,
        {
          type: 'info',
          size: 'small',
          vShow: !closeInfo
        }, {
        default: () => '文件内容：资源文件或者输入内容. 二选一，优先输入的内容',
        //3s后自动关闭
        trigger: () => setTimeout(() => {
          //关闭当前提示
          closeInfo.value = true
        })
      }
      )
    },
    {
      type: 'custom-parameters',
      field: 'resourceItems',
      name: resourceItemsName.value,
      children: [{
        type: 'input',
        class: 'input-url-name',
        field: 'fileName',
        name: '文件名',
        span: 20,
        props: {
          placeholder: '请输入文件名'
        },
      },
      {
        type: 'select',
        field: 'operMethod',
        span: 10,
        name: '操作方式',
        options: POSITIONS,
        props: {
          placeholder: '脚本文件是否上传/删除'
        },
      },
      {
        type: 'select',
        field: 'parseMethod',
        span: 10,
        name: '解析方式',
        options: ParseMethods,
        props: {
          placeholder: '是否参数替换，支持freemark以及简单参数替换'
        },
      },
      // '文件内容：资源文件 或者 输入内容，二选一，优先输入的文件内容'
      useResources(20, false, 1),
      {
        type: 'editor',
        field: 'fileContext',
        span: 20,
        name: t('project.node.script'),
        validate: {
          trigger: ['input', 'trigger'],
          required: false,
          message: t('project.node.script_tips')
        }
      }
      ]
    },
    ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]
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
