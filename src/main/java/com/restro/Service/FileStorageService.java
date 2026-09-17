package com.restro.Service;

import com.restro.entity.UploadedFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;

/**
 * Uploaded/generated images (logos, food photos, QR codes) are stored as DB
 * BLOBs, exactly like the original app - keeps the container stateless, no
 * persistent volume needed on cheap hosting.
 */
public interface FileStorageService {

    String store(MultipartFile file, String subdir) throws IOException;

    String storeBytes(byte[] bytes, String contentType, String subdir, String suggestedName);

    Optional<UploadedFile> find(String relativePath);
}
