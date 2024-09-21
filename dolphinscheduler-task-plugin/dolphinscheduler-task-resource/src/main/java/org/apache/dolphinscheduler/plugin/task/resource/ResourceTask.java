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

import java.io.FileInputStream;
import java.io.IOException;
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
import org.apache.dolphinscheduler.plugin.task.api.resource.ResourceContext.ResourceItem;
import org.apache.dolphinscheduler.plugin.task.api.utils.ParameterUtils;

import freemarker.template.TemplateException;
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
    private Map<String, ResourceItem> resourceItemMap;

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
        resourceItemMap = taskExecutionContext.getResourceContext().getResourceItemMap();

        log.info("Initialized resource task. \n params: \n{}\n prepareParams:\n{} resoureItemKeys:\n{}",
                JSONUtils.toPrettyJsonString(resourceParameters),
                prepareParams,
                resourceItemMap == null ? "" : JSONUtils.toPrettyJsonString(resourceItemMap.keySet()));

        if (resourceParameters == null || !resourceParameters.checkParameters()) {
            throw new RuntimeException("http task params is not valid");
        }

        parseParameters();

        log.info("Initialized resource task. after parse context. \n params: \n{}\n prepareParams: \n{}",
                JSONUtils.toPrettyJsonString(resourceParameters), prepareParams);

    }

    private void parseParameters() {
        for (ResourceTaskParameter parameter : resourceParameters.getResourceItems()) {
            // 1. file name parse with prepare params
            if (StringUtils.isEmpty(parameter.getFileName()) && parameter.getResource() != null) {
                parameter.setFileName(parameter.getResource());
            }
            if (StringUtils.isNotEmpty(parameter.getFileName())) {
                parameter.setFileName(parseContent(parameter.getFileName()));
            }
            // 2. set file context
            if (StringUtils.isEmpty(parameter.getFileContext()) && parameter.getResource() != null) {
                parameter.setFileContext(readLocalFile(parameter.getResource()));
            }

            // 3. if parse context, parse content with create a new file
            String template = parameter.getFileContext();
            if (StringUtils.isNotEmpty(template)) {
                String parsedContent = template;
                switch (parameter.getParseMethod()) {
                    case NONE:
                        break;
                    case FREEMARK:
                        parsedContent = paseContentWithFreeMark(template, prepareParams);
                        break;
                    case SIMPLE:
                        parsedContent = parseContent(template);
                        break;
                    default:
                        break;
                }
                parameter.setFileContext(parsedContent);
            }

            // 4. add local resource with create file
            if (StringUtils.isEmpty(parameter.getFileContext())) {
                log.warn("file context is empty, skip this parameter: {}", parameter);
            } else {
                String localNewFilePath = getLocalDownloadString(parameter.getFileName());
                String storeFileName = parameter.getFileName();
                // store file name with out suffix.local file name with suffix
                FileUtils.writeContent2File(parameter.getFileContext(), localNewFilePath);
                log.info("add local resource with create file: {}. store file name:{}", localNewFilePath,
                        storeFileName);
            }
        }
    }

    public String readLocalFile(String fileName) {
        ResourceItem item = null;
        try {
            item = resourceItemMap.getOrDefault(fileName, null);
            if (item == null) {
                throw new TaskException("resource file not exist in resours item map. file name: " + fileName);
            }
            return FileUtils.readFile2Str(new FileInputStream(item.getResourceAbsolutePathInLocal()));
        } catch (IOException e) {
            String msg = String.format("read local file error: %s",
                    item == null ? fileName : item.getResourceAbsolutePathInLocal());
            log.error(msg, e);

            throw new TaskException(msg, e);
        }
    }

    public String getLocalDownloadString(String fileName) {
        return FileUtils.formatDownloadUpstreamLocalFullPath(taskExecutionContext.getExecutePath(), fileName);
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        try {
            for (ResourceTaskParameter parameter : resourceParameters.getResourceItems()) {

                String storeFileName = parameter.getFileName();
                String localFilePath = getLocalDownloadString(storeFileName);

                // 处理文件操作
                switch (parameter.getOperMethod()) {
                    case UPLOAD:
                    case UPLOAD_FORCE:
                    case DELETE:
                        storageOperate.upload(tenant, localFilePath, storeFileName,
                                parameter.getOperMethod() == OperMethod.DELETE,
                                parameter.getOperMethod() == OperMethod.UPLOAD_FORCE);
                        break;
                    default:
                        log.info("resource task method not support: {}", parameter.getOperMethod());
                        break;
                }
            }

            exitStatusCode = 0;
        } catch (Exception e) {
            log.error("resource task failed", e);
            exitStatusCode = -1;
            throw new TaskException("resource task failed", e);
        }
    }

    private String paseContentWithFreeMark(String content, Map<String, String> params) {
        try {
            return FreemarkHelper.processTemplate(content, params);
        } catch (IOException | TemplateException e) {
            String msg = "freemark convert exception！source: \n" + content + " \ninput params:"
                    + JSONUtils.toJsonString(params);
            log.error(msg, e);
            throw new TaskException("freemark convert exception！");
        } catch (Exception e) {
            throw new TaskException("task content error");
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
