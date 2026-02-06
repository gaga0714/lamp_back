package com.lamp.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "leave_apply")
public class LeaveApply {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long userId;
    /** personal-事假 sick-病假 other-其他 */
    @Column(nullable = false, length = 20)
    private String type;
    @Column(nullable = false)
    private LocalDateTime startTime;
    @Column(nullable = false)
    private LocalDateTime endTime;
    @Column(columnDefinition = "text")
    private String reason;
    /** 待审批 已通过 已驳回 */
    @Column(nullable = false, length = 16)
    private String status = "待审批";
    @Column(columnDefinition = "text")
    private String approveRemark;
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
