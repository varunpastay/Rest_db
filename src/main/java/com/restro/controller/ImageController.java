package com.restro.controller;

import org.springframework.beans.factory.annotation.Autowired;

import com.restro.entity.UploadedFile;
import com.restro.Service.FileStorageService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/** Serves BLOB-stored images (logos, food photos, QR codes) back out by their /uploads/... relative path. */
@RestController
public class ImageController {

    @Autowired
    private FileStorageService fileStorageService;

    @GetMapping("/images")
    public ResponseEntity<byte[]> serve(@RequestParam String path) {
        return fileStorageService.find(path)
                .map(f -> ResponseEntity.ok().contentType(MediaType.parseMediaType(f.getContentType())).body(f.getData()))
                .orElse(ResponseEntity.notFound().build());
    }
}
