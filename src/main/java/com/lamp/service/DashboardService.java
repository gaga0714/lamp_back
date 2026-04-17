package com.lamp.service;

import com.lamp.repository.AttendanceRecordRepository;
import com.lamp.repository.LabBookingRepository;
import com.lamp.repository.LabRepository;
import com.lamp.repository.LeaveApplyRepository;
import com.lamp.repository.UserRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class DashboardService {

    private final AttendanceRecordRepository attendanceRecordRepository;
    private final LeaveApplyRepository leaveApplyRepository;
    private final LabBookingRepository labBookingRepository;
    private final LabRepository labRepository;
    private final UserRepository userRepository;

    public DashboardService(AttendanceRecordRepository attendanceRecordRepository,
                            LeaveApplyRepository leaveApplyRepository,
                            LabBookingRepository labBookingRepository,
                            LabRepository labRepository,
                            UserRepository userRepository) {
        this.attendanceRecordRepository = attendanceRecordRepository;
        this.leaveApplyRepository = leaveApplyRepository;
        this.labBookingRepository = labBookingRepository;
        this.labRepository = labRepository;
        this.userRepository = userRepository;
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
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long attendanceDays = attendanceRecordRepository.countByUserIdAndDateBetween(userId, monthStart, today);
        long bookingTotal = labBookingRepository.countByUserId(userId);
        long leaveTotal = leaveApplyRepository.countByUserId(userId);
        long todoTotal = labBookingRepository.countByUserIdAndStatus(userId, "pending")
                + leaveApplyRepository.countByUserIdAndStatus(userId, "待审批");

        data.put("card1Label", "本月签到天数");
        data.put("card1Value", attendanceDays);
        data.put("card2Label", "我的预约");
        data.put("card2Value", bookingTotal);
        data.put("card3Label", "我的请假");
        data.put("card3Value", leaveTotal);
        data.put("card4Label", "待办事项");
        data.put("card4Value", todoTotal);
        return data;
    }

    private Map<String, Object> buildTeacherStats(Long userId, Map<String, Object> data) {
        LocalDate today = LocalDate.now();
        LocalDate monthStart = today.withDayOfMonth(1);

        long pendingLeaveTotal = leaveApplyRepository.countByStatus("待审批");
        long bookingTotal = labBookingRepository.countByUserId(userId);
        long bookingThisMonth = labBookingRepository.countByUserIdAndDateBetween(userId, monthStart, today);
        long labTotal = labRepository.count();

        data.put("card1Label", "待审批请假");
        data.put("card1Value", pendingLeaveTotal);
        data.put("card2Label", "我的预约");
        data.put("card2Value", bookingTotal);
        data.put("card3Label", "本月预约数");
        data.put("card3Value", bookingThisMonth);
        data.put("card4Label", "实验室总数");
        data.put("card4Value", labTotal);
        return data;
    }

    private Map<String, Object> buildAdminStats(Map<String, Object> data) {
        LocalDate today = LocalDate.now();

        data.put("card1Label", "今日考勤记录");
        data.put("card1Value", attendanceRecordRepository.countByDate(today));
        data.put("card2Label", "待审批预约");
        data.put("card2Value", labBookingRepository.countByStatus("pending"));
        data.put("card3Label", "实验室总数");
        data.put("card3Value", labRepository.count());
        data.put("card4Label", "用户总数");
        data.put("card4Value", userRepository.count());
        return data;
    }
}
