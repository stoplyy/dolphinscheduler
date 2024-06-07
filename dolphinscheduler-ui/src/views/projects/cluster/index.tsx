import { defineComponent, ref } from 'vue'
import { NButton, NTable, NDialog, NForm, NFormItem, NInput, NDialogProvider } from 'naive-ui'
import { PlayCircleOutlined } from '@vicons/antd'

export default defineComponent({
  name: "clusterManager",
  setup() {
    const tableData = ref([]) // 你的集群列表数据
    const showDialog = ref(false) // 控制弹窗的显示和隐藏
    const formValue = ref({}) // 弹窗中的表单数据

    // 获取集群列表数据的函数
    const fetchTableData = async () => {
      // 调用后端接口获取数据，并赋值给 tableData
    }

    // 添加或修改集群的函数
    const handleAddOrEdit = async () => {
      // 调用后端接口添加或修改集群
      showDialog.value = false // 关闭弹窗
      await fetchTableData() // 刷新表格数据
    }

    return {
      tableData,
      showDialog,
      formValue,
      fetchTableData,
      handleAddOrEdit,
    }
  },
  render() {
    return <NDialogProvider>
      <NButton onClick={this.fetchTableData}>刷新</NButton>
      <NButton onClick={() => { this.showDialog = true }}>新增</NButton>
      <NTable data={this.tableData}>
        {/* 你的表格列 */}
        <NTable.Column title="操作">
          {{
            default: ({ row }) => <>
              <NButton onClick={() => { this.formValue = row; this.showDialog = true }}>修改</NButton>
              <NButton onClick={() => { /* 调用后端接口删除集群 */ }}>删除</NButton>
            </>
          }}
        </NTable.Column>
      </NTable>
      <NDialog visible={this.showDialog} onUpdate:visible={v => { this.showDialog = v }}>
        <NForm model={this.formValue}>
          {/* 你的表单项 */}
          <NFormItem label="集群名称">
            <NInput v-model={this.formValue.name} />
          </NFormItem>
          {/* 其他表单项 */}
        </NForm>
        <NButton onClick={this.handleAddOrEdit}>确定</NButton>
      </NDialog>
    </NDialogProvider>
  }
})