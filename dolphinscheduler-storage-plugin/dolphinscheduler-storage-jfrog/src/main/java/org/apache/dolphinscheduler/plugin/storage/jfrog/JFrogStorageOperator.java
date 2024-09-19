
package org.apache.dolphinscheduler.plugin.storage.jfrog;

import static org.apache.dolphinscheduler.common.constants.Constants.*;

import java.io.BufferedReader;
import java.io.Closeable;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.lang3.StringUtils;
import org.apache.dolphinscheduler.common.constants.Constants;
import org.apache.dolphinscheduler.common.enums.ResUploadType;
import org.apache.dolphinscheduler.common.utils.PropertyUtils;
import org.apache.dolphinscheduler.plugin.storage.api.StorageEntity;
import org.apache.dolphinscheduler.plugin.storage.api.StorageOperate;
import org.apache.dolphinscheduler.plugin.task.api.TaskConstants;
import org.apache.dolphinscheduler.spi.enums.ResourceType;
import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.ArtifactoryClientBuilder;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.jfrog.artifactory.client.model.impl.FileImpl;
import org.jfrog.artifactory.client.model.impl.FolderImpl;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Data
public class JFrogStorageOperator implements Closeable, StorageOperate {

    public Artifactory artifactory;

    private String pwd;

    private String user;

    /*
     * jfrog repository name
     * default: dolphinscheduler
     */
    private String REPO_NAME;

    /*
     * jfrog url
     * default: http://localhost:8081/artifactory
     */
    private String jfrogUrl;

    /**
     * jfrog dir prefix. auto set '/' prefix if not set.
     * 
     * 1. system property: jfrog.dir.prefix
     * 2. common.properties: resource.jfrog.dir.prefix
     * 3. default: /local
     */
    private String jfrogDirPrefix = "/local";

    public JFrogStorageOperator() {
    }

    public void init() {
        setRepo();
        setUrl();
        setUser();
        setPwd();
        setDirPrefix();

        artifactory = ArtifactoryClientBuilder.create()
                .setUrl(jfrogUrl)
                .setUsername(user)
                .setPassword(pwd)
                .build();
    }

    private void setRepo() {
        REPO_NAME = StringUtils.isBlank(REPO_NAME)
                ? PropertyUtils.getString(Constants.JFROG_REPO_NAME, "dolphinscheduler")
                : REPO_NAME;
    }

    private void setUrl() {
        jfrogUrl = StringUtils.isBlank(jfrogUrl)
                ? PropertyUtils.getString(Constants.JFROG_URL, "http://localhost:8081/artifactory")
                : jfrogUrl;
    }

    private void setUser() {
        user = StringUtils.isBlank(user) ? PropertyUtils.getString(Constants.JFROG_USERNAME, "admin") : user;
    }

    private void setPwd() {
        pwd = StringUtils.isBlank(pwd) ? PropertyUtils.getString(Constants.JFROG_PASSWORD, "password") : pwd;
    }

    private void setDirPrefix() {
        jfrogDirPrefix = System.getProperty("jfrog.dir.prefix");

        if (StringUtils.isBlank(jfrogDirPrefix)) {
            jfrogDirPrefix = PropertyUtils.getString(Constants.JFROG_DIR_PREFIX, "");
        }
        if (StringUtils.isBlank(jfrogDirPrefix)) {
            jfrogDirPrefix = "/local";
        }
        if (!jfrogDirPrefix.startsWith(Constants.FOLDER_SEPARATOR)) {
            jfrogDirPrefix = Constants.FOLDER_SEPARATOR + jfrogDirPrefix;
        }
    }

    @Override
    public void createTenantDirIfNotExists(String tenantCode) throws Exception {
        String tenantDirPath = getResDir(tenantCode);
        if (!existsDir(tenantDirPath)) {
            artifactory.repository(REPO_NAME).folder(tenantDirPath).create();
        }
    }

