package com.restro.Impl;

import com.restro.Repo.UploadedFileRepository;
import com.restro.Service.FileStorageService;
import com.restro.entity.UploadedFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    @Autowired
    private UploadedFileRepository uploadedFileRepository;

    @Override
    @Transactional
    public String store(MultipartFile file, String subdir) throws IOException {
        String ext = extensionOf(file.getOriginalFilename());
        String relativePath = "/uploads/" + subdir + "/" + UUID.randomUUID() + ext;
        UploadedFile entity = UploadedFile.builder()
                .relativePath(relativePath)
                .contentType(file.getContentType() != null ? file.getContentType() : "application/octet-stream")
                .data(file.getBytes())
                .build();
        uploadedFileRepository.save(entity);
        return relativePath;
    }

    @Override
    @Transactional
    public String storeBytes(byte[] bytes, String contentType, String subdir, String suggestedName) {
        String relativePath = "/uploads/" + subdir + "/" + suggestedName;
        UploadedFile entity = UploadedFile.builder()
                .relativePath(relativePath)
                .contentType(contentType)
                .data(bytes)
                .build();
        uploadedFileRepository.save(entity);
        return relativePath;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<UploadedFile> find(String relativePath) {
        return uploadedFileRepository.findByRelativePath(relativePath);
    }

    private String extensionOf(String filename) {
        if (filename == null || !filename.contains(".")) {
            return "";
        }
        return filename.substring(filename.lastIndexOf('.'));
    }
}
