package com.lamp.repository;

import com.lamp.entity.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;

public interface CourseRepository extends JpaRepository<Course, Long> {
    List<Course> findByTeacherIdOrderByWeekdayAscStartTimeAsc(Long teacherId);

    List<Course> findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(Long teacherId, String status);

    List<Course> findByIdIn(Collection<Long> ids);

    Page<Course> findByCourseNameContainingOrCourseCodeContaining(String courseName, String courseCode, Pageable pageable);
}
