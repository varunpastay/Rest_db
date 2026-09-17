package com.restro.controller;

import com.restro.Service.BackupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.time.LocalDateTime;

/**
 * Settings > Backup: download a JSON snapshot of the restaurant's
 * configuration (profile, branding, menu, tables, taxes, discounts), and
 * restore/import one back in. See BackupService for exactly what is and
 * isn't included and why.
 */
@Controller
public class OwnerBackupController {

    @Autowired
    private BackupService backupService;

    @GetMapping("/owner/settings/backup/export")
    public ResponseEntity<byte[]> export() {
        byte[] json = backupService.exportBackup();
        String filename = "restaurant-backup-" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss")) + ".json";
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_JSON)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=" + filename)
                .body(json);
    }

    @PostMapping("/owner/settings/backup/restore")
    public String restore(@RequestParam MultipartFile backupFile, Model model) {
        try {
            backupService.restoreBackup(backupFile.getBytes());
            return "redirect:/owner/settings?backupRestored";
        } catch (IOException | RuntimeException e) {
            return "redirect:/owner/settings?backupError=" + java.net.URLEncoder.encode(
                    e.getMessage() == null ? "Restore failed" : e.getMessage(), java.nio.charset.StandardCharsets.UTF_8);
        }
    }
}
