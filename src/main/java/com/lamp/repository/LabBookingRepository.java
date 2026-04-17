package com.lamp.repository;

import com.lamp.entity.LabBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LabBookingRepository extends JpaRepository<LabBooking, Long> {
    Optional<LabBooking> findByLabIdAndDateAndSlotAndStatusNot(Long labId, LocalDate date, String slot, String status);

    boolean existsByLabIdAndDateAndSlotAndStatusIn(Long labId, LocalDate date, String slot, List<String> statuses);

    Page<LabBooking> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<LabBooking> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    long countByStatus(String status);
}
