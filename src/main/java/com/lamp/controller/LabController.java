package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.entity.Lab;
import com.lamp.entity.LabBooking;
import com.lamp.entity.User;
import com.lamp.security.UserContext;
import com.lamp.service.LabService;
import com.lamp.service.UserService;
import org.springframework.data.domain.Page;
import org.springframework.web.bind.annotation.*;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/lab")
public class LabController {

    private final LabService labService;
    private final UserService userService;

    public LabController(LabService labService, UserService userService) {
        this.labService = labService;
        this.userService = userService;
    }

    @GetMapping("/list")
    public Result<Map<String, Object>> list(@RequestParam(required = false) String keyword) {
        List<Lab> list = labService.listLabs(keyword);
        List<Map<String, Object>> items = list.stream().map(this::labToMap).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", items);
        return Result.ok(data);
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Lab lab = labService.getDetail(id);
        return Result.ok(labToMap(lab));
    }

    @GetMapping("/{id}/slots")
    public Result<List<String>> slots(@PathVariable Long id, @RequestParam(required = false) String date) {
        java.time.LocalDate d = date != null && !date.isEmpty() ? java.time.LocalDate.parse(date) : java.time.LocalDate.now();
        List<String> slots = labService.getSlots(id, d);
        return Result.ok(slots);
    }

    @PostMapping("/booking")
    public Result<Map<String, Object>> createBooking(@RequestBody Map<String, Object> body) {
        Long labId = body.get("labId") instanceof Number ? ((Number) body.get("labId")).longValue() : null;
        String dateStr = (String) body.get("date");
        String slot = (String) body.get("slot");
        String purpose = (String) body.get("purpose");
        if (labId == null || dateStr == null || slot == null || purpose == null) {
            return Result.fail("请填写完整");
        }
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr);
        Long userId = UserContext.getUserId();
        LabBooking booking = labService.createBooking(userId, labId, date, slot, purpose);
        Map<String, Object> data = new HashMap<>();
        data.put("id", booking.getId());
        return Result.ok(data);
    }

    @GetMapping("/booking/my")
    public Result<Map<String, Object>> myBookings(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Long userId = UserContext.getUserId();
        Page<LabBooking> pg = labService.getMyBookings(userId, page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(b -> bookingToMap(b, true)).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PutMapping("/booking/{id}/cancel")
    public Result<Void> cancelBooking(@PathVariable Long id) {
        Long userId = UserContext.getUserId();
        labService.cancelBooking(id, userId);
        return Result.ok();
    }

    @GetMapping("/booking/approve/list")
    public Result<Map<String, Object>> approveList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<LabBooking> pg = labService.getApproveList(page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(b -> bookingToMap(b, false)).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PutMapping("/booking/{id}/approve")
    public Result<Map<String, Object>> approveBooking(@PathVariable Long id, @RequestBody Map<String, Object> body) {
        Boolean approved = body.get("approved") instanceof Boolean ? (Boolean) body.get("approved") : Boolean.TRUE;
        String remark = body.get("remark") != null ? body.get("remark").toString() : "";
        LabBooking booking = labService.approveBooking(id, approved, remark);
        return Result.ok(bookingToMap(booking, false));
    }

    @GetMapping("/manage/list")
    public Result<Map<String, Object>> manageList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {
        Page<Lab> pg = labService.getManageList(page, pageSize);
        List<Map<String, Object>> list = pg.getContent().stream().map(this::labToMap).collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PostMapping("/manage")
    public Result<Map<String, Object>> saveLab(@RequestBody Map<String, Object> body) {
        Long id = body.get("id") instanceof Number ? ((Number) body.get("id")).longValue() : null;
        String name = (String) body.get("name");
        String location = (String) body.get("location");
        String description = (String) body.get("description");
        Integer capacity = body.get("capacity") != null ? ((Number) body.get("capacity")).intValue() : null;
        String status = (String) body.get("status");
        Lab lab = labService.saveLab(id, name, location, description, capacity, status);
        return Result.ok(labToMap(lab));
    }

    @PutMapping("/manage")
    public Result<Map<String, Object>> updateLab(@RequestBody Map<String, Object> body) {
        return saveLab(body);
    }

    @DeleteMapping("/manage/{id}")
    public Result<Void> deleteLab(@PathVariable Long id) {
        labService.deleteLab(id);
        return Result.ok();
    }

    private Map<String, Object> labToMap(Lab l) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", l.getId());
        m.put("name", l.getName());
        m.put("location", l.getLocation());
        m.put("description", l.getDescription());
        m.put("capacity", l.getCapacity());
        m.put("status", l.getStatus());
        m.put("openTime", l.getOpenTime());
        return m;
    }

    private static final Map<String, String> BOOKING_STATUS_MAP = new HashMap<String, String>() {{
        put("pending", "待审批");
        put("approved", "已通过");
        put("rejected", "已拒绝");
        put("used", "已使用");
        put("cancelled", "已取消");
    }};

    private Map<String, Object> bookingToMap(LabBooking b, boolean my) {
        Map<String, Object> m = new HashMap<>();
        m.put("id", b.getId());
        m.put("labId", b.getLabId());
        Lab lab = labService.getDetail(b.getLabId());
        m.put("labName", lab.getName());
        m.put("date", b.getDate() != null ? b.getDate().toString() : null);
        m.put("slot", b.getSlot());
        m.put("purpose", b.getPurpose());
        m.put("status", BOOKING_STATUS_MAP.getOrDefault(b.getStatus(), b.getStatus()));
        if (!my) {
            User user = userService.getById(b.getUserId());
            m.put("userName", user != null ? user.getName() : "");
        }
        return m;
    }
}
