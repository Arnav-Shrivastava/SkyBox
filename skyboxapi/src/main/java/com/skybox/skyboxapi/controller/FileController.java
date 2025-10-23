package com.skybox.skyboxapi.controller;

import com.skybox.skyboxapi.document.UserCredits;
import com.skybox.skyboxapi.dto.FileMetadataDTO;
import com.skybox.skyboxapi.service.CloudflareR2Service;
import com.skybox.skyboxapi.service.FileMetadataService;
import com.skybox.skyboxapi.service.UserCreditsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/files")
public class FileController {

    private final FileMetadataService fileMetadataService;
    private final UserCreditsService userCreditsService;
    private final CloudflareR2Service r2Service;

    @PostMapping("/upload")
    public ResponseEntity<?> uploadFiles(@RequestPart("files") MultipartFile files[]) throws IOException {
        Map<String, Object> response = new HashMap<>();
        List<FileMetadataDTO> list = fileMetadataService.uploadFiles(files);

        UserCredits finalCredits = userCreditsService.getUserCredits();

        response.put("files", list);
        response.put("remainingCredits", finalCredits.getCredits());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/my")
    public ResponseEntity<?> getFilesForCurrentUser() {
        List<FileMetadataDTO> files = fileMetadataService.getFiles();
        return ResponseEntity.ok(files);
    }

    @GetMapping("/public/{id}")
    public ResponseEntity<?> getPublicFile(@PathVariable String id) {
        FileMetadataDTO file = fileMetadataService.getPublicFile(id);
        return ResponseEntity.ok(file);
    }

    @GetMapping("/download/{id}")
    public ResponseEntity<byte[]> download(@PathVariable String id) throws IOException {
        FileMetadataDTO downloadbleFile = fileMetadataService.getDownloadableFile(id);
        
        // Download file from Cloudflare R2
        byte[] fileData = r2Service.downloadFile(downloadbleFile.getFileLocation());

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\""+downloadbleFile.getName()+"\"")
                .body(fileData);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteFile(@PathVariable String id) {
        fileMetadataService.deleteFile(id);
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/toggle-public")
    public ResponseEntity<?> togglePublic(@PathVariable String id) {
        FileMetadataDTO file = fileMetadataService.togglePublic(id);
        return ResponseEntity.ok(file);
    }
}
