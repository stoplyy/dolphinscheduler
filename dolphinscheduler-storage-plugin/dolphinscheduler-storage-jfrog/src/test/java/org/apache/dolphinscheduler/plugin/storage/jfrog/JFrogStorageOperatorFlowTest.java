package org.apache.dolphinscheduler.plugin.storage.jfrog;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.mockito.Mockito.mock;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

import org.apache.dolphinscheduler.plugin.storage.api.StorageEntity;
import org.apache.dolphinscheduler.spi.enums.ResourceType;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JFrogStorageOperatorFlowTest {

    private static final String ENV = "unit";

    private static final String UNITTEST = "unittest";

    private static JFrogStorageOperator jFrogStorageOperator;

    RepositoryHandle mockRepository = mock(RepositoryHandle.class);
    ItemHandle mockFolderHandle = mock(ItemHandle.class);

    @BeforeAll
    public static void beforeClass() {
        jFrogStorageOperator = new JFrogStorageOperator();
        jFrogStorageOperator.init();
        jFrogStorageOperator.setREPO_NAME("arch-stellarops");
        jFrogStorageOperator.setJfrogDirPrefix("/" + ENV);
        jFrogStorageOperator.setJfrogUrl("https://packages.tuhuyun.cn/artifactory/");
        jFrogStorageOperator.setUser("arch-stellarops");
        jFrogStorageOperator.setPwd("Tuhu@1234");
    }

    @AfterAll
    public static void afterClass() throws IOException {
        jFrogStorageOperator.close();
    }

    @BeforeEach
    public void setUp() throws Exception {
        jFrogStorageOperator.createTenantDirIfNotExists(UNITTEST);
    }

    @AfterEach
    public void after() throws Exception {
        jFrogStorageOperator.deleteTenant(UNITTEST);
    }

    @Test
    public void test_getResDir() throws Exception {
        String resDir = jFrogStorageOperator.getResDir(UNITTEST);
        assert resDir.equals("/" + ENV + "/tenants/" + UNITTEST + "/resources/");
    }

    @Test
    public void test_getUdfDir() throws Exception {
        String udfDir = jFrogStorageOperator.getUdfDir(UNITTEST);
        assert udfDir.equals("/" + ENV + "/tenants/" + UNITTEST + "/udf/");
    }

    @Test
    public void test_mkdir() throws IOException {
        boolean result = jFrogStorageOperator.mkdir(UNITTEST, "newDir");
        assert result;
    }

    @Test
    public void test_getResourceFullName() throws Exception {
        String fullName = jFrogStorageOperator.getResourceFullName(UNITTEST, "test.txt");
        assert fullName.equals("/" + ENV + "/tenants/" + UNITTEST + "/resources/test.txt");
    }

    @Test
    public void test_getResourceFileName() throws Exception {
        String fullName = jFrogStorageOperator.getResourceFullName(UNITTEST, "test.txt");
        String fileName = jFrogStorageOperator.getResourceFileName(UNITTEST, fullName);
        assert fileName.equals("test.txt");
    }

    @Test
    public void test_getFileName() throws IOException {
        String fileName = jFrogStorageOperator.getFileName(ResourceType.FILE, UNITTEST, "test.txt");
        assert fileName.equals("/" + ENV + "/tenants/" + UNITTEST + "/resources/test.txt");
    }

    @Test
    public void test_exists() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;
        boolean exists = jFrogStorageOperator.exists(dstFullName);
        assert !exists;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        assert jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);
        assert jFrogStorageOperator.exists(dstFullName);
    }

    @Test
    public void test_delete() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;
        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        assert jFrogStorageOperator.exists(dstFullName);
        assert jFrogStorageOperator.delete(dstFullName, false);
        assert !jFrogStorageOperator.exists(dstFullName);
    }

    @Test
    public void test_deleteWithChildren() throws Exception {
        String dstFileName = "test.txt";
        String dstFileName_2 = "test2.txt";

        String dstFileFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;
        String dstFileFullName_2 = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName_2;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        jFrogStorageOperator.copy(dstFileFullName, dstFileFullName_2, false, false);

        assert jFrogStorageOperator.exists(dstFileFullName);
        assert jFrogStorageOperator.exists(dstFileFullName_2);

        assert jFrogStorageOperator.delete(dstFileFullName, Arrays.asList(dstFileFullName_2), true);

        assert !jFrogStorageOperator.exists(dstFileFullName);
        assert !jFrogStorageOperator.exists(dstFileFullName_2);
    }

    @Test
    public void test_copy() throws Exception {
        String dstFileName = "test.txt";
        String dstFileName_2 = "test2.txt";
        String dstFileName_3 = "test3.txt";
        String dstFileName_4 = "test4.txt";

        String dstFileFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;
        String dstFileFullName_2 = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName_2;
        String dstFileFullName_3 = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName_3;
        String dstFileFullName_4 = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName_4;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        assert jFrogStorageOperator.exists(dstFileFullName);
        assert jFrogStorageOperator.copy(dstFileFullName, dstFileFullName_2, false, false);
        assert jFrogStorageOperator.exists(dstFileFullName_2);

        assert jFrogStorageOperator.copy(dstFileFullName, dstFileFullName_3, true, false);
        assert !jFrogStorageOperator.exists(dstFileFullName);
        assert jFrogStorageOperator.exists(dstFileFullName_3);

        // TEST overwrite
        File tempFile_old = File.createTempFile("test", ".txt");
        tempFile_old.deleteOnExit(); // 确保测试结束时文件被删除
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write("This is a test file for JFrog upload. old");
        }
        jFrogStorageOperator.upload(UNITTEST, tempFile_old.getPath(), dstFileName_4, true, true);

        assert jFrogStorageOperator.exists(dstFileFullName_4);
        assert jFrogStorageOperator.getFileStatus(dstFileFullName_4, "", UNITTEST, ResourceType.FILE)
                .getSize() == tempFile_old.length();

        assert jFrogStorageOperator.copy(dstFileFullName_3, dstFileFullName_4, false, false);
        assert jFrogStorageOperator.getFileStatus(dstFileFullName_4, "", UNITTEST, ResourceType.FILE)
                .getSize() == tempFile_old.length();

        assert jFrogStorageOperator.copy(dstFileFullName_3, dstFileFullName_4, false, true);
        assert jFrogStorageOperator.getFileStatus(dstFileFullName_4, "", UNITTEST, ResourceType.FILE)
                .getSize() == content.length();

    }

    @Test
    public void test_getDir() throws Exception {
        String dir = jFrogStorageOperator.getDir(ResourceType.FILE, UNITTEST);
        assert dir.equals(jFrogStorageOperator.getResDir(UNITTEST));
    }

    @Test
    public void test_upload() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        assert jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);
        assert jFrogStorageOperator.getFileStatus(dstFullName, "", UNITTEST, ResourceType.FILE).getSize() == content
                .length();

        // TEST overwrite
        File tempFile_old = File.createTempFile("test", ".txt");
        String content_old = "This is a test file for JFrog upload. old";
        tempFile_old.deleteOnExit(); // 确保测试结束时文件被删除
        try (FileWriter writer = new FileWriter(tempFile_old)) {
            writer.write(content_old);
        }

        assert jFrogStorageOperator.upload(UNITTEST, tempFile_old.getPath(), dstFileName, false, false);
        assert jFrogStorageOperator.getFileStatus(dstFullName, "", UNITTEST, ResourceType.FILE).getSize() == content
                .length();

        assert jFrogStorageOperator.upload(UNITTEST, tempFile_old.getPath(), dstFileName, false, true);
        assert jFrogStorageOperator.getFileStatus(dstFullName, "", UNITTEST, ResourceType.FILE).getSize() == content_old
                .length();
    }

    @Test
    public void test_download() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        String downloadPath = tempFile.getParent() + "/download.txt";

        jFrogStorageOperator.download(dstFullName, downloadPath, false);
        File downloadFile = new File(downloadPath);
        assert downloadFile.exists();
        // 验证内容
        assert downloadFile.length() == content.length();
        downloadFile.deleteOnExit();

        // TEST overwrite
        File tempFile_old = File.createTempFile("test_old", ".txt");
        tempFile_old.deleteOnExit(); // 确保测试结束时文件被删除
        String content_old = "This is a test file for JFrog upload. old";
        try (FileWriter writer = new FileWriter(tempFile_old)) {
            writer.write(content_old);
        }
        assert tempFile_old.length() == content_old.length();

        jFrogStorageOperator.download(dstFullName, tempFile_old.getPath(), false);

        assert tempFile_old.length() == content_old.length();

        jFrogStorageOperator.download(dstFullName, tempFile_old.getPath(), true);

        assert tempFile_old.length() == content.length();
    }

    @Test
    public void test_vimFile() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String firstLine = "This is a test file for JFrog upload.";
        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(firstLine + "\nThis is a test file for JFrog upload. 2");
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        List<String> lines = jFrogStorageOperator.vimFile(UNITTEST, dstFullName, 0, 10);
        assert lines.size() == 2;
        assert lines.get(0).equals(firstLine);
    }

    @Test
    public void test_deleteTenant() throws Exception {
    }

    @Test
    public void test_listFilesStatusRecursively() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/";

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        List<StorageEntity> files = jFrogStorageOperator.listFilesStatusRecursively(dstFullName, "", UNITTEST,
                ResourceType.FILE);

        assert files.size() == 1;
        assert files.get(0).getAlias().equals(dstFileName);
    }

    @Test
    public void test_listFilesStatus() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/";

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        List<StorageEntity> files = jFrogStorageOperator.listFilesStatus(dstFullName, "", UNITTEST, ResourceType.FILE);

        assert files.size() == 1;
        assert files.get(0).getAlias().equals(dstFileName);
    }

    @Test
    public void test_getFileStatus() throws Exception {
        String dstFileName = "test.txt";
        String dstFullName = jFrogStorageOperator.getResDir(UNITTEST) + "/" + dstFileName;

        // 1. 创建临时文件
        File tempFile = File.createTempFile("test", ".txt");
        tempFile.deleteOnExit(); // 确保测试结束时文件被删除
        String content = "This is a test file for JFrog upload.";

        try (FileWriter writer = new FileWriter(tempFile)) {
            writer.write(content);
        }

        jFrogStorageOperator.upload(UNITTEST, tempFile.getPath(), dstFileName, true, true);

        StorageEntity fileStatus = jFrogStorageOperator.getFileStatus(dstFullName, "", UNITTEST, ResourceType.FILE);

        assert fileStatus != null;
        assert fileStatus.isDirectory() == false;
        assert fileStatus.getAlias().equals(dstFileName);
    }

}
