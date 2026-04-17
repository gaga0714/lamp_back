package com.lamp.repository;

import com.lamp.entity.LeaveApply;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LeaveApplyRepository extends JpaRepository<LeaveApply, Long> {
    Page<LeaveApply> findByUserIdOrderByCreateTimeDesc(Long userId, Pageable pageable);

    Page<LeaveApply> findByStatusOrderByCreateTimeDesc(String status, Pageable pageable);

    long countByUserId(Long userId);

    long countByUserIdAndStatus(Long userId, String status);

    long countByStatus(String status);
}
