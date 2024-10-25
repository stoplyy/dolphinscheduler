import { defineComponent, ref } from 'vue'
import { NCascader, NTooltip, NText } from 'naive-ui'
import Modal from '@/components/modal'
import type { CascaderOption } from 'naive-ui'

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
        title="选择项目源"
        onCancel={props.onCancel}
        onConfirm={props.onConfirm}
      >
        <NCascader
          multiple
          clearable
          onLoad={() => loadingSource.value}
          checkStrategy="child"
          value={props.selectedIds}
          filterable
          maxTagCount={5}
          themeOverrides={{ columnWidth: '400px', optionFontSize: '13px' }}
          renderLabel={(option: CascaderOption, checked: boolean) => (
            <NTooltip>
              {{
                trigger: () => (
                  <NText
                    type={checked ? 'primary' : 'default'}
                    style={{ minWidth: '400px' }}
                  >
                    {option.label}
                  </NText>
                ),
                default: () =>
                  option.disabled
                    ? `${option.label} 节点未关联源，不可选择！`
                    : `${option.value}:${option.label}`
              }}
            </NTooltip>
          )}
          options={props.renderProjectSourcesOptions(props.projectCode)}
          onUpdateValue={props.onUpdateValue}
          placeholder="Load Source from project"
        />
      </Modal>
    )
  }
})
