package com.lamp.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "course_student", uniqueConstraints = {
        @UniqueConstraint(name = "uk_course_student", columnNames = {"courseId", "studentId"})
}, indexes = {
        @Index(name = "idx_student_id", columnList = "studentId")
})
public class CourseStudent {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long studentId;

    private LocalDateTime createTime;

    @PrePersist
    public void prePersist() {
        createTime = LocalDateTime.now();
    }
}
