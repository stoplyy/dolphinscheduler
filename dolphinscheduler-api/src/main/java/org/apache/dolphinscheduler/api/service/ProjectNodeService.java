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

package org.apache.dolphinscheduler.api.service;

import java.util.List;

import org.apache.dolphinscheduler.api.utils.Result;
import org.apache.dolphinscheduler.dao.entity.ProjectNode;
import org.apache.dolphinscheduler.dao.entity.User;

public interface ProjectNodeService {

    /**
     * 同步平台的节点数据（新增、删除）
     * 
     * @param loginUser
     * @param projectCode
     * @param clusterCode
     * @return
     */
    Result<Boolean> syncNodeFromPlatform(User loginUser, long projectCode, Integer clusterCode);

    Result<ProjectNode> createNode(User loginUser, long projectCode, Integer clusterCode, String nodeName,
            String nodeKey, String nodeId, String from, String desc);

    Result<List<ProjectNode>> queryList(User loginUser, long projectCode, Integer clusterCode);

    /**
     * delete project by code
     *
     * @param loginUser   login user
     * @param projectCode project code
     * @return delete result code
     */
    Result<Boolean> delete(User loginUser, long projectCode, Integer clusterCode, Integer code);

    /**
     * updateProcessInstance project
     *
     * @param loginUser login user
     * @return update result code
     */
    Result<ProjectNode> update(User loginUser, long projectCode, Integer code, String nodeName, String nodeKey,
            String nodeId, String desc);

}
