package org.apache.dolphinscheduler.plugin.storage.jfrog;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Arrays;

import org.apache.dolphinscheduler.plugin.storage.api.StorageEntity;
import org.apache.dolphinscheduler.plugin.task.api.TaskConstants;
import org.apache.dolphinscheduler.spi.enums.ResourceType;
import org.jfrog.artifactory.client.Artifactory;
import org.jfrog.artifactory.client.DownloadableArtifact;
import org.jfrog.artifactory.client.ItemHandle;
import org.jfrog.artifactory.client.RepositoryHandle;
import org.jfrog.artifactory.client.model.Folder;
import org.jfrog.artifactory.client.model.Item;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
public class JFrogStorageOperatorTest {

    @Mock
    private Artifactory artifactory;

    @InjectMocks
    private JFrogStorageOperator jFrogStorageOperator;

    RepositoryHandle mockRepository = mock(RepositoryHandle.class);
    ItemHandle mockFolderHandle = mock(ItemHandle.class);

    @BeforeEach
    void setUp() {
        jFrogStorageOperator = new JFrogStorageOperator();
        jFrogStorageOperator.init();
        jFrogStorageOperator.artifactory = this.artifactory;

    }

    @Test
    public void testCreateTenantDirIfNotExists_DirDoesNotExist() throws Exception {
        // 假设目录不存在
        when(artifactory.repository(anyString())).thenReturn(mockRepository);
        when(mockRepository.folder(anyString())).thenReturn(mockFolderHandle);
        when(mockFolderHandle.info()).thenReturn(null);

        // 执行方法
        jFrogStorageOperator.createTenantDirIfNotExists("tenant1");

        // 验证 create 方法被调用
        verify(mockRepository.folder(anyString())).create();
        verify(mockRepository.folder(jFrogStorageOperator.getResDir("tenant1"))).create();
    }

    @Test
    public void testCreateTenantDirIfNotExists_DirExists() throws Exception {
        // 假设目录已存在
        when(artifactory.repository(anyString())).thenReturn(mockRepository);
        when(mockRepository.folder(anyString())).thenReturn(mockFolderHandle);
        Item mockItem = mock(Item.class);
        when(mockFolderHandle.info()).thenReturn(mockItem);

        // 执行方法
        jFrogStorageOperator.createTenantDirIfNotExists("tenant1");

        // 验证没有尝试创建目录
        verify(mockFolderHandle, never()).create();
    }

    @Test
    void testMkdir() throws IOException {
        Folder mockFolder = mock(Folder.class);
        when(artifactory.repository(anyString()).folder(anyString()).create()).thenReturn(mockFolder);

        boolean result = jFrogStorageOperator.mkdir("tenant1", "newDir");

        assertTrue(result);
    }

    @Test
    void testExists() throws IOException {
        when(artifactory.repository(anyString()).file(anyString()).info()).thenReturn(mock(Item.class));

        boolean exists = jFrogStorageOperator.exists("path/to/file");

        assertTrue(exists);
    }

    @Test
    void testDeleteFile() throws IOException {
        when(artifactory.repository(anyString()).delete(anyString())).thenReturn("Success");

        boolean result = jFrogStorageOperator.delete("path/to/file", false);

        assertTrue(result);
    }

    @Test
    void testDeleteFileWithChildren() throws IOException {
        when(artifactory.repository(anyString()).delete(anyString())).thenReturn("Success");

        boolean result = jFrogStorageOperator.delete("path/to/directory",
                Arrays.asList("child1", "child2"), true);

        assertTrue(result);
    }

    @Test
    void testCopy() throws IOException {
        when(artifactory.repository(anyString()).folder("").copy(anyString(), anyString())).thenReturn(null);
        when(artifactory.repository(anyString()).delete(anyString())).thenReturn("Success");

        boolean result = jFrogStorageOperator.copy("srcPath", "dstPath", true, true);

        assertTrue(result);
    }

    @Test
    void testUpload() throws IOException {
        when(artifactory.repository(anyString()).upload(anyString(), any(java.io.File.class)).doUpload())
                .thenReturn(null);

        boolean result = jFrogStorageOperator.upload("tenant1", "srcFile", "dstPath", true, true);

        assertTrue(result);
    }

    @Test
    void testDownload() throws IOException {
        DownloadableArtifact downloadableArtifact = mock(DownloadableArtifact.class);
        when(artifactory.repository(anyString()).download(anyString())).thenReturn(downloadableArtifact);
        doNothing().when(downloadableArtifact).doDownload();

        assertDoesNotThrow(() -> jFrogStorageOperator.download("srcFilePath", "dstFile", true));
    }

    @Test
    void testListFilesStatusRecursively() throws Exception {
        // Implementation-specific setup for recursive listing test
    }

    @Test
    void testListFilesStatus() throws Exception {
        // Implementation-specific setup for listing files test
    }

    @Test
    void testGetFileStatus() throws Exception {
        Item mockItem = mock(Item.class);
        when(artifactory.repository(anyString()).file(anyString()).info()).thenReturn(mockItem);
        when(mockItem.getName()).thenReturn("file.txt");

        StorageEntity result = jFrogStorageOperator.getFileStatus("path", "/default/path", "tenant", ResourceType.FILE);

        assertNotNull(result);
    }

    @Test
    void testDeleteTenant() throws Exception {
        when(artifactory.repository(anyString()).delete(anyString())).thenReturn("Success");

        assertDoesNotThrow(() -> jFrogStorageOperator.deleteTenant("tenantCode"));
    }
}
