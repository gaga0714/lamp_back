package com.lamp.service;

import com.lamp.entity.AttendanceRecord;
import com.lamp.entity.LeaveApply;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.repository.AttendanceRecordRepository;
import com.lamp.repository.LeaveApplyRepository;
import com.lamp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class AttendanceService {

    private static final LocalTime LATE_THRESHOLD = LocalTime.of(9, 0);
    private static final LocalTime EARLY_LEAVE_THRESHOLD = LocalTime.of(17, 0);

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveApplyRepository leaveApplyRepository;
    private final UserRepository userRepository;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             LeaveApplyRepository leaveApplyRepository,
                             UserRepository userRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveApplyRepository = leaveApplyRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public AttendanceRecord checkIn(Long userId, String type) {
        LocalDate today = LocalDate.now();
        Optional<AttendanceRecord> opt = attendanceRecordRepository.findByUserIdAndDate(userId, today);
        AttendanceRecord record;
        if (!opt.isPresent()) {
            if ("out".equals(type)) {
                throw new BusinessException("今日未签到，请先签到");
            }
            record = new AttendanceRecord();
            record.setUserId(userId);
            record.setDate(today);
            LocalDateTime now = LocalDateTime.now();
            record.setCheckInTime(now);
            record.setStatus(now.toLocalTime().isAfter(LATE_THRESHOLD) ? "迟到" : "正常");
            record = attendanceRecordRepository.save(record);
        } else {
            record = opt.get();
            if ("in".equals(type)) {
                throw new BusinessException("今日已签到，请签退");
            }
            LocalDateTime now = LocalDateTime.now();
            record.setCheckOutTime(now);
            if (record.getStatus() == null) record.setStatus("正常");
            if (now.toLocalTime().isBefore(EARLY_LEAVE_THRESHOLD) && "正常".equals(record.getStatus())) {
                record.setStatus("早退");
            }
            record = attendanceRecordRepository.save(record);
        }
        return record;
    }

    public Page<AttendanceRecord> getMyRecords(Long userId, LocalDate startDate, LocalDate endDate, int page, int pageSize) {
        if (startDate == null) startDate = LocalDate.now().minusMonths(1);
        if (endDate == null) endDate = LocalDate.now();
        Pageable p = PageRequest.of(page - 1, pageSize);
        return attendanceRecordRepository.findByUserIdAndDateBetweenOrderByDateDesc(userId, startDate, endDate, p);
    }

    public Page<AttendanceRecord> getManageList(LocalDate date, String keyword, int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        if (date == null) date = LocalDate.now();
        if (keyword != null && !keyword.trim().isEmpty()) {
            List<Long> userIds = userRepository.findByUsernameContainingOrNameContaining(keyword, keyword)
                    .stream().map(u -> u.getId()).collect(Collectors.toList());
            if (userIds.isEmpty()) {
                return Page.empty(p);
            }
            return attendanceRecordRepository.findByDateAndUserIdInOrderByIdDesc(date, userIds, p);
        }
        return attendanceRecordRepository.findByDateOrderByIdDesc(date, p);
    }

    @Transactional
    public LeaveApply applyLeave(Long userId, String type, LocalDateTime startTime, LocalDateTime endTime, String reason) {
        LeaveApply apply = new LeaveApply();
        apply.setUserId(userId);
        apply.setType(type);
        apply.setStartTime(startTime);
        apply.setEndTime(endTime);
        apply.setReason(reason);
        apply.setStatus("待审批");
        return leaveApplyRepository.save(apply);
    }

    public Page<LeaveApply> getMyLeaveList(Long userId, int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return leaveApplyRepository.findByUserIdOrderByCreateTimeDesc(userId, p);
    }

    public Page<LeaveApply> getLeaveManageList(int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return leaveApplyRepository.findByStatusOrderByCreateTimeDesc("待审批", p);
    }

    @Transactional
    public LeaveApply approveLeave(Long id, boolean approved, String remark) {
        LeaveApply apply = leaveApplyRepository.findById(id).orElseThrow(() -> new BusinessException("请假记录不存在"));
        User applicant = userRepository.findById(apply.getUserId()).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!"student".equals(applicant.getRole())) {
            throw new BusinessException("仅可审批学生请假申请");
        }
        if (!"待审批".equals(apply.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        apply.setStatus(approved ? "已通过" : "已驳回");
        apply.setApproveRemark(remark);
        return leaveApplyRepository.save(apply);
    }
}
