package com.restro.Repo;

import com.restro.entity.UploadedFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface UploadedFileRepository extends JpaRepository<UploadedFile, Integer> {
    Optional<UploadedFile> findByRelativePath(String relativePath);
}
