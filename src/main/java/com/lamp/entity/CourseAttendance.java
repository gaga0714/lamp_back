package com.lamp.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "course_attendance", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_student_date", columnNames = {"courseId", "studentId", "courseDate"})
}, indexes = {
        @Index(name = "idx_student_date", columnList = "studentId,courseDate"),
        @Index(name = "idx_course_date", columnList = "courseId,courseDate")
})
public class CourseAttendance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private LocalDate courseDate;

    @Column(nullable = false, length = 16)
    private String status = "待签到";

    private LocalDateTime checkInTime;

    @Column(columnDefinition = "text")
    private String remark;

    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    @PrePersist
    public void prePersist() {
        createTime = updateTime = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        updateTime = LocalDateTime.now();
    }
}
