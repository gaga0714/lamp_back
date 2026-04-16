package com.lamp.entity;

import javax.persistence.*;
import lombok.Data;

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
    @Column(length = 255)
    private String equipmentInfo;
    private Integer capacity;
    /** available-可预约 maintenance-维护中 */
    @Column(nullable = false, length = 20)
    private String status = "available";
}
