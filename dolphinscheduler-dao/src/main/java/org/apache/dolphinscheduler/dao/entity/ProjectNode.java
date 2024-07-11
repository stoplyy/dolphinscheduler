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

package org.apache.dolphinscheduler.dao.entity;

import java.util.Date;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("t_ds_project_node")
public class ProjectNode {

    @TableId(value = "id", type = IdType.AUTO)
    private Integer id;
    /**
     * user id
     */
    @TableField("user_id")
    private Integer userId;

    /**
     * user name
     */
    @TableField(exist = false)
    private String userName;

    @TableField("project_code")
    private Long projectCode;

    @TableField("cluster_id")
    private String clusterId;

    @TableField("cluster_code")
    private Integer clusterCode;

    @TableField("data_source_code")
    private Long dataSourceCode;

    @TableField("node_id")
    private String nodeId;

    @TableField("node_key")
    private String nodeKey;

    @TableField("node_name")
    private String nodeName;

    /*
     * auto/manual
     */
    @TableField("data_from")
    private String dataFrom;

    @TableField(exist = false)
    private String from;

    public void setFrom(String from) {
        this.from = dataFrom;
        this.dataFrom = from;
    }

    public String getFrom() {
        return dataFrom;
    }

    /**
     * project description
     */
    @TableField("des")
    private String description;

    /**
     * create time
     */
    private Date createTime;

    /**
     * update time
     */
    private Date updateTime;
}
