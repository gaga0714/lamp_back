package com.lamp.service;

import com.lamp.entity.Lab;
import com.lamp.entity.LabBooking;
import com.lamp.exception.BusinessException;
import com.lamp.repository.LabBookingRepository;
import com.lamp.repository.LabRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Service
public class LabService {

    private static final List<String> SLOTS = Arrays.asList("08:00-10:00", "10:00-12:00", "14:00-16:00", "16:00-18:00");

    private final LabRepository labRepository;
    private final LabBookingRepository labBookingRepository;

    public LabService(LabRepository labRepository, LabBookingRepository labBookingRepository) {
        this.labRepository = labRepository;
        this.labBookingRepository = labBookingRepository;
    }

    public List<Lab> listLabs(String keyword) {
        return labRepository.listByKeyword(keyword);
    }

    public Lab getDetail(Long id) {
        return labRepository.findById(id).orElseThrow(() -> new BusinessException("实验室不存在"));
    }

    public List<String> getSlots(Long labId, LocalDate date) {
        labRepository.findById(labId).orElseThrow(() -> new BusinessException("实验室不存在"));
        return SLOTS;
    }

    @Transactional
    public LabBooking createBooking(Long userId, Long labId, LocalDate date, String slot, String purpose) {
        Lab lab = labRepository.findById(labId).orElseThrow(() -> new BusinessException("实验室不存在"));
        if (!"available".equals(lab.getStatus())) {
            throw new BusinessException("该实验室暂不可预约");
        }
        boolean occupied = labBookingRepository.existsByLabIdAndDateAndSlotAndStatusIn(
                labId, date, slot, Arrays.asList("pending", "approved"));
        if (occupied) {
            throw new BusinessException("该时段已被预约");
        }
        LabBooking booking = new LabBooking();
        booking.setLabId(labId);
        booking.setUserId(userId);
        booking.setDate(date);
        booking.setSlot(slot);
        booking.setPurpose(purpose);
        booking.setStatus("pending");
        return labBookingRepository.save(booking);
    }

    public Page<LabBooking> getMyBookings(Long userId, int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return labBookingRepository.findByUserIdOrderByCreateTimeDesc(userId, p);
    }

    @Transactional
    public LabBooking cancelBooking(Long id, Long userId) {
        LabBooking booking = labBookingRepository.findById(id).orElseThrow(() -> new BusinessException("预约不存在"));
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (!"pending".equals(booking.getStatus())) {
            throw new BusinessException("仅待审批的预约可取消");
        }
        booking.setStatus("cancelled");
        return labBookingRepository.save(booking);
    }

    public Page<LabBooking> getApproveList(int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return labBookingRepository.findByStatusOrderByCreateTimeDesc("pending", p);
    }

    @Transactional
    public LabBooking approveBooking(Long id, boolean approved, String remark) {
        LabBooking booking = labBookingRepository.findById(id).orElseThrow(() -> new BusinessException("预约不存在"));
        if (!"pending".equals(booking.getStatus())) {
            throw new BusinessException("该预约已处理");
        }
        booking.setStatus(approved ? "approved" : "rejected");
        booking.setApproveRemark(remark);
        return labBookingRepository.save(booking);
    }

    public Page<Lab> getManageList(int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return labRepository.findAllByOrderByIdDesc(p);
    }

    @Transactional
    public Lab saveLab(Long id, String name, String description, Integer capacity, String status) {
        Lab lab;
        if (id != null && id > 0) {
            lab = labRepository.findById(id).orElseThrow(() -> new BusinessException("实验室不存在"));
        } else {
            lab = new Lab();
        }
        if (name != null) lab.setName(name);
        if (description != null) lab.setDescription(description);
        if (capacity != null) lab.setCapacity(capacity);
        if (status != null) lab.setStatus(status);
        return labRepository.save(lab);
    }

    @Transactional
    public void deleteLab(Long id) {
        labRepository.deleteById(id);
    }
}
