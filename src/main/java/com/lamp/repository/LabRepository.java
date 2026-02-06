package com.lamp.repository;

import com.lamp.entity.Lab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface LabRepository extends JpaRepository<Lab, Long> {
    List<Lab> findByNameContainingAndStatus(String name, String status);

    List<Lab> findByNameContaining(String name);

    default List<Lab> listByKeyword(String keyword) {
        if (keyword != null && !keyword.trim().isEmpty()) {
            return findByNameContaining(keyword);
        }
        return findAll();
    }

    Page<Lab> findAllByOrderByIdDesc(Pageable pageable);
}
