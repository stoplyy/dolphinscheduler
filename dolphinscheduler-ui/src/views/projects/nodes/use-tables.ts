
import { h } from "vue";
import { NButton, NTag } from "naive-ui";

export function useTable() {

    const columns = [
        {
            title: '集群名称',
            key: 'clusterName'
        },
        {
            title: '集群ID',
            key: 'clusterId'
        },
        {
            title: "Tags",
            key: "tags"
        },
        {
            title: "Action",
            key: "actions"
        }
    ]
    return {
        columns
    }
}
