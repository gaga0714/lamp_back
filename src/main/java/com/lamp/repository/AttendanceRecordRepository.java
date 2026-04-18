package com.lamp.repository;

import com.lamp.entity.AttendanceRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface AttendanceRecordRepository extends JpaRepository<AttendanceRecord, Long> {
    Optional<AttendanceRecord> findByUserIdAndDate(Long userId, LocalDate date);

    Page<AttendanceRecord> findByUserIdAndDateBetweenOrderByDateDesc(Long userId, LocalDate start, LocalDate end, Pageable pageable);

    long countByUserIdAndDateBetween(Long userId, LocalDate start, LocalDate end);

    long countByDate(LocalDate date);

    Page<AttendanceRecord> findByDateOrderByIdDesc(LocalDate date, Pageable pageable);

    Page<AttendanceRecord> findByDateAndUserIdInOrderByIdDesc(LocalDate date, List<Long> userIds, Pageable pageable);
}
