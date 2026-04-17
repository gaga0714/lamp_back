package com.lamp.repository;

import com.lamp.entity.CourseAttendance;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface CourseAttendanceRepository extends JpaRepository<CourseAttendance, Long> {
    Optional<CourseAttendance> findByCourseIdAndStudentIdAndCourseDate(Long courseId, Long studentId, LocalDate courseDate);

    List<CourseAttendance> findByStudentIdAndCourseDateBetweenOrderByCourseDateDesc(Long studentId, LocalDate startDate, LocalDate endDate);

    List<CourseAttendance> findByCourseIdInAndCourseDate(Collection<Long> courseIds, LocalDate courseDate);

    List<CourseAttendance> findByCourseIdInAndCourseDateBetween(Collection<Long> courseIds, LocalDate startDate, LocalDate endDate);

    List<CourseAttendance> findByCourseDateBetween(LocalDate startDate, LocalDate endDate);

    long countByStudentIdAndStatus(Long studentId, String status);

    long countByCourseDateAndStatus(LocalDate courseDate, String status);
}