    @Override
    public String getResDir(String tenantCode) {
        return jfrogDirPrefix + "/tenants/" + tenantCode + "/resources" + Constants.FOLDER_SEPARATOR;
    }

    @Override
    public String getUdfDir(String tenantCode) {
        return jfrogDirPrefix + "/tenants/" + tenantCode + "/udf" + Constants.FOLDER_SEPARATOR;
    }

    @Override
    public boolean mkdir(String tenantCode, String path) throws IOException {
        try {
            String fullPath = replactTenantCodePath(getResDir(tenantCode), path);
            Folder folder = artifactory.repository(REPO_NAME).folder(fullPath).create();
            return folder != null;
        } catch (Exception e) {
            throw new IOException("Failed to create directory", e);
        }
    }

    @Override
    public String getResourceFullName(String tenantCode, String fileName) {
        return replactTenantCodePath(getResDir(tenantCode), fileName);
    }

    /**
     * if the path is not start with tenantParentPath,
     * add tenantParentPath to the path
     * 
     * @param tenantParentPath
     * @param path
     * @return
     */
    public static String replactTenantCodePath(String tenantParentPath, String path) {
        // 去除开头的'/'
        final String tmpTenantParentPath = tenantParentPath.startsWith(Constants.FOLDER_SEPARATOR)
                ? tenantParentPath.substring(1)
                : tenantParentPath;
        final String tmpPath = path.startsWith(Constants.FOLDER_SEPARATOR) ? path.substring(1) : path;

        if (tmpPath.startsWith(tmpTenantParentPath)) {
            return Constants.FOLDER_SEPARATOR + tmpPath;
        }
        if (tmpTenantParentPath.endsWith(Constants.FOLDER_SEPARATOR)) {
            return Constants.FOLDER_SEPARATOR + tmpTenantParentPath + tmpPath;
        }
        return Constants.FOLDER_SEPARATOR + tmpTenantParentPath + Constants.FOLDER_SEPARATOR + tmpPath;
    }

    @Override
    public String getResourceFileName(String tenantCode, String fullName) {
        String tenantParentPath = getResDir(tenantCode);
        String filenameReplaceResDir = fullName.replaceFirst(tenantParentPath, "");
        if (!filenameReplaceResDir.equals(fullName)) {
            return filenameReplaceResDir;
        }
        return filenameReplaceResDir.contains(RESOURCE_TYPE_FILE)
                ? filenameReplaceResDir.split(String.format("%s/", RESOURCE_TYPE_FILE))[1]
                : filenameReplaceResDir;
    }

    @Override
    public String getFileName(ResourceType resourceType, String tenantCode, String fileName) {
        String tenantParentPath = resourceType == ResourceType.UDF ? getUdfDir(tenantCode) : getResDir(tenantCode);
        return replactTenantCodePath(tenantParentPath, fileName);
    }

    @Override
    public boolean exists(String fullName) {
        return existsFile(fullName);
    }

    private boolean existsDir(String folderFullName) {
        try {
            Folder folder = artifactory.repository(REPO_NAME).folder(folderFullName).info();
            return folder != null;
        } catch (Exception e) {
            if (e instanceof HttpResponseException) {
                HttpResponseException responseException = (HttpResponseException) e;
                if (responseException.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return false;
                }
            }
            log.error("Failed to check directory existence", e);
            throw e;
        }
    }

    private boolean existsFile(String fullName) {
        try {
            Item item = artifactory.repository(REPO_NAME).file(fullName).info();
            return item != null;
        } catch (Exception e) {
            if (e instanceof HttpResponseException) {
                HttpResponseException responseException = (HttpResponseException) e;
                if (responseException.getStatusCode() == HttpStatus.SC_NOT_FOUND) {
                    return false;
                }
            }
            log.error("Failed to check file existence", e);
            throw e;
        }
    }

