package com.lamp.repository;

import com.lamp.entity.CourseStudent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseStudentRepository extends JpaRepository<CourseStudent, Long> {
    List<CourseStudent> findByStudentId(Long studentId);

    List<CourseStudent> findByCourseId(Long courseId);

    List<CourseStudent> findByCourseIdIn(Collection<Long> courseIds);

    boolean existsByCourseIdAndStudentId(Long courseId, Long studentId);

    void deleteByCourseId(Long courseId);
}
