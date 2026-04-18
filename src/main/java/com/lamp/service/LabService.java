package com.lamp.service;

import com.lamp.entity.Lab;
import com.lamp.entity.LabBooking;
import com.lamp.exception.BusinessException;
import com.lamp.repository.LabBookingRepository;
import com.lamp.repository.LabRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class LabService {

    private static final List<String> SLOT_BLOCKING_STATUSES = Arrays.asList(
            "pending",
            "approved",
            "checked_in",
            "completed",
            "no_show"
    );
    private static final List<String> EFFECTIVE_STATUSES = Arrays.asList(
            "approved",
            "checked_in",
            "completed",
            "no_show"
    );
    private static final List<String> SLOTS = Arrays.asList(
            "08:00-10:00",
            "10:00-12:00",
            "14:00-16:00",
            "16:00-18:00",
            "18:00-20:00",
            "20:00-22:00"
    );

    private final LabRepository labRepository;
    private final LabBookingRepository labBookingRepository;

    public LabService(LabRepository labRepository, LabBookingRepository labBookingRepository) {
        this.labRepository = labRepository;
        this.labBookingRepository = labBookingRepository;
    }

    public Page<Lab> listLabs(String name, String location, String equipment, Integer minCapacity,
                              boolean onlyAvailable, LocalDate date, String slot, int page, int pageSize) {
        List<Lab> filtered = labRepository.findAll().stream()
                .filter(lab -> isTextMatch(lab.getName(), name))
                .filter(lab -> isTextMatch(lab.getLocation(), location))
                .filter(lab -> isTextMatch(lab.getEquipmentInfo(), equipment))
                .filter(lab -> minCapacity == null || (lab.getCapacity() != null && lab.getCapacity() >= minCapacity))
                .filter(lab -> !onlyAvailable || isLabAvailable(lab, date, slot))
                .collect(Collectors.toList());
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        int total = filtered.size();
        int fromIndex = (int) pageable.getOffset();
        int toIndex = Math.min(fromIndex + pageable.getPageSize(), total);
        List<Lab> pageContent = fromIndex >= total ? new ArrayList<Lab>() : filtered.subList(fromIndex, toIndex);
        return new PageImpl<Lab>(pageContent, pageable, total);
    }

    public Lab getDetail(Long id) {
        return labRepository.findById(id).orElseThrow(() -> new BusinessException("实验室不存在"));
    }

    public List<String> getSlots(Long labId, LocalDate date) {
        labRepository.findById(labId).orElseThrow(() -> new BusinessException("实验室不存在"));
        return SLOTS;
    }

    public boolean isSlotAvailable(Long labId, LocalDate date, String slot) {
        if (date == null || slot == null || slot.trim().isEmpty()) {
            return false;
        }
        Lab lab = getDetail(labId);
        return isLabAvailable(lab, date, slot);
    }

    @Transactional
    public LabBooking createBooking(Long userId, Long labId, LocalDate date, String slot, String purpose) {
        Lab lab = labRepository.findById(labId).orElseThrow(() -> new BusinessException("实验室不存在"));
        if (!"available".equals(lab.getStatus())) {
            throw new BusinessException("该实验室暂不可预约");
        }
        boolean occupied = labBookingRepository.existsByLabIdAndDateAndSlotAndStatusIn(
                labId, date, slot, SLOT_BLOCKING_STATUSES);
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
        Page<LabBooking> result = labBookingRepository.findByUserIdOrderByCreateTimeDesc(userId, p);
        refreshExpiredBookings(result.getContent());
        return result;
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

    @Transactional
    public LabBooking checkInBooking(Long id, Long userId) {
        LabBooking booking = labBookingRepository.findById(id).orElseThrow(() -> new BusinessException("预约不存在"));
        refreshExpiredBooking(booking);
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (!"approved".equals(booking.getStatus())) {
            throw new BusinessException("当前预约不可签到");
        }
        LocalDateTime now = LocalDateTime.now();
        if (!LocalDate.now().equals(booking.getDate())) {
            throw new BusinessException("仅当天预约可签到");
        }
        LocalDateTime signInStart = getSlotStartDateTime(booking).minusMinutes(15);
        LocalDateTime signInEnd = getSlotEndDateTime(booking);
        if (now.isBefore(signInStart)) {
            throw new BusinessException("未到签到时间，请提前15分钟后再签到");
        }
        if (now.isAfter(signInEnd)) {
            throw new BusinessException("当前预约已过签到时间");
        }
        booking.setCheckInTime(now);
        booking.setStatus("checked_in");
        return labBookingRepository.save(booking);
    }

    @Transactional
    public LabBooking checkOutBooking(Long id, Long userId) {
        LabBooking booking = labBookingRepository.findById(id).orElseThrow(() -> new BusinessException("预约不存在"));
        refreshExpiredBooking(booking);
        if (!booking.getUserId().equals(userId)) {
            throw new BusinessException("无权操作");
        }
        if (!"checked_in".equals(booking.getStatus()) || booking.getCheckInTime() == null) {
            throw new BusinessException("当前预约不可签退");
        }
        booking.setCheckOutTime(LocalDateTime.now());
        booking.setStatus("completed");
        return labBookingRepository.save(booking);
    }

    @Transactional
    public Map<String, Object> getUsageStats(LocalDate startDate, LocalDate endDate, Long labId, int page, int pageSize) {
        LocalDate actualStart = startDate == null ? LocalDate.now().withDayOfMonth(1) : startDate;
        LocalDate actualEnd = endDate == null ? LocalDate.now() : endDate;
        if (actualEnd.isBefore(actualStart)) {
            throw new BusinessException("结束日期不能早于开始日期");
        }
        List<LabBooking> bookings = labId == null
                ? labBookingRepository.findByDateBetween(actualStart, actualEnd)
                : labBookingRepository.findByLabIdAndDateBetween(labId, actualStart, actualEnd);
        refreshExpiredBookings(bookings);

        List<Lab> labs = labId == null
                ? labRepository.findAll()
                : Collections.singletonList(getDetail(labId));
        Map<Long, Lab> labMap = new HashMap<Long, Lab>();
        for (Lab lab : labs) {
            labMap.put(lab.getId(), lab);
        }

        long pendingBookingCount = 0;
        long effectiveBookingCount = 0;
        long actualUsageCount = 0;
        long noShowCount = 0;
        Map<Long, UsageCounter> counterMap = new LinkedHashMap<Long, UsageCounter>();
        for (Lab lab : labs) {
            counterMap.put(lab.getId(), new UsageCounter());
        }

        for (LabBooking booking : bookings) {
            if ("pending".equals(booking.getStatus())) {
                pendingBookingCount++;
            }
            if (EFFECTIVE_STATUSES.contains(booking.getStatus())) {
                effectiveBookingCount++;
            }
            if (isActualUsageStatus(booking.getStatus())) {
                actualUsageCount++;
            }
            if ("no_show".equals(booking.getStatus())) {
                noShowCount++;
            }
            UsageCounter counter = counterMap.get(booking.getLabId());
            if (counter == null) {
                counter = new UsageCounter();
                counterMap.put(booking.getLabId(), counter);
            }
            counter.totalBookingCount++;
            if ("pending".equals(booking.getStatus())) {
                counter.pendingBookingCount++;
            }
            if (EFFECTIVE_STATUSES.contains(booking.getStatus())) {
                counter.effectiveBookingCount++;
            }
            if (isActualUsageStatus(booking.getStatus())) {
                counter.actualUsageCount++;
            }
            if ("no_show".equals(booking.getStatus())) {
                counter.noShowCount++;
            }
        }

        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Lab lab : labs) {
            UsageCounter counter = counterMap.get(lab.getId());
            if (counter == null) {
                counter = new UsageCounter();
            }
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("labId", lab.getId());
            item.put("labName", lab.getName());
            item.put("location", lab.getLocation());
            item.put("totalBookingCount", counter.totalBookingCount);
            item.put("pendingBookingCount", counter.pendingBookingCount);
            item.put("effectiveBookingCount", counter.effectiveBookingCount);
            item.put("actualUsageCount", counter.actualUsageCount);
            item.put("noShowCount", counter.noShowCount);
            item.put("usageRate", calculateRate(counter.actualUsageCount, counter.effectiveBookingCount));
            list.add(item);
        }

        Map<String, Object> overview = new HashMap<String, Object>();
        overview.put("startDate", actualStart.toString());
        overview.put("endDate", actualEnd.toString());
        overview.put("totalBookingCount", bookings.size());
        overview.put("pendingBookingCount", pendingBookingCount);
        overview.put("effectiveBookingCount", effectiveBookingCount);
        overview.put("actualUsageCount", actualUsageCount);
        overview.put("noShowCount", noShowCount);
        overview.put("usageRate", calculateRate(actualUsageCount, effectiveBookingCount));

        int actualPage = page <= 0 ? 1 : page;
        int actualPageSize = pageSize <= 0 ? 10 : pageSize;
        int fromIndex = Math.min((actualPage - 1) * actualPageSize, list.size());
        int toIndex = Math.min(fromIndex + actualPageSize, list.size());

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("overview", overview);
        result.put("list", list.subList(fromIndex, toIndex));
        result.put("total", list.size());
        result.put("page", actualPage);
        result.put("pageSize", actualPageSize);
        return result;
    }

    public boolean canCheckIn(LabBooking booking, Long userId) {
        refreshExpiredBooking(booking);
        if (booking == null || userId == null || !userId.equals(booking.getUserId())) {
            return false;
        }
        if (!"approved".equals(booking.getStatus()) || !LocalDate.now().equals(booking.getDate())) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        return !now.isBefore(getSlotStartDateTime(booking).minusMinutes(15))
                && !now.isAfter(getSlotEndDateTime(booking));
    }

    public boolean canCheckOut(LabBooking booking, Long userId) {
        refreshExpiredBooking(booking);
        return booking != null
                && userId != null
                && userId.equals(booking.getUserId())
                && "checked_in".equals(booking.getStatus())
                && booking.getCheckInTime() != null;
    }

    public String getBookingActionHint(LabBooking booking, Long userId) {
        refreshExpiredBooking(booking);
        if (booking == null) {
            return "";
        }
        if (canCheckIn(booking, userId) || canCheckOut(booking, userId)) {
            return "";
        }
        if (userId == null || !userId.equals(booking.getUserId())) {
            return "仅预约人可操作";
        }

        String status = booking.getStatus();
        if ("pending".equals(status)) {
            return "待审批后可签到";
        }
        if ("rejected".equals(status)) {
            return "预约已拒绝";
        }
        if ("cancelled".equals(status)) {
            return "预约已取消";
        }
        if ("no_show".equals(status)) {
            return "已爽约，无法签到";
        }
        if ("completed".equals(status)) {
            return "已完成签退";
        }
        if ("checked_in".equals(status)) {
            return "已签到，可签退";
        }
        if (!"approved".equals(status)) {
            return "当前状态不可操作";
        }
        if (!LocalDate.now().equals(booking.getDate())) {
            return booking.getDate() != null && booking.getDate().isAfter(LocalDate.now())
                    ? "仅预约当天可签到"
                    : "预约日期已过，无法签到";
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime signInStart = getSlotStartDateTime(booking).minusMinutes(15);
        LocalDateTime slotEnd = getSlotEndDateTime(booking);
        if (now.isBefore(signInStart)) {
            return "开始前15分钟内可签到";
        }
        if (now.isAfter(slotEnd)) {
            return "当前预约已过签到时间";
        }
        return "";
    }

    public Page<Lab> getManageList(int page, int pageSize) {
        Pageable p = PageRequest.of(page - 1, pageSize);
        return labRepository.findAllByOrderByIdDesc(p);
    }

    @Transactional
    public Lab saveLab(Long id, String name, String location, String description,
                       String equipmentInfo, Integer capacity, String status) {
        Lab lab;
        if (id != null && id > 0) {
            lab = labRepository.findById(id).orElseThrow(() -> new BusinessException("实验室不存在"));
        } else {
            lab = new Lab();
        }
        if (name != null) lab.setName(name);
        if (location != null) lab.setLocation(location);
        if (description != null) lab.setDescription(description);
        if (equipmentInfo != null) lab.setEquipmentInfo(equipmentInfo);
        if (capacity != null) lab.setCapacity(capacity);
        if (status != null) lab.setStatus(status);
        return labRepository.save(lab);
    }

    @Transactional
    public void deleteLab(Long id) {
        labRepository.deleteById(id);
    }

    private boolean isTextMatch(String source, String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        return source != null && source.contains(keyword.trim());
    }

    private boolean isLabAvailable(Lab lab, LocalDate date, String slot) {
        if (!"available".equals(lab.getStatus())) {
            return false;
        }
        if (date == null || slot == null || slot.trim().isEmpty()) {
            return true;
        }
        return !labBookingRepository.existsByLabIdAndDateAndSlotAndStatusIn(
                lab.getId(), date, slot, SLOT_BLOCKING_STATUSES);
    }

    public void refreshExpiredBookings(List<LabBooking> bookings) {
        if (bookings == null || bookings.isEmpty()) {
            return;
        }
        List<LabBooking> changed = new ArrayList<LabBooking>();
        for (LabBooking booking : bookings) {
            if (refreshExpiredBooking(booking)) {
                changed.add(booking);
            }
        }
        if (!changed.isEmpty()) {
            labBookingRepository.saveAll(changed);
        }
    }

    public boolean refreshExpiredBooking(LabBooking booking) {
        if (booking == null) {
            return false;
        }
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime slotEnd = getSlotEndDateTime(booking);
        if ("approved".equals(booking.getStatus()) && booking.getCheckInTime() == null && now.isAfter(slotEnd)) {
            booking.setStatus("no_show");
            return true;
        }
        if ("checked_in".equals(booking.getStatus()) && booking.getCheckOutTime() == null && now.isAfter(slotEnd)) {
            booking.setStatus("completed");
            booking.setCheckOutTime(slotEnd);
            return true;
        }
        return false;
    }

    public LocalDateTime getSlotStartDateTime(LabBooking booking) {
        String[] parts = booking.getSlot().split("-");
        return LocalDateTime.of(booking.getDate(), LocalTime.parse(parts[0]));
    }

    public LocalDateTime getSlotEndDateTime(LabBooking booking) {
        String[] parts = booking.getSlot().split("-");
        return LocalDateTime.of(booking.getDate(), LocalTime.parse(parts[1]));
    }

    private boolean isActualUsageStatus(String status) {
        return "checked_in".equals(status) || "completed".equals(status);
    }

    private double calculateRate(long numerator, long denominator) {
        if (denominator <= 0) {
            return 0D;
        }
        return Math.round((numerator * 10000D) / denominator) / 100D;
    }

    private static class UsageCounter {
        private long totalBookingCount;
        private long pendingBookingCount;
        private long effectiveBookingCount;
        private long actualUsageCount;
        private long noShowCount;
    }
}
