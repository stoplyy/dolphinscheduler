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

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.utils.FileUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.resource.ResourceContext;
import org.apache.dolphinscheduler.plugin.task.api.resource.ResourceContext.ResourceItem;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;

import lombok.extern.slf4j.Slf4j;

/**
 * 配置资源任务
 */
@Slf4j
public class ResourceTask extends AbstractTask {

    /**
     * parameters
     */
    private ResourceTaskParameters resourceParameters;
    /**
     * taskExecutionContext
     */
    private TaskExecutionContext taskExecutionContext;

    private String tenant;
    private StorageOperate storageOperate;
    private Map<String, String> prepareParams;

    /**
     * constructor
     *
     * @param taskExecutionContext taskExecutionContext
     */
    public ResourceTask(TaskExecutionContext taskExecutionContext) {
        super(taskExecutionContext);
        this.taskExecutionContext = taskExecutionContext;
        this.tenant = taskExecutionContext.getResourceContext().getTenant();
        this.storageOperate = taskExecutionContext.getResourceContext().getStorageOperate();
    }

    @Override
    public void init() {
        resourceParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), ResourceTaskParameters.class);
        prepareParams = ParameterUtils.convert(taskExecutionContext.getPrepareParamsMap());

        log.info("Initialized resource task. \n params: \n{}\n prepareParams:\n{}",
                JSONUtils.toPrettyJsonString(resourceParameters), prepareParams);

        if (resourceParameters == null || !resourceParameters.checkParameters()) {
            throw new RuntimeException("http task params is not valid");
        }

        parseParameters();

        log.info("Initialized resource task. after parse context. \n params: \n{}\n prepareParams: \n{}",
                JSONUtils.toPrettyJsonString(resourceParameters), prepareParams);

    }

    private void parseParameters() {
        for (ResourceTaskParameters.ResourceTaskParameter parameter : resourceParameters.taskParameters()) {
            // 如果参数需要替换
            if (parameter.isParseContext()) {
                String template = parameter.getFileContext();
                if (StringUtils.isEmpty(template) && parameter.getResource() != null) {
                    template = readLocalFile(parameter.getResource().getResourceName());
                }
                if (StringUtils.isNotEmpty(template)) {
                    parameter.setFileContext(parseContent(template));
                }
            }
            if (StringUtils.isNotEmpty(parameter.getFileName())) {
                parameter.setFileName(parseContent(parameter.getFileName()));
            }
        }
    }

    public String readLocalFile(String fileName) {
        try {
            String localDownloadString = getLocalDownloadString(fileName);
            return FileUtils.readFile2Str(new FileInputStream(localDownloadString));
        } catch (IOException e) {
            log.error("read local file failed", e);
            throw new TaskException("read local file failed", e);
        }
    }

    public String getLocalDownloadString(String fileName) {
        return FileUtils.formatDownloadUpstreamLocalFullPath(taskExecutionContext.getExecutePath(), fileName);
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        try {
            // 生成的文件会以 文件名 为 key 存储在 resourceItemMap 中
            // 如果有 OUT FILE 参数，则参数名为key. Follow 文件参数传递到下一个任务

            /**
             * 资源文件两个来源：
             * 1. 当前任务通过content创建
             * content创建的需要保存到本地并添加到resourceItemMap
             * 2. 通过resource参数下载（下载的已存在本地）
             * 
             * content创建的需要保存到本地并添加到resourceItemMap
             * ** 如果是替换了内容
             * 
             */
            for (ResourceTaskParameters.ResourceTaskParameter parameter : resourceParameters.taskParameters()) {

                String dstFileName = parameter.getFileName();
                String localFilePath = getLocalDownloadString(dstFileName);

                // 1. 通过content创建的文件
                if (parameter.getResource() == null) {
                    // 与某个资源文件同名 删除这个本地文件 并log
                    ResourceItem item = taskExecutionContext.getResourceContext().getResourceItem(localFilePath);
                    if (item != null) {
                        // 删除本地文件
                        log.warn(
                                "will create a new file named {} but already exists with a resource file. {} resource file will be replaced in local",
                                localFilePath, item.getResourceAbsolutePathInStorage());
                        FileUtils.deleteFile(localFilePath);
                    }
                    // 写文件，并尝试添加到resourceItemMap
                    addLocalResourceWithCreateFile(parameter.getFileContext(), dstFileName, localFilePath);
                } else {
                    // 2. 通过resource参数下载的文件
                    // 2.2 本地文件不存在 本地文件名称改变
                    if (taskExecutionContext.getResourceContext().getResourceItem(localFilePath) == null) {
                        addLocalResourceWithCreateFile(parameter.getFileContext(), dstFileName, localFilePath);
                    } else {
                        // 2.1 本地文件存在，文件内容不一致
                        if (parameter.isParseContext()) {
                            log.warn("resource file {} already exists, will be replaced because content is parsed",
                                    localFilePath);
                            FileUtils.deleteFile(localFilePath);
                            addLocalResourceWithCreateFile(parameter.getFileContext(), dstFileName, localFilePath);
                        }
                    }
                }
                // 处理文件操作
                switch (parameter.getOperMethod()) {
                    case UPLOAD:
                    case UPLOAD_FORCE:
                    case DELETE:
                        storageOperate.upload(tenant, localFilePath, dstFileName,
                                parameter.getOperMethod() == OperMethod.DELETE,
                                parameter.getOperMethod() == OperMethod.UPLOAD_FORCE);
                        break;
                    default:
                        log.info("resource task method not support: {}", parameter.getOperMethod());
                        break;
                }
                // TODO: 处理文件参数
            }
        } catch (Exception e) {
            log.error("resource task failed", e);
            throw new TaskException("resource task failed", e);
        }
    }

    private void addLocalResourceWithCreateFile(String content, String dstFileName,
            String localFilePath) {
        FileUtils.writeContent2File(content, localFilePath);
        String resourceRelativePath = storageOperate.getResourceFileName(tenant, dstFileName);

        if (taskExecutionContext.getResourceContext().getResourceItem(localFilePath) == null) {
            ResourceContext.ResourceItem resourceItem = ResourceContext.ResourceItem.builder()
                    .resourceAbsolutePathInStorage(dstFileName)
                    .resourceRelativePath(resourceRelativePath)
                    .resourceAbsolutePathInLocal(localFilePath)
                    .build();
            taskExecutionContext.getResourceContext().addResourceItem(resourceItem);
        }
    }

    private String parseContent(String script) {
        Map<String, Property> paramsMap = taskExecutionContext.getPrepareParamsMap();
        return ParameterUtils.convertParameterPlaceholders(script, ParameterUtils.convert(paramsMap));
    }

    @Override
    public void cancel() throws TaskException {

    }

    @Override
    public AbstractParameters getParameters() {
        return resourceParameters;
    }

}
