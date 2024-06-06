
import { defineComponent } from 'vue'
import { NButton } from 'naive-ui'

export default defineComponent({
  name: "clusterManager",
  render() {
    return <NButton>{{ default: () => 'Star Kirby kitty' }}</NButton>
  }
})
