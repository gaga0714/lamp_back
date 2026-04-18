package com.lamp.entity;

import javax.persistence.*;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Entity
@Table(name = "lab_booking", indexes = {
        @Index(name = "idx_lab_date_slot", columnList = "labId,date,slot")
})
public class LabBooking {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private Long labId;
    @Column(nullable = false)
    private Long userId;
    @Column(nullable = false)
    private LocalDate date;
    /** 08:00-10:00 等 */
    @Column(nullable = false, length = 32)
    private String slot;
    @Column(columnDefinition = "text")
    private String purpose;
    /** pending-待审批 approved-已通过 checked_in-已签到 completed-已完成 no_show-爽约 rejected-已拒绝 cancelled-已取消 */
    @Column(nullable = false, length = 20)
    private String status = "pending";
    @Column(columnDefinition = "text")
    private String approveRemark;
    private LocalDateTime checkInTime;
    private LocalDateTime checkOutTime;
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
