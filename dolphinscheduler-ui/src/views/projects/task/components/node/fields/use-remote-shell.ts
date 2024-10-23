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
import { useCustomParams } from '.'
import type { IJsonItem } from '../types'
import { computed, vShow } from 'vue'

export function useRemoteShell(model: { [field: string]: any }): IJsonItem[] {
  const { t } = useI18n()

  return [
    {
      type: 'switch',
      field: 'enablePlatformSource',
      span: 8,
      name: '使用平台数据源',
      itemProps: {
        labelPlacement: "left",
      }
    },
    {
      type: 'input',
      field: 'platformSourceParamName',
      name: '动态数据源参数',
      span: 16,
      props: {
        placeholder: '默认为:${platform.node.node_source}',
        disabled: computed(() => { return !model.enablePlatformSource }),
      },
      itemProps: {
        labelPlacement: "left",
      }
    },
    {
      type: 'editor',
      field: 'rawScript',
      name: t('project.node.script'),
      validate: {
        trigger: ['input', 'trigger'],
        required: true,
        message: t('project.node.script_tips')
      }
    },
    ...useCustomParams({ model, field: 'localParams', isSimple: false })
  ]
}
