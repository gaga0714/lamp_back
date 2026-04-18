package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.exception.BusinessException;
import com.lamp.security.UserContext;
import com.lamp.service.CourseService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/course")
public class CourseController {

    private final CourseService courseService;

    public CourseController(CourseService courseService) {
        this.courseService = courseService;
    }

    @GetMapping("/student/schedule")
    public Result<Map<String, Object>> studentSchedule(@RequestParam(required = false) String weekStart) {
        Long userId = requireRole("student");
        LocalDate date = weekStart == null || weekStart.trim().isEmpty() ? null : LocalDate.parse(weekStart);
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", courseService.getStudentSchedule(userId, date));
        return Result.ok(data);
    }

    @GetMapping("/teacher/schedule")
    public Result<Map<String, Object>> teacherSchedule(@RequestParam(required = false) String weekStart) {
        Long userId = requireRole("teacher");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", courseService.getTeacherSchedule(userId, parseDate(weekStart)));
        return Result.ok(data);
    }

    @GetMapping("/student/options")
    public Result<Map<String, Object>> studentOptions(@RequestParam(required = false) String date) {
        Long userId = requireRole("student");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", courseService.getStudentCourseOptions(userId, parseDate(date)));
        return Result.ok(data);
    }

    @GetMapping("/student/leave-options")
    public Result<Map<String, Object>> studentLeaveOptions(@RequestParam(required = false) String date) {
        Long userId = requireRole("student");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", courseService.getStudentLeaveCourseOptions(userId, parseDate(date)));
        return Result.ok(data);
    }

    @GetMapping("/teacher/options")
    public Result<Map<String, Object>> teacherOptions() {
        Long userId = requireRole("teacher");
        Map<String, Object> data = new HashMap<String, Object>();
        data.put("list", courseService.getTeacherCourseOptions(userId));
        return Result.ok(data);
    }

    @GetMapping("/{id}/students")
    public Result<Map<String, Object>> students(@PathVariable Long id, @RequestParam(required = false) String courseDate) {
        requireRole("teacher");
        return Result.ok(courseService.getCourseStudents(id, parseDate(courseDate)));
    }

    @GetMapping("/manage/list")
    public Result<Map<String, Object>> manageList(@RequestParam(defaultValue = "1") int page,
                                                  @RequestParam(defaultValue = "10") int pageSize,
                                                  @RequestParam(required = false) String keyword) {
        rejectLabAdminAccess();
        return Result.ok(new HashMap<String, Object>());
    }

    @GetMapping("/attendance/teacher/list")
    public Result<Map<String, Object>> teacherAttendance(@RequestParam(defaultValue = "1") int page,
                                                         @RequestParam(defaultValue = "10") int pageSize,
                                                         @RequestParam(required = false) Long courseId,
                                                         @RequestParam(required = false) String date) {
        Long teacherId = requireRole("teacher");
        return Result.ok(courseService.getTeacherAttendance(teacherId, courseId, parseDate(date), page, pageSize));
    }

    @GetMapping("/attendance/admin/list")
    public Result<Map<String, Object>> adminAttendance(@RequestParam(defaultValue = "1") int page,
                                                       @RequestParam(defaultValue = "10") int pageSize,
                                                       @RequestParam(required = false) String keyword,
                                                       @RequestParam(required = false) String date) {
        rejectLabAdminAccess();
        return Result.ok(new HashMap<String, Object>());
    }

    private void rejectLabAdminAccess() {
        requireRole("admin");
        throw new BusinessException(403, "实验室管理员无权限");
    }

    private LocalDate parseDate(String value) {
        return value == null || value.trim().isEmpty() ? null : LocalDate.parse(value);
    }

    private Long requireRole(String... roles) {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        String currentRole = UserContext.getRole();
        for (String role : roles) {
            if (role.equals(currentRole)) {
                return userId;
            }
        }
        throw new BusinessException(403, "无权限");
    }
}
