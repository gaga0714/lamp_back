package com.lamp.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "sys_user")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false, length = 64)
    private String username;
    @Column(nullable = false, length = 64)
    private String password;
    @Column(nullable = false, length = 32)
    private String name;
    @Column(length = 20)
    private String phone;
    @Column(length = 64)
    private String email;
    /** student, teacher, labAdmin, admin */
    @Column(nullable = false, length = 20)
    private String role;
    /** 1 正常 0 禁用 */
    @Column(nullable = false)
    private Integer status = 1;
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
