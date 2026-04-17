package com.lamp.service;

import com.lamp.repository.AttendanceRecordRepository;
import com.lamp.repository.CourseRepository;
import com.lamp.repository.LabBookingRepository;
import com.lamp.repository.LabRepository;
import com.lamp.repository.LeaveApplyRepository;
import com.lamp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class DashboardService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveApplyRepository leaveApplyRepository;
    private final LabBookingRepository labBookingRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseService courseService;

    public DashboardService(AttendanceRecordRepository attendanceRecordRepository,
                            LeaveApplyRepository leaveApplyRepository,
                            LabBookingRepository labBookingRepository,
                            LabRepository labRepository,
                            UserRepository userRepository,
                            CourseRepository courseRepository,
                            CourseService courseService) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveApplyRepository = leaveApplyRepository;
        this.labBookingRepository = labBookingRepository;
        this.labRepository = labRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseService = courseService;
    }

    public Map<String, Object> getStats(Long userId, String role) {
        Map<String, Object> data = new HashMap<>();
        data.put("role", role);

        if ("student".equals(role)) {
            return buildStudentStats(userId, data);
        }
        if ("teacher".equals(role)) {
            return buildTeacherStats(userId, data);
        }
        if ("admin".equals(role)) {
            return buildAdminStats(data);
        }
        return data;
    }

    private Map<String, Object> buildStudentStats(Long userId, Map<String, Object> data) {
        long weekCourses = courseService.countStudentCoursesThisWeek(userId);
        long pendingCheckIn = courseService.countStudentPendingCheckIn(userId);
        long leaveTotal = leaveApplyRepository.countByUserId(userId);
        long bookingTotal = labBookingRepository.countByUserId(userId);

        data.put("card1Label", "本周课程数");
        data.put("card1Value", weekCourses);
        data.put("card2Label", "待签到课程");
        data.put("card2Value", pendingCheckIn);
        data.put("card3Label", "我的课程请假");
        data.put("card3Value", leaveTotal);
        data.put("card4Label", "我的预约");
        data.put("card4Value", bookingTotal);
        return data;
    }

    private Map<String, Object> buildTeacherStats(Long userId, Map<String, Object> data) {
        List<Long> courseIds = new ArrayList<Long>();
        courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(userId, "active")
                .forEach(course -> courseIds.add(course.getId()));
        long pendingLeaveTotal = courseIds.isEmpty() ? 0 : leaveApplyRepository.countByStatusAndCourseIdIn("待审批", courseIds);
        long bookingTotal = labBookingRepository.countByUserId(userId);
        long weekCourses = courseService.countTeacherCoursesThisWeek(userId);
        long todayCourses = courseService.countTeacherTodayCourses(userId);

        data.put("card1Label", "本周授课数");
        data.put("card1Value", weekCourses);
        data.put("card2Label", "今日授课");
        data.put("card2Value", todayCourses);
        data.put("card3Label", "待审批课程请假");
        data.put("card3Value", pendingLeaveTotal);
        data.put("card4Label", "我的预约");
        data.put("card4Value", bookingTotal);
        return data;
    }

    private Map<String, Object> buildAdminStats(Map<String, Object> data) {
        data.put("card1Label", "今日课程考勤");
        data.put("card1Value", courseService.countAdminTodayCourseAttendances());
        data.put("card2Label", "待审批预约");
        data.put("card2Value", labBookingRepository.countByStatus("pending"));
        data.put("card3Label", "课程总数");
        data.put("card3Value", courseRepository.count());
        data.put("card4Label", "用户总数");
        data.put("card4Value", userRepository.count());
        return data;
    }
}
