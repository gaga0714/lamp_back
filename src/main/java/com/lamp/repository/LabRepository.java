package com.lamp.repository;

import com.lamp.entity.Lab;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;

import java.util.List;

public interface LabRepository extends JpaRepository<Lab, Long> {

    @Query("SELECT DISTINCT l.name FROM Lab l ORDER BY l.name")
    List<String> findDistinctNames();

    @Query("SELECT DISTINCT l.location FROM Lab l WHERE l.location IS NOT NULL ORDER BY l.location")
    List<String> findDistinctLocations();
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
