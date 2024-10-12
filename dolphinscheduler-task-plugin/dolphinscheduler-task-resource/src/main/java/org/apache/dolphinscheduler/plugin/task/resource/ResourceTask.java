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
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.utils.FileUtils;
import org.apache.dolphinscheduler.common.utils.JSONUtils;
import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;
import org.apache.dolphinscheduler.plugin.task.api.AbstractTask;
import org.apache.dolphinscheduler.plugin.task.api.TaskCallBack;
import org.apache.dolphinscheduler.plugin.task.api.TaskException;
import org.apache.dolphinscheduler.plugin.task.api.TaskExecutionContext;
import org.apache.dolphinscheduler.plugin.task.api.enums.DataType;
import org.apache.dolphinscheduler.plugin.task.api.enums.Direct;
import org.apache.dolphinscheduler.plugin.task.api.model.Property;
import org.apache.dolphinscheduler.plugin.task.api.parameters.AbstractParameters;
import org.apache.dolphinscheduler.plugin.task.api.resource.ResourceContext;
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
        this.storageOperate = taskExecutionContext.getResourceContext().getStorageOperate();
    }

    @Override
    public void init() {
        resourceParameters = JSONUtils.parseObject(taskExecutionContext.getTaskParams(), ResourceTaskParameters.class);
        prepareParams = ParameterUtils.convert(taskExecutionContext.getPrepareParamsMap());
        resourceItemMap = taskExecutionContext.getResourceContext().getResourceItemMap();

        log.info("Initialized resource task. \n===params===\n{}\n===prepareParams===:\n{} \n===resoureItemKeys===\n{}",
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
            final boolean NEED_PARSE_CONTENT = parameter.needParse();

            if (StringUtils.isEmpty(parameter.getTenant())) {
                parameter.setTenant(taskExecutionContext.getTenantCode());
            }

            // 1. file name parse with prepare params
            // 1.1 default file name
            String dstFileName = parseDstFileName(parameter);
            if(dstFileName.startsWith(Constants.FOLDER_SEPARATOR)){ {
                dstFileName = dstFileName.substring(1);
                log.info("remove the first character '/' of the file name: {}", dstFileName);
            }

            log.info("parse file name: {} -> {}", parameter.getFileName(), dstFileName);
            parameter.setFileName(dstFileName);

            if (StringUtils.isNotEmpty(parameter.getFileContext())) {
                // 2.1 if file context, set source file with file context
                String sourceLocalAbsoluteFile = reWriteToLocalWithFileName(parameter);
                log.info("file context is not empty, save file to: {}", sourceLocalAbsoluteFile);
                parameter.setSourceLocalAbsoluteFile(sourceLocalAbsoluteFile);
            } else {
                // 2.1 if parse context, load source file
                String sourceLocalAbsoluteFile = loadSourceAbsluteFile(parameter);
                if (StringUtils.isEmpty(sourceLocalAbsoluteFile)) {
                    log.warn("file context and source file is empty, skip this parameter: {}", parameter);
                    continue;
                }

                log.info("file context is empty, load source file from: {}", sourceLocalAbsoluteFile);
                parameter.setSourceLocalAbsoluteFile(sourceLocalAbsoluteFile);
                if (NEED_PARSE_CONTENT) {
                    // need parse content, load file content with source file
                    parameter.setFileContext(readLocalFileWithAbsolutePath(sourceLocalAbsoluteFile));
                }
            }
        }
    }

    private String getParsedContent(ResourceTaskParameter parameter) {
        String parsedContent = parameter.getFileContext();
        if (StringUtils.isEmpty(parsedContent)) {
            log.warn("file context is empty, skip parse step, parameter: {}", parameter);
        } else {
            switch (parameter.getParseMethod()) {
                case NONE:
                    break;
                case FREEMARK:
                    parsedContent = paseContentWithFreeMark(parsedContent, prepareParams);
                    break;
                case SIMPLE:
                    parsedContent = parseContent(parsedContent);
                    break;
                default:
                    break;
            }
        }
        return parsedContent;
    }

    private String parseDstFileName(ResourceTaskParameter parameter) {
        String dstFileNameTmp = parameter.getFileName();

        if (StringUtils.isEmpty(parameter.getFileName())
                && StringUtils.isNotEmpty(parameter.getDynamicResource())) {
            dstFileNameTmp = parameter.getDynamicResource();
        }
        if (StringUtils.isEmpty(dstFileNameTmp)
                && StringUtils.isNotEmpty(parameter.getResource())) {
            dstFileNameTmp = parameter.getResource();
        }

        if (StringUtils.isNotEmpty(dstFileNameTmp)) {
            dstFileNameTmp = parseContent(dstFileNameTmp);
        }
        return dstFileNameTmp;
    }

    private String loadSourceAbsluteFile(ResourceTaskParameter parameter) {
        String sourceLocalAbsoluteFile = null;
        // 2.1 set with input file param
        if (StringUtils.isNotEmpty(parameter.getInputFileParam())) {
            String fileParamName = parameter.getInputFileParam();
            Property property = resourceParameters.getLocalParametersMap().getOrDefault(fileParamName, null);
            if (property == null) {
                throw new TaskException(
                        "input file param not exist in local params. param name: " + fileParamName);
            }
            if (property.getDirect() != Direct.IN || property.getType() != DataType.FILE) {
                throw new TaskException(
                        "local param is not a [input] [file] param. param name: " + fileParamName);
            }
            sourceLocalAbsoluteFile = getLocalDownloadString(fileParamName);
        } else if (StringUtils.isNotEmpty(parameter.getDynamicResource())) {
            // 2.2 set with dynamic resource
            final String dynamicResourceName = parseContent(parameter.getDynamicResource());
            // set resource name
            parameter.setResource(dynamicResourceName);
            // download resource to local and add resource item
            sourceLocalAbsoluteFile = downloadResourceWithAddResource(parameter.getTenant(),
                    dynamicResourceName);
            // if dynamic resource name is not equal to file name, copy file to new file
            // enable output file exist
            if (!dynamicResourceName.equals(parameter.getFileName())) {
                String localNewFilePath = getLocalDownloadString(parameter.getFileName());
                copyFileToNew(sourceLocalAbsoluteFile, localNewFilePath);
            }
        } else if (StringUtils.isNotEmpty(parameter.getResource())) {
            // 2.3 set with resource
            ResourceItem resourceItem = resourceItemMap.get(parameter.getResource());
            if (resourceItem == null) {
                throw new TaskException("resource item not exist in resource context. resource name: "
                        + parameter.getResource());
            }
            sourceLocalAbsoluteFile = resourceItem.getResourceAbsolutePathInLocal();
        }
        return sourceLocalAbsoluteFile;
    }

    private void copyFileToNew(String srcFilePath, String dstFilePath) {
        try {
            String context = readLocalFileWithAbsolutePath(srcFilePath);
            FileUtils.writeContent2File(context, dstFilePath);
        } catch (Exception e) {
            String msg = String.format("copy file error: %s -> %s", srcFilePath, dstFilePath);
            log.error(msg, e);

            throw new TaskException(msg, e);
        }
    }

    private String reWriteToLocalWithFileName(ResourceTaskParameter parameter) {
        String storeFileName = parameter.getFileName();
        String localNewFilePath = getLocalDownloadString(parameter.getFileName());

        if (StringUtils.isEmpty(parameter.getFileContext())) {
            log.warn("file context is empty, skip this parameter: {}", parameter);
        } else {
            // create file with file context. if file exist, override it.
            FileUtils.writeContent2File(parameter.getFileContext(), localNewFilePath);
            log.info("add local resource with create file: {}. store file name:{}", localNewFilePath,
                    storeFileName);
        }
        return localNewFilePath;
    }

    private String readLocalFileWithAbsolutePath(String fileAbsolutePathInLocal) {
        try {
            return FileUtils.readFile2Str(new FileInputStream(fileAbsolutePathInLocal));
        } catch (IOException e) {
            String msg = String.format("read local file error: %s", fileAbsolutePathInLocal);
            log.error(msg, e);

            throw new TaskException(msg, e);
        }
    }

    private String downloadResourceWithAddResource(String tenant, String resourceAbsolutePathInStorage) {
        String resourceRelativePath = storageOperate.getResourceFileName(tenant, resourceAbsolutePathInStorage);
        String resourceAbsolutePathInLocal = Paths.get(taskExecutionContext.getExecutePath(), resourceRelativePath)
                .toString();
        File file = new File(resourceAbsolutePathInLocal);
        if (!file.exists()) {
            try {
                String remoteFullPath = resourceAbsolutePathInStorage.startsWith(Constants.FOLDER_SEPARATOR)
                        ? resourceAbsolutePathInStorage
                        : storageOperate.getResDir(tenant) + resourceAbsolutePathInStorage;
                storageOperate.download(remoteFullPath, resourceAbsolutePathInLocal, true);
                log.info("Download resource file {} under: {} successfully", remoteFullPath,
                        resourceAbsolutePathInLocal);
                FileUtils.setFileTo755(file);
            } catch (Exception ex) {
                throw new TaskException(
                        String.format("Download resource file: %s error", resourceAbsolutePathInStorage), ex);
            }
        } else {
            log.warn("resource file already exist, skip download. file: {}", resourceAbsolutePathInLocal);
        }

        ResourceContext.ResourceItem resourceItem = ResourceContext.ResourceItem.builder()
                .resourceAbsolutePathInStorage(resourceAbsolutePathInStorage)
                .resourceRelativePath(resourceRelativePath)
                .resourceAbsolutePathInLocal(resourceAbsolutePathInLocal)
                .build();
        taskExecutionContext.getResourceContext().addResourceItem(resourceItem);
        return resourceAbsolutePathInLocal;
    }

    private String getLocalDownloadString(String fileName) {
        return FileUtils.formatDownloadUpstreamLocalFullPath(taskExecutionContext.getExecutePath(), fileName);
    }

    @Override
    public void handle(TaskCallBack taskCallBack) throws TaskException {
        try {
            for (ResourceTaskParameter parameter : resourceParameters.getResourceItems()) {

                String storeFileName = parameter.getFileName();
                String localFilePath = parameter.getSourceLocalAbsoluteFile();

                // parse content
                if (parameter.needParse()) {
                    if (StringUtils.isEmpty(parameter.getFileContext())) {
                        String msg = "file context is empty, can not parse content. parameter: " + parameter;
                        exitStatusCode = -1;
                        throw new TaskException(msg);
                    }

                    parameter.setFileContext(getParsedContent(parameter));
                    localFilePath = reWriteToLocalWithFileName(parameter);
                }

                // upload or delete
                switch (parameter.getOperMethod()) {
                    case UPLOAD:
                    case UPLOAD_FORCE:
                    case DELETE:
                        String tenant = parameter.getTenant();
                        if (StringUtils.isEmpty(tenant)) {
                            tenant = "default";
                        }
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
