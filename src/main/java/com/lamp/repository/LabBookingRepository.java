package com.lamp.repository;

import com.lamp.entity.LabBooking;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface LabBookingRepository extends JpaRepository<LabBooking, Long> {
    Optional<LabBooking> findByLabIdAndDateAndSlotAndStatusNot(Long labId, LocalDate date, String slot, String status);

    boolean existsByLabIdAndDateAndSlotAndStatusIn(Long labId, LocalDate date, String slot, List<String> statuses);

    Page<LabBooking> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<LabBooking> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    List<LabBooking> findByDateBetween(LocalDate startDate, LocalDate endDate);

    List<LabBooking> findByLabIdAndDateBetween(Long labId, LocalDate startDate, LocalDate endDate);

    List<LabBooking> findByStatusInAndDateBetween(Collection<String> statuses, LocalDate startDate, LocalDate endDate);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    long countByStatus(String status);

    long countByStatusInAndDateBetween(Collection<String> statuses, LocalDate startDate, LocalDate endDate);
}
