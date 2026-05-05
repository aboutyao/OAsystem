package com.company.oa.file;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public final class FileLibraryDtos {
    private FileLibraryDtos() {
    }

    public record FolderCreateRequest(
            Long parentId,
            @NotBlank String folderName
    ) {
    }

    public record FolderUpdateRequest(
            @NotBlank String folderName
    ) {
    }

    public record FileCreateRequest(
            Long folderId,
            @NotBlank String fileName,
            String mimeType,
            Long fileSize
    ) {
    }

    public record FileMoveRequest(@NotNull Long folderId) {
    }

    public record FileVersionRequest(
            @NotBlank String fileName,
            String mimeType,
            Long fileSize
    ) {
    }
}
