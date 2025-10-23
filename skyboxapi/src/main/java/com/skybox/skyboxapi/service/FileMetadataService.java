package com.skybox.skyboxapi.service;

import com.skybox.skyboxapi.document.FileMetadataDocument;
import com.skybox.skyboxapi.document.ProfileDocument;
import com.skybox.skyboxapi.dto.FileMetadataDTO;
import com.skybox.skyboxapi.repository.FileMetadataRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FileMetadataService {

    private final ProfileService profileService;
    private final UserCreditsService userCreditsService;
    private final FileMetadataRepository fileMetadataRepository;
    private final CloudflareR2Service r2Service;

    public List<FileMetadataDTO> uploadFiles(MultipartFile files[]) throws IOException {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetadataDocument> savedFiles = new ArrayList<>();

        if (!userCreditsService.hasEnoughCredits(files.length)) {
            throw new RuntimeException("Not enough credits to upload files. Please purchase more credits");
        }

        for (MultipartFile file : files) {
            // Upload file to Cloudflare R2
            String folder = "private/" + currentProfile.getClerkId();
            String fileKey = r2Service.uploadFile(file, folder);

            FileMetadataDocument fileMetadata = FileMetadataDocument.builder()
                    .fileLocation(fileKey)  // Store R2 key instead of local path
                    .name(file.getOriginalFilename())
                    .size(file.getSize())
                    .type(file.getContentType())
                    .clerkId(currentProfile.getClerkId())
                    .isPublic(false)
                    .uploadedAt(LocalDateTime.now())
                    .build();

            userCreditsService.consumeCredit();

            savedFiles.add(fileMetadataRepository.save(fileMetadata));
        }
        return savedFiles.stream().map(fileMetadataDocument -> mapToDTO(fileMetadataDocument))
                .collect(Collectors.toList());

    }

    private FileMetadataDTO mapToDTO(FileMetadataDocument fileMetadataDocument) {
        return FileMetadataDTO.builder()
                .id(fileMetadataDocument.getId())
                .fileLocation(fileMetadataDocument.getFileLocation())
                .name(fileMetadataDocument.getName())
                .size(fileMetadataDocument.getSize())
                .type(fileMetadataDocument.getType())
                .clerkId(fileMetadataDocument.getClerkId())
                .isPublic(fileMetadataDocument.getIsPublic())
                .uploadedAt(fileMetadataDocument.getUploadedAt())
                .build();
    }

    public List<FileMetadataDTO> getFiles() {
        ProfileDocument currentProfile = profileService.getCurrentProfile();
        List<FileMetadataDocument> files = fileMetadataRepository.findByClerkId(currentProfile.getClerkId());
        return files.stream().map(this::mapToDTO).collect(Collectors.toList());
    }

    public FileMetadataDTO getPublicFile(String id) {
        Optional<FileMetadataDocument> fileOptional = fileMetadataRepository.findById(id);
        if (fileOptional.isEmpty() || !fileOptional.get().getIsPublic()) {
            throw new RuntimeException("Unable to get the file");
        }

        FileMetadataDocument document = fileOptional.get();
        return mapToDTO(document);
    }

    public FileMetadataDTO getDownloadableFile(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id).orElseThrow(() -> new RuntimeException("File not found"));
        return mapToDTO(file);
    }

    public void deleteFile(String id) {
        try {
            ProfileDocument currentProfile = profileService.getCurrentProfile();
            FileMetadataDocument file = fileMetadataRepository.findById(id)
                    .orElseThrow(() -> new RuntimeException("File not found"));

            if (!file.getClerkId().equals(currentProfile.getClerkId())) {
                throw new RuntimeException("File is not belong to current user");
            }

            // Delete file from Cloudflare R2
            r2Service.deleteFile(file.getFileLocation());

            fileMetadataRepository.deleteById(id);
        }catch (Exception e) {
            throw new RuntimeException("Error deleting the file");
        }
    }

    public FileMetadataDTO togglePublic(String id) {
        FileMetadataDocument file = fileMetadataRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("File not found"));

        file.setIsPublic(!file.getIsPublic());
        fileMetadataRepository.save(file);
        return mapToDTO(file);
    }
}