package com.lamp.repository;

import com.lamp.entity.LeaveApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;

public interface LeaveApplyRepository extends JpaRepository<LeaveApply, Long> {
    Page<LeaveApply> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<LeaveApply> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    Page<LeaveApply> findByStatusAndCourseIdInOrderByCreateTimeDesc(String status, Collection<Long> courseIds, Pageable pageable);

    boolean existsByUserIdAndCourseIdAndCourseDateAndStatusIn(Long userId, Long courseId, LocalDate courseDate, Collection<String> statuses);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByStatus(String status);

    long countByStatusAndCourseIdIn(String status, Collection<Long> courseIds);
}
