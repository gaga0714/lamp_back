package com.lamp.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab")
public class Lab {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false, length = 64)
    private String name;
    @Column(length = 128)
    private String location;
    @Column(columnDefinition = "text")
    private String description;
    private Integer capacity;
    /** available-可预约 maintenance-维护中 */
    @Column(nullable = false, length = 20)
    private String status = "available";
    @Column(length = 64)
    private String openTime;
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
