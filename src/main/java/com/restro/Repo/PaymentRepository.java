package com.restro.Repo;

import com.restro.entity.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Integer> {
    Optional<Payment> findByTableSession_TableSessionId(Integer tableSessionId);
    Optional<Payment> findByInvoiceNo(String invoiceNo);
}