    @Override
    public boolean delete(String fullFilePath, boolean recursive) throws IOException {
        String rep = artifactory.repository(REPO_NAME).delete(fullFilePath);
        return rep == null;
    }

    @Override
    public boolean delete(String filePath, List<String> childrenPathArray, boolean recursive) throws IOException {
        artifactory.repository(REPO_NAME).delete(filePath);

        if (childrenPathArray != null && !childrenPathArray.isEmpty()) {
            for (String childPath : childrenPathArray) {
                artifactory.repository(REPO_NAME).delete(childPath);
            }
        }

        return true;
    }

    @Override
    public String getDir(ResourceType resourceType, String tenantCode) {
        return resourceType == ResourceType.UDF ? getUdfDir(tenantCode) : getResDir(tenantCode);
    }

    @Override
    public boolean copy(String srcPath, String dstPath, boolean deleteSource, boolean overwrite) throws IOException {
        if (!overwrite && existsFile(dstPath)) {
            log.info("File already exists, skip copying: {}", dstPath);
            return true;
        }
        artifactory.repository(REPO_NAME).file(srcPath).copy(REPO_NAME, dstPath);
        if (deleteSource) {
            return delete(srcPath, false);
        }
        return true;
    }

    @Override
    public boolean upload(String tenantCode, String srcFile, String dstPath, boolean deleteSource, boolean overwrite)
            throws IOException {
        String fullPath = getResourceFullName(tenantCode, dstPath);
        if (!overwrite && existsFile(fullPath)) {
            log.info("File already exists, skip uploading: {}", fullPath);
            return true;
        }
        java.io.File file = new java.io.File(srcFile);
        artifactory.repository(REPO_NAME).upload(fullPath, file).doUpload();
        if (deleteSource) {
            return file.delete();
        }
        return true;
    }

    @Override
    public void download(String srcFilePath, String dstFile, boolean overwrite) throws IOException {
        DownloadableArtifact downloadableArtifact = artifactory.repository(REPO_NAME).download(srcFilePath);
        try {
            if (!overwrite && new java.io.File(dstFile).exists()) {
                log.warn("File already exists, skip downloading: {}", dstFile);
                return;
            }
            org.apache.commons.io.FileUtils.copyInputStreamToFile(downloadableArtifact.doDownload(),
                    new java.io.File(dstFile));
        } catch (IOException e) {
            log.error("Failed to download file", e);
        }
    }

