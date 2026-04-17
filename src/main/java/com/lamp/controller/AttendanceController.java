package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.entity.AttendanceRecord;
import com.lamp.entity.LeaveApply;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.security.UserContext;
import com.lamp.service.AttendanceService;
import com.lamp.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final UserService userService;

    public AttendanceController(AttendanceService attendanceService, UserService userService) {
        this.attendanceService = attendanceService;
        this.userService = userService;
    }

    @PostMapping("/check-in")
    public Result<Map<String, Object>> checkIn(@RequestBody Map<String, String> body) {
        Long userId = requireStudent();
        String type = body.get("type");
        if (type == null || (!"in".equals(type) && !"out".equals(type))) {
            return Result.fail("type 为 in 或 out");
        }
        AttendanceRecord record = attendanceService.checkIn(userId, type);
        Map<String, Object> data = new HashMap<>();
        data.put("type", type);
        data.put("date", record.getDate().toString());
        data.put("checkInTime", record.getCheckInTime() != null ? record.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        data.put("checkOutTime", record.getCheckOutTime() != null ? record.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        return Result.ok(data);
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
        Page<AttendanceRecord> pg = attendanceService.getMyRecords(userId, start, end, page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(this::recordToMap).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PostMapping("/leave")
    public Result<Map<String, Object>> applyLeave(@RequestBody Map<String, Object> body) {
        Long userId = requireStudent();
        String type = (String) body.get("type");
        String startTimeStr = (String) body.get("startTime");
        String endTimeStr = (String) body.get("endTime");
        String reason = (String) body.get("reason");
        if (type == null || startTimeStr == null || endTimeStr == null || reason == null) {
            return Result.fail("请填写完整");
        }
        LocalDateTime startTime = LocalDateTime.parse(startTimeStr.replace(" ", "T"));
        LocalDateTime endTime = LocalDateTime.parse(endTimeStr.replace(" ", "T"));
        LeaveApply apply = attendanceService.applyLeave(userId, type, startTime, endTime, reason);
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

    @GetMapping("/manage")
    public Result<Map<String, Object>> manageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String date,
            @RequestParam(required = false) String keyword) {
        requireAdmin();
        LocalDate d = date != null && !date.isEmpty() ? LocalDate.parse(date) : null;
        Page<AttendanceRecord> pg = attendanceService.getManageList(d, keyword, page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(rec -> {
            Map<String, Object> m = recordToMap(rec);
            User user = userService.getById(rec.getUserId());
            m.put("username", user.getUsername());
            m.put("name", user.getName());
            return m;
        }).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @GetMapping("/leave/pending")
    public Result<Map<String, Object>> pendingLeaveList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        requireTeacher();
        Page<LeaveApply> pg = attendanceService.getLeaveManageList(page, pageSize);
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
        requireTeacher();
        Boolean approved = body.get("approved") instanceof Boolean ? (Boolean) body.get("approved") : Boolean.TRUE;
        String remark = body.get("remark") != null ? body.get("remark").toString() : "";
        LeaveApply apply = attendanceService.approveLeave(id, approved, remark);
        Map<String, Object> data = leaveToMap(apply);
        User user = userService.getById(apply.getUserId());
        data.put("username", user.getUsername());
        data.put("name", user.getName());
        return Result.ok(data);
    }

    private Map<String, Object> recordToMap(AttendanceRecord r) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", r.getId());
        m.put("date", r.getDate() != null ? r.getDate().toString() : null);
        m.put("checkInTime", r.getCheckInTime() != null ? r.getCheckInTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        m.put("checkOutTime", r.getCheckOutTime() != null ? r.getCheckOutTime().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")) : null);
        m.put("status", r.getStatus());
        return m;
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

    private void requireTeacher() {
        requireRole("teacher");
    }

    private void requireAdmin() {
        requireRole("admin");
    }
}
