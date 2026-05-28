package com.company.oa.file;

import com.company.oa.BaseSpringTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.assertj.core.api.Assertions.assertThat;

class FileLibraryServiceTest extends BaseSpringTest {

    @Autowired
    private FileLibraryService fileLibraryService;

    @Test
    void foldersTreeReturnsList() {
        var tree = fileLibraryService.foldersTree();
        assertThat(tree).isNotNull();
    }

    @Test
    void createAndListFolders() {
        var req = new FileLibraryDtos.FolderCreateRequest(null, "测试文件夹");
        var created = fileLibraryService.createFolder(req);
        assertThat(created.get("folderName")).isEqualTo("测试文件夹");

        var tree = fileLibraryService.foldersTree();
        assertThat(tree).isNotEmpty();
    }

    @Test
    void createAndListFiles() {
        // Create a folder first
        var folderReq = new FileLibraryDtos.FolderCreateRequest(null, "文件测试文件夹");
        var folder = fileLibraryService.createFolder(folderReq);
        long folderId = ((Number) folder.get("id")).longValue();

        var fileReq = new FileLibraryDtos.FileCreateRequest(
                folderId, "test-document.pdf", "application/pdf", 1024L
        );
        var created = fileLibraryService.createFile(fileReq);
        assertThat(created.get("fileName")).isEqualTo("test-document.pdf");
        assertThat(created.get("status")).isEqualTo("NORMAL");

        var files = fileLibraryService.listFiles(1, 20, folderId, null);
        assertThat(files.total()).isGreaterThanOrEqualTo(1);
    }

    @Test
    void deleteAndRestoreFile() {
        var folderReq = new FileLibraryDtos.FolderCreateRequest(null, "删除测试文件夹");
        var folder = fileLibraryService.createFolder(folderReq);
        long folderId = ((Number) folder.get("id")).longValue();

        var fileReq = new FileLibraryDtos.FileCreateRequest(
                folderId, "to-delete.txt", "text/plain", 100L
        );
        var created = fileLibraryService.createFile(fileReq);
        long fileId = ((Number) created.get("id")).longValue();

        var deleted = fileLibraryService.delete(fileId);
        assertThat(deleted.get("status")).isEqualTo("PENDING_CLEAN");

        var restored = fileLibraryService.restoreFile(fileId);
        assertThat(restored.get("status")).isEqualTo("NORMAL");
    }
}
