package com.lamp.entity;

import lombok.Data;

import javax.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Entity
@Table(name = "course", indexes = {
        @Index(name = "idx_teacher_semester", columnList = "teacherId,semester"),
        @Index(name = "idx_weekday_status", columnList = "weekday,status")
})
public class Course {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 32)
    private String courseCode;

    @Column(nullable = false, length = 64)
    private String courseName;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false, length = 32)
    private String semester;

    @Column(nullable = false)
    private LocalDate termStartDate;

    @Column(nullable = false, length = 32)
    private String weeks;

    @Column(nullable = false)
    private Integer weekday;

    @Column(nullable = false)
    private LocalTime startTime;

    @Column(nullable = false)
    private LocalTime endTime;

    @Column(length = 64)
    private String location;

    @Column(columnDefinition = "text")
    private String remark;

    @Column(nullable = false, length = 20)
    private String status = "active";

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
