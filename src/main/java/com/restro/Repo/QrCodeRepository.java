package com.restro.Repo;

import com.restro.entity.QrCode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface QrCodeRepository extends JpaRepository<QrCode, Integer> {
    List<QrCode> findByTable_TableIdOrderByGeneratedAtDesc(Integer tableId);
}