    @Override
    public List<String> vimFile(String tenantCode, String filePath, int skipLineNums, int limit) throws IOException {
        if (filePath == null || filePath.trim().isEmpty()) {
            System.err.println("File path is blank: " + filePath);
            return Collections.emptyList();
        }

        DownloadableArtifact downloadableArtifact = artifactory.repository(REPO_NAME).download(filePath);

        try (BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(downloadableArtifact.doDownload()))) {
            Stream<String> stream = bufferedReader.lines().skip(skipLineNums).limit(limit);
            return stream.collect(Collectors.toList());
        }
    }

    @Override
    public void deleteTenant(String tenantCode) throws Exception {
        String tenantDir = getResDir(tenantCode);
        artifactory.repository(REPO_NAME).delete(tenantDir);
    }

    @Override
    public ResUploadType returnStorageType() {
        return ResUploadType.JFROG;
    }

    @Override
    public List<StorageEntity> listFilesStatusRecursively(String path, String defaultPath, String tenantCode,
            ResourceType type) {

        List<StorageEntity> storageEntityList = new ArrayList<>();
        LinkedList<StorageEntity> foldersToFetch = new LinkedList<>();

        StorageEntity initialEntity = null;
        try {
            initialEntity = getFileStatus(path, defaultPath, tenantCode, type);
        } catch (Exception e) {
            log.error("Error while listing files status recursively, path: {}", path, e);
            return storageEntityList;
        }

        if (initialEntity != null) {
            foldersToFetch.add(initialEntity);
        }

        while (!foldersToFetch.isEmpty()) {
            StorageEntity folderEntity = foldersToFetch.pop();
            String pathToExplore = folderEntity.getFullName();
            try {
                List<StorageEntity> tempList = listFilesStatus(pathToExplore, defaultPath, tenantCode, type);
                for (StorageEntity temp : tempList) {
                    if (temp.isDirectory()) {
                        foldersToFetch.add(temp);
                    }
                }
                storageEntityList.addAll(tempList);
            } catch (Exception e) {
                log.error("Error while listing files status recursively, path: {}", pathToExplore, e);
            }
        }

        return storageEntityList;
    }

    @Override
    public List<StorageEntity> listFilesStatus(String path, String defaultPath, String tenantCode, ResourceType type)
            throws Exception {
        List<StorageEntity> storageEntityList = new ArrayList<>();

        try {
            // dict path should end with '/' and start with '/'
            if (!path.endsWith(Constants.FOLDER_SEPARATOR)) {
                path = path + Constants.FOLDER_SEPARATOR;
            }
            if (!path.startsWith(Constants.FOLDER_SEPARATOR)) {
                path = Constants.FOLDER_SEPARATOR + path;
            }

            String fullPath = defaultPath + path.replaceFirst(defaultPath, "");
            if (!existsDir(fullPath)) {
                return storageEntityList;
            }
            Folder folder = artifactory.repository(REPO_NAME).folder(fullPath).info();

            // Handling files in the directory
            folder.getChildren().forEach(child -> {
                try {
                    StorageEntity entity = getFileStatus(child.getUri(), folder.getPath(), tenantCode, type);
                    if (entity != null) {
                        storageEntityList.add(entity);
                    }
                } catch (Exception e) {
                    log.error("Error while listing files status, path: {}", child.getUri(), e);
                }
            });
        } catch (Exception e) {
            throw new Exception("Error listing files in Artifactory: " + e.getMessage(), e);
        }

        return storageEntityList;
    }

    @Override
    public StorageEntity getFileStatus(String path, String defaultPath, String tenantCode, ResourceType type)
            throws Exception {
        String fullPath = defaultPath + path.replaceFirst(defaultPath, "");
        try {
            if (!existsFile(fullPath)) {
                return null;
            }
            StorageEntity entity = new StorageEntity();
            if (artifactory.repository(REPO_NAME).isFolder(fullPath)) {
                FolderImpl folderInfo = artifactory.repository(REPO_NAME).folder(fullPath).info();
                entity.setAlias(folderInfo.getName());
                entity.setFileName(folderInfo.getName());
                entity.setFullName(folderInfo.getPath());
                entity.setDirectory(folderInfo.isFolder());
                entity.setPfullName(folderInfo.getPath().substring(0, folderInfo.getPath().lastIndexOf("/")));
                entity.setUserName(tenantCode);
                entity.setType(type);
                entity.setSize(0);
                entity.setCreateTime(folderInfo.getCreated());
                entity.setUpdateTime(folderInfo.getLastUpdated());
            } else {
                FileImpl fileInfo = artifactory.repository(REPO_NAME).file(fullPath).info();
                entity.setAlias(fileInfo.getName());
                entity.setFileName(fileInfo.getName());
                entity.setFullName(fileInfo.getPath());
                entity.setDirectory(fileInfo.isFolder());
                entity.setPfullName(fileInfo.getPath().substring(0, fileInfo.getPath().lastIndexOf("/")));
                entity.setUserName(tenantCode);
                entity.setType(type);
                entity.setSize(fileInfo.getSize());
                entity.setCreateTime(fileInfo.getCreated());
                entity.setUpdateTime(fileInfo.getLastUpdated());
            }
            return entity;
        } catch (Exception e) {
            // 文件和目录都没找到时抛出异常
            throw new FileNotFoundException("Object is not found in Artifactory: " + fullPath);
        }
    }

    @Override
    public void close() throws IOException {
        artifactory.close();
    }
}
