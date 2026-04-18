package com.lamp.service;

import com.lamp.entity.AttendanceRecord;
import com.lamp.entity.Course;
import com.lamp.entity.CourseAttendance;
import com.lamp.entity.LeaveApply;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.repository.AttendanceRecordRepository;
import com.lamp.repository.CourseAttendanceRepository;
import com.lamp.repository.CourseRepository;
import com.lamp.repository.CourseStudentRepository;
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
import java.util.Arrays;
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
    private final CourseRepository courseRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final CourseAttendanceRepository courseAttendanceRepository;
    private final CourseService courseService;

    public AttendanceService(AttendanceRecordRepository attendanceRecordRepository,
                             LeaveApplyRepository leaveApplyRepository,
                             UserRepository userRepository,
                             CourseRepository courseRepository,
                             CourseStudentRepository courseStudentRepository,
                             CourseAttendanceRepository courseAttendanceRepository,
                             CourseService courseService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveApplyRepository = leaveApplyRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.courseAttendanceRepository = courseAttendanceRepository;
        this.courseService = courseService;
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
    public LeaveApply applyLeave(Long userId, Long courseId, LocalDate courseDate, String type, String reason) {
        if (courseId == null || courseDate == null) {
            throw new BusinessException("请选择请假的课程和日期");
        }
        if (courseDate.isBefore(LocalDate.now())) {
            throw new BusinessException("不能申请今天之前的课程请假");
        }
        Course course = courseRepository.findById(courseId).orElseThrow(() -> new BusinessException("课程不存在"));
        if (!courseStudentRepository.existsByCourseIdAndStudentId(courseId, userId)) {
            throw new BusinessException("只能对自己的课程发起请假");
        }
        if (!courseService.isCourseActiveOnDate(course, courseDate)) {
            throw new BusinessException("当前日期不存在该课程安排");
        }
        if (leaveApplyRepository.existsByUserIdAndCourseIdAndCourseDateAndStatusIn(
                userId, courseId, courseDate, Arrays.asList("待审批", "已通过"))) {
            throw new BusinessException("该课程已存在请假申请");
        }
        LeaveApply apply = new LeaveApply();
        apply.setUserId(userId);
        apply.setCourseId(courseId);
        apply.setCourseDate(courseDate);
        apply.setType(type);
        apply.setStartTime(LocalDateTime.of(courseDate, course.getStartTime()));
        apply.setEndTime(LocalDateTime.of(courseDate, course.getEndTime()));
        apply.setReason(reason);
        apply.setStatus("待审批");
        return leaveApplyRepository.save(apply);
    }

    public Page<LeaveApply> getMyLeaveList(Long userId, int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return leaveApplyRepository.findByUserIdOrderByCreateTimeDesc(userId, p);
    }

    public Page<LeaveApply> getLeaveManageList(Long teacherId, int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        List<Long> courseIds = courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active")
                .stream()
                .map(Course::getId)
                .collect(Collectors.toList());
        if (courseIds.isEmpty()) {
            return Page.empty(p);
        }
        return leaveApplyRepository.findByStatusAndCourseIdInOrderByCreateTimeDesc("待审批", courseIds, p);
    }

    @Transactional
    public void cancelLeave(Long id, Long userId) {
        LeaveApply apply = leaveApplyRepository.findById(id).orElseThrow(() -> new BusinessException("请假记录不存在"));
        if (!apply.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (!"待审批".equals(apply.getStatus())) {
            throw new BusinessException("仅待审批的请假可撤回");
        }
        leaveApplyRepository.delete(apply);
    }

    @Transactional
    public LeaveApply approveLeave(Long id, Long teacherId, boolean approved, String remark) {
        LeaveApply apply = leaveApplyRepository.findById(id).orElseThrow(() -> new BusinessException("请假记录不存在"));
        User applicant = userRepository.findById(apply.getUserId()).orElseThrow(() -> new BusinessException("用户不存在"));
        if (!"student".equals(applicant.getRole())) {
            throw new BusinessException("仅可审批学生请假申请");
        }
        if (apply.getCourseId() == null || apply.getCourseDate() == null) {
            throw new BusinessException("当前请假记录未绑定课程，无法审批");
        }
        Course course = courseRepository.findById(apply.getCourseId()).orElseThrow(() -> new BusinessException("课程不存在"));
        if (!teacherId.equals(course.getTeacherId())) {
            throw new BusinessException(403, "只能审批本人授课课程的请假");
        }
        if (!"待审批".equals(apply.getStatus())) {
            throw new BusinessException("该申请已处理");
        }
        Optional<CourseAttendance> existingAttendance = courseAttendanceRepository
                .findByCourseIdAndStudentIdAndCourseDate(apply.getCourseId(), apply.getUserId(), apply.getCourseDate());
        if (approved && existingAttendance.isPresent()) {
            String status = existingAttendance.get().getStatus();
            if ("已签到".equals(status) || "迟到".equals(status)) {
                throw new BusinessException("该课程已完成签到，不能再审批为请假");
            }
        }
        apply.setStatus(approved ? "已通过" : "已驳回");
        apply.setApproveRemark(remark);
        LeaveApply saved = leaveApplyRepository.save(apply);
        if (approved) {
            CourseAttendance attendance = existingAttendance.isPresent() ? existingAttendance.get() : new CourseAttendance();
            attendance.setCourseId(apply.getCourseId());
            attendance.setStudentId(apply.getUserId());
            attendance.setCourseDate(apply.getCourseDate());
            attendance.setStatus("请假");
            attendance.setRemark(remark);
            attendance.setCheckInTime(null);
            courseAttendanceRepository.save(attendance);
        }
        return saved;
    }
}
