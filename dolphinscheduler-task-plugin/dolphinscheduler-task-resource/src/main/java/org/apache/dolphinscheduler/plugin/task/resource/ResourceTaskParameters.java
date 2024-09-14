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

package org.apache.dolphinscheduler.plugin.task.resource;

import java.util.ArrayList;
import java.util.List;

import org.apache.dolphinscheduler.plugin.task.api.model.ResourceInfo;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.springframework.lang.Nullable;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * http parameter
 */

public class ResourceTaskParameters extends AbstractParameters {

    @Override
    public boolean checkParameters() {
        return taskParameters != null;
    }

    /**
     * get resource files list need download
     *
     * @return resource files list
     */
    @Override
    public List<ResourceInfo> getResourceFilesList() {
        List<ResourceInfo> resourceInfoList = new ArrayList<>();
        if (taskParameters != null) {
            for (ResourceTaskParameter taskParameter : taskParameters) {
                // 返回需要下载的资源文件列表，如果是操作删除的资源文件，则不返回
                if (taskParameter.getResource() != null
                        && taskParameter.operMethod != OperMethod.DELETE) {
                    resourceInfoList.add(taskParameter.getResource());
                }
            }
        }
        return resourceInfoList;
    }

    List<ResourceTaskParameter> taskParameters = new ArrayList<>();

    public List<ResourceTaskParameter> taskParameters() {
        return taskParameters;
    }

    public void setTaskParameters(List<ResourceTaskParameter> resourceParameters) {
        this.taskParameters = resourceParameters;
    }

    @Data
    @Builder
    @AllArgsConstructor
    @NoArgsConstructor
    public static class ResourceTaskParameter {

        /**
         * 文件模板 （支持参数替换）
         */
        private String fileContext;

        /** 是否用 参数替换 文件内容 */
        private boolean isParseContext;

        /**
         * 关联的资源文件
         *
         */
        @Nullable
        private ResourceInfo resource;

        /**
         * 文件名称 支持参数替换
         * 上传文件时，文件名称 包含相对路径 a/b/file.txt
         * 下载文件时，文件名称 包含相对路径 a/b/file.txt (与resourceInfo中的文件名称一致)
         * 删除文件时，文件名称 包含相对路径 a/b/file.txt (与resourceInfo中的文件名称一致)
         */
        private String fileName;

        /**
         * NONE: 无操作
         * DOWNLOAD: 下载
         * DELETE: 删除
         * UPLOAD: 上传
         */
        private OperMethod operMethod;
    }

}
