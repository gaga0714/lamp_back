package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.entity.Course;
import com.lamp.entity.LeaveApply;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.security.UserContext;
import com.lamp.service.AttendanceService;
import com.lamp.service.CourseService;
import com.lamp.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final CourseService courseService;
    private final UserService userService;

    public AttendanceController(AttendanceService attendanceService, CourseService courseService, UserService userService) {
        this.attendanceService = attendanceService;
        this.courseService = courseService;
        this.userService = userService;
    }

    @PostMapping("/check-in")
    public Result<Map<String, Object>> checkIn(@RequestBody Map<String, Object> body) {
        Long userId = requireStudent();
        Long courseId = toLong(body.get("courseId"));
        String courseDateStr = body.get("courseDate") == null ? null : body.get("courseDate").toString();
        if (courseId == null || courseDateStr == null || courseDateStr.trim().isEmpty()) {
            return Result.fail("请先选择课程");
        }
        return Result.ok(courseService.checkIn(userId, courseId, LocalDate.parse(courseDateStr)));
    }

    @GetMapping("/records")
    public Result<Map<String, Object>> records(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate) {
        Long userId = requireStudent();
        LocalDate start = startDate != null && !startDate.isEmpty() ? LocalDate.parse(startDate) : null;
        LocalDate end = endDate != null && !endDate.isEmpty() ? LocalDate.parse(endDate) : null;
        return Result.ok(courseService.getStudentAttendance(userId, start, end, page, pageSize));
    }

    @PostMapping("/leave")
    public Result<Map<String, Object>> applyLeave(@RequestBody Map<String, Object> body) {
        Long userId = requireStudent();
        String type = (String) body.get("type");
        Long courseId = toLong(body.get("courseId"));
        String courseDateStr = body.get("courseDate") == null ? null : body.get("courseDate").toString();
        String reason = (String) body.get("reason");
        if (type == null || courseId == null || courseDateStr == null || reason == null) {
            return Result.fail("请填写完整");
        }
        LeaveApply apply = attendanceService.applyLeave(userId, courseId, LocalDate.parse(courseDateStr), type, reason);
        Map<String, Object> data = new HashMap<>();
        data.put("id", apply.getId());
        return Result.ok(data);
    }

    @GetMapping("/leave/list")
    public Result<Map<String, Object>> leaveList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = requireStudent();
        Page<LeaveApply> pg = attendanceService.getMyLeaveList(userId, page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(this::leaveToMap).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PutMapping("/leave/{id}/cancel")
    public Result<Void> cancelLeave(@PathVariable Long id) {
        Long userId = requireStudent();
        attendanceService.cancelLeave(id, userId);
        return Result.ok();
    }

    @GetMapping("/manage")
    public Result<Map<String, Object>> manageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {
        requireAdmin();
        LocalDate d = date != null && !date.isEmpty() ? LocalDate.parse(date) : null;
        return Result.ok(courseService.getAdminAttendance(keyword, d, page, pageSize));
    }

    @GetMapping("/leave/pending")
    public Result<Map<String, Object>> pendingLeaveList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long teacherId = requireTeacher();
        Page<LeaveApply> pg = attendanceService.getLeaveManageList(teacherId, page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(apply -> {
            Map<String, Object> m = leaveToMap(apply);
            User user = userService.getById(apply.getUserId());
            m.put("username", user.getUsername());
            m.put("name", user.getName());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PutMapping("/leave/{id}/approve")
    public Result<Map<String, Object>> approveLeave(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Long teacherId = requireTeacher();
        Boolean approved = body.get("approved") instanceof Boolean ? (Boolean) body.get("approved") : Boolean.TRUE;
        String remark = body.get("remark") != null ? body.get("remark").toString() : "";
        LeaveApply apply = attendanceService.approveLeave(id, teacherId, approved, remark);
        Map<String, Object> data = leaveToMap(apply);
        User user = userService.getById(apply.getUserId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        return Result.ok(data);
    }

    private Map<String, Object> leaveToMap(LeaveApply l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("type", l.getType());
        m.put("startTime", l.getStartTime() != null ? l.getStartTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        m.put("endTime", l.getEndTime() != null ? l.getEndTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        m.put("reason", l.getReason());
        m.put("status", l.getStatus());
        m.put("approveRemark", l.getApproveRemark());
        m.put("createTime", l.getCreateTime() != null ? l.getCreateTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        m.put("courseId", l.getCourseId());
        m.put("courseDate", l.getCourseDate() != null ? l.getCourseDate().toString() : null);
        if (l.getCourseId() != null) {
            Course course = courseService.getCourse(l.getCourseId());
            User teacher = userService.getById(course.getTeacherId());
            m.put("courseCode", course.getCourseCode());
            m.put("courseName", course.getCourseName());
            m.put("courseTime", course.getStartTime() + "-" + course.getEndTime());
            m.put("teacherName", teacher.getName());
            m.put("location", course.getLocation());
        }
        return m;
    }

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }

    private Long requireRole(String... roles) {
        Long userId = requireLogin();
        String currentRole = UserContext.getRole();
        for (String role : roles) {
            if (role.equals(currentRole)) {
                return userId;
            }
        }
        throw new BusinessException(403, "无权限");
    }

    private Long requireStudent() {
        return requireRole("student");
    }

    private Long requireTeacher() {
        requireRole("teacher");
        return requireLogin();
    }

    private void requireAdmin() {
        requireRole("admin");
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        try {
            return Long.parseLong(value.toString());
        } catch (Exception ex) {
            return null;
        }
    }
}
