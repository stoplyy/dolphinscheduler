import { defineComponent, h, ref } from 'vue'
import { NCascader, NTooltip, NText } from 'naive-ui'
import Modal from '@/components/modal'
import type { CascaderOption } from 'naive-ui'
import { ProjectNode } from '@/service/modules/project-platform/platform'

export default defineComponent({
  name: 'SourceModal',
  props: {
    show: {
      type: Boolean,
      required: true
    },
    projectCode: {
      type: String,
      required: true
    },
    selectedIds: {
      type: Array as () => string[],
      required: true
    },
    renderProjectSourcesOptions: {
      type: Function,
      required: true
    },
    onUpdateValue: {
      type: Function,
      required: true
    },
    onCancel: {
      type: Function,
      required: true
    },
    onConfirm: {
      type: Function,
      required: true
    }
  },
  setup(props) {
    const loadingSource = ref(false)

    return () => (
      <Modal
        show={props.show}
        title='选择项目源'
        onCancel={props.onCancel}
        onConfirm={props.onConfirm}
      >
        <NCascader
          multiple
          clearable
          onLoad={() => loadingSource.value}
          checkStrategy='child'
          value={props.selectedIds}
          filterable
          maxTagCount={5}
          themeOverrides={{ columnWidth: '400px', optionFontSize: '13px' }}
          renderLabel={(option: CascaderOption, checked: boolean) => (
            <NTooltip placement='right-end' trigger='hover'>
              {{
                trigger: () => (
                  <NText
                    type={checked ? 'primary' : 'default'}
                    style={{ minWidth: '400px' }}
                  >
                    {option.label}
                  </NText>
                ),
                default: () => {
                  {
                    if (option.disabled) {
                      return `${option.label} 节点未关联源，不可选择！`
                    } else {
                      let it = option.key as ProjectNode
                      return h(
                        'div',
                        { style: 'min-width:200px; padding:4px;' },
                        [
                          h('div', {}, [
                            h('b', {}, 'source_code: '),
                            it.dataSourceCode
                          ]),
                          h('div', {}, [h('b', {}, 'node_code: '), it.id]),
                          h('div', {}, [
                            h('b', {}, 'node_name: '),
                            option.label
                          ]),
                          h('div', {}, [h('b', {}, 'node_key: '), it.nodeKey])
                        ]
                      )
                    }
                  }
                }
              }}
            </NTooltip>
          )}
          options={props.renderProjectSourcesOptions(props.projectCode)}
          onUpdateValue={props.onUpdateValue}
          placeholder='Load Source from project'
        />
      </Modal>
    )
  }
})
