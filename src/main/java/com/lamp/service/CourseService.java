package com.lamp.service;

import com.lamp.entity.Course;
import com.lamp.entity.CourseAttendance;
import com.lamp.entity.CourseStudent;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.repository.CourseAttendanceRepository;
import com.lamp.repository.CourseRepository;
import com.lamp.repository.CourseStudentRepository;
import com.lamp.repository.LeaveApplyRepository;
import com.lamp.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class CourseService {

    private final CourseRepository courseRepository;
    private final CourseStudentRepository courseStudentRepository;
    private final CourseAttendanceRepository courseAttendanceRepository;
    private final LeaveApplyRepository leaveApplyRepository;
    private final UserRepository userRepository;

    public CourseService(CourseRepository courseRepository,
                         CourseStudentRepository courseStudentRepository,
                         CourseAttendanceRepository courseAttendanceRepository,
                         LeaveApplyRepository leaveApplyRepository,
                         UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseStudentRepository = courseStudentRepository;
        this.courseAttendanceRepository = courseAttendanceRepository;
        this.leaveApplyRepository = leaveApplyRepository;
        this.userRepository = userRepository;
    }

    public List<Map<String, Object>> getStudentSchedule(Long studentId, LocalDate weekStart) {
        List<Course> courses = getStudentCourses(studentId);
        return buildScheduleResponse(courses, normalizeWeekStart(weekStart));
    }

    public List<Map<String, Object>> getTeacherSchedule(Long teacherId, LocalDate weekStart) {
        List<Course> courses = courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active");
        return buildScheduleResponse(courses, normalizeWeekStart(weekStart));
    }

    public List<Map<String, Object>> getStudentCourseOptions(Long studentId, LocalDate courseDate) {
        LocalDate targetDate = courseDate == null ? LocalDate.now() : courseDate;
        List<Course> courses = getStudentCourses(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, User> teacherMap = getUserMapByIds(extractTeacherIds(courses));
        for (Course course : courses) {
            if (!isCourseActiveOnDate(course, targetDate)) {
                continue;
            }
            Optional<CourseAttendance> attendance = courseAttendanceRepository
                    .findByCourseIdAndStudentIdAndCourseDate(course.getId(), studentId, targetDate);
            String status = resolveAttendanceStatus(course, targetDate, attendance.orElse(null));
            Map<String, Object> item = buildCourseOccurrenceMap(course, targetDate, teacherMap.get(course.getTeacherId()));
            item.put("status", status);
            item.put("checkInTime", attendance.isPresent() ? attendance.get().getCheckInTime() : null);
            item.put("canCheckIn", LocalDate.now().equals(targetDate) && "待签到".equals(status));
            result.add(item);
        }
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return String.valueOf(o1.get("startTime")).compareTo(String.valueOf(o2.get("startTime")));
            }
        });
        return result;
    }

    public List<Map<String, Object>> getStudentLeaveCourseOptions(Long studentId, LocalDate courseDate) {
        LocalDate targetDate = courseDate == null ? LocalDate.now() : courseDate;
        if (targetDate.isBefore(LocalDate.now())) {
            return Collections.emptyList();
        }
        List<Course> courses = getStudentCourses(studentId);
        List<Map<String, Object>> result = new ArrayList<>();
        Map<Long, User> teacherMap = getUserMapByIds(extractTeacherIds(courses));
        for (Course course : courses) {
            if (!isCourseActiveOnDate(course, targetDate)) {
                continue;
            }
            Optional<CourseAttendance> attendance = courseAttendanceRepository
                    .findByCourseIdAndStudentIdAndCourseDate(course.getId(), studentId, targetDate);
            String status = resolveAttendanceStatus(course, targetDate, attendance.orElse(null));
            if (!"待签到".equals(status)) {
                continue;
            }
            if (leaveApplyRepository.existsByUserIdAndCourseIdAndCourseDateAndStatusIn(
                    studentId, course.getId(), targetDate, Arrays.asList("待审批", "已通过"))) {
                continue;
            }
            Map<String, Object> item = buildCourseOccurrenceMap(course, targetDate, teacherMap.get(course.getTeacherId()));
            item.put("status", status);
            result.add(item);
        }
        Collections.sort(result, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                return String.valueOf(o1.get("startTime")).compareTo(String.valueOf(o2.get("startTime")));
            }
        });
        return result;
    }

    public List<Map<String, Object>> getTeacherCourseOptions(Long teacherId) {
        List<Course> courses = courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active");
        Map<Long, Integer> studentCountMap = getStudentCountMap(extractCourseIds(courses));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            Map<String, Object> item = new HashMap<String, Object>();
            item.put("id", course.getId());
            item.put("courseCode", course.getCourseCode());
            item.put("courseName", course.getCourseName());
            item.put("weekday", course.getWeekday());
            item.put("weeks", course.getWeeks());
            item.put("semester", course.getSemester());
            item.put("startTime", course.getStartTime());
            item.put("endTime", course.getEndTime());
            item.put("location", course.getLocation());
            item.put("studentCount", studentCountMap.get(course.getId()) == null ? 0 : studentCountMap.get(course.getId()));
            result.add(item);
        }
        return result;
    }

    @Transactional
    public Map<String, Object> checkIn(Long studentId, Long courseId, LocalDate courseDate) {
        Course course = getCourse(courseId);
        validateStudentCourse(studentId, course, courseDate);
        Optional<CourseAttendance> existing = courseAttendanceRepository
                .findByCourseIdAndStudentIdAndCourseDate(courseId, studentId, courseDate);
        if (existing.isPresent() && !"待签到".equals(existing.get().getStatus())) {
            throw new BusinessException("该课程已完成签到");
        }
        CourseAttendance attendance = existing.isPresent() ? existing.get() : new CourseAttendance();
        attendance.setCourseId(courseId);
        attendance.setStudentId(studentId);
        attendance.setCourseDate(courseDate);
        attendance.setCheckInTime(LocalDateTime.now());
        attendance.setRemark(null);
        attendance.setStatus(LocalDateTime.now().toLocalTime().isAfter(course.getStartTime()) ? "迟到" : "已签到");
        courseAttendanceRepository.save(attendance);

        Map<String, Object> result = new HashMap<String, Object>();
        result.put("courseId", courseId);
        result.put("courseDate", courseDate);
        result.put("status", attendance.getStatus());
        result.put("checkInTime", attendance.getCheckInTime());
        return result;
    }

    public Map<String, Object> getStudentAttendance(Long studentId, LocalDate startDate, LocalDate endDate, int page, int pageSize) {
        LocalDate actualEnd = endDate == null ? LocalDate.now() : endDate;
        LocalDate actualStart = startDate == null ? actualEnd.minusDays(30) : startDate;
        List<Course> courses = getStudentCourses(studentId);
        Map<Long, User> teacherMap = getUserMapByIds(extractTeacherIds(courses));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseOccurrence occurrence : buildOccurrences(courses, actualStart, actualEnd)) {
            CourseAttendance attendance = courseAttendanceRepository
                    .findByCourseIdAndStudentIdAndCourseDate(occurrence.course.getId(), studentId, occurrence.date)
                    .orElse(null);
            Map<String, Object> item = buildCourseOccurrenceMap(occurrence.course, occurrence.date,
                    teacherMap.get(occurrence.course.getTeacherId()));
            item.put("status", resolveAttendanceStatus(occurrence.course, occurrence.date, attendance));
            item.put("checkInTime", attendance == null ? null : attendance.getCheckInTime());
            item.put("remark", attendance == null ? null : attendance.getRemark());
            rows.add(item);
        }
        sortOccurrenceRows(rows);
        return pageResult(rows, page, pageSize);
    }

    public Map<String, Object> getTeacherAttendance(Long teacherId, Long courseId, LocalDate courseDate, int page, int pageSize) {
        LocalDate targetDate = courseDate == null ? LocalDate.now() : courseDate;
        List<Course> teacherCourses = courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active");
        if (courseId != null) {
            List<Course> filtered = new ArrayList<>();
            for (Course course : teacherCourses) {
                if (course.getId().equals(courseId)) {
                    filtered.add(course);
                    break;
                }
            }
            teacherCourses = filtered;
        }
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, User> studentMap = new HashMap<Long, User>();
        for (Course course : teacherCourses) {
            if (!isCourseActiveOnDate(course, targetDate)) {
                continue;
            }
            List<CourseStudent> courseStudents = courseStudentRepository.findByCourseId(course.getId());
            fillUserMap(studentMap, extractStudentIds(courseStudents));
            for (CourseStudent relation : courseStudents) {
                CourseAttendance attendance = courseAttendanceRepository
                        .findByCourseIdAndStudentIdAndCourseDate(course.getId(), relation.getStudentId(), targetDate)
                        .orElse(null);
                User student = studentMap.get(relation.getStudentId());
                Map<String, Object> item = buildAttendanceManageRow(course, targetDate, student, attendance);
                rows.add(item);
            }
        }
        sortManageRows(rows);
        return pageResult(rows, page, pageSize);
    }

    public Map<String, Object> getAdminAttendance(String keyword, LocalDate courseDate, int page, int pageSize) {
        LocalDate targetDate = courseDate == null ? LocalDate.now() : courseDate;
        List<Course> courses = courseRepository.findAll();
        List<Map<String, Object>> rows = new ArrayList<>();
        Map<Long, User> studentMap = new HashMap<Long, User>();
        for (Course course : courses) {
            if (!isCourseActiveOnDate(course, targetDate)) {
                continue;
            }
            List<CourseStudent> courseStudents = courseStudentRepository.findByCourseId(course.getId());
            fillUserMap(studentMap, extractStudentIds(courseStudents));
            for (CourseStudent relation : courseStudents) {
                User student = studentMap.get(relation.getStudentId());
                if (!matchKeyword(keyword, student, course)) {
                    continue;
                }
                CourseAttendance attendance = courseAttendanceRepository
                        .findByCourseIdAndStudentIdAndCourseDate(course.getId(), relation.getStudentId(), targetDate)
                        .orElse(null);
                rows.add(buildAttendanceManageRow(course, targetDate, student, attendance));
            }
        }
        sortManageRows(rows);
        return pageResult(rows, page, pageSize);
    }

    public Map<String, Object> getCourseStudents(Long courseId, LocalDate courseDate) {
        Course course = getCourse(courseId);
        LocalDate targetDate = courseDate == null ? LocalDate.now() : courseDate;
        List<CourseStudent> relations = courseStudentRepository.findByCourseId(courseId);
        Map<Long, User> studentMap = getUserMapByIds(extractStudentIds(relations));
        List<Map<String, Object>> rows = new ArrayList<>();
        for (CourseStudent relation : relations) {
            CourseAttendance attendance = courseAttendanceRepository
                    .findByCourseIdAndStudentIdAndCourseDate(courseId, relation.getStudentId(), targetDate)
                    .orElse(null);
            rows.add(buildAttendanceManageRow(course, targetDate, studentMap.get(relation.getStudentId()), attendance));
        }
        sortManageRows(rows);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("course", buildCourseBaseMap(course, null));
        result.put("list", rows);
        result.put("total", rows.size());
        return result;
    }

    public Map<String, Object> getManageCourseList(String keyword, int page, int pageSize) {
        Pageable pageable = PageRequest.of(Math.max(page - 1, 0), pageSize);
        Page<Course> coursePage;
        if (keyword != null && !keyword.trim().isEmpty()) {
            coursePage = courseRepository.findByCourseNameContainingOrCourseCodeContaining(keyword, keyword, pageable);
        } else {
            coursePage = courseRepository.findAll(pageable);
        }
        List<Course> records = coursePage.getContent();
        Map<Long, User> teacherMap = getUserMapByIds(extractTeacherIds(records));
        Map<Long, Integer> studentCountMap = getStudentCountMap(extractCourseIds(records));
        List<Map<String, Object>> list = new ArrayList<>();
        for (Course course : records) {
            Map<String, Object> item = buildCourseBaseMap(course, teacherMap.get(course.getTeacherId()));
            item.put("studentCount", studentCountMap.get(course.getId()) == null ? 0 : studentCountMap.get(course.getId()));
            list.add(item);
        }
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("list", list);
        result.put("total", coursePage.getTotalElements());
        result.put("page", page);
        result.put("pageSize", pageSize);
        return result;
    }

    public long countStudentCoursesThisWeek(Long studentId) {
        LocalDate weekStart = normalizeWeekStart(LocalDate.now());
        return buildOccurrences(getStudentCourses(studentId), weekStart, weekStart.plusDays(6)).size();
    }

    public long countStudentPendingCheckIn(Long studentId) {
        List<Course> courses = getStudentCourses(studentId);
        LocalDate today = LocalDate.now();
        long total = 0;
        for (Course course : courses) {
            if (!isCourseActiveOnDate(course, today)) {
                continue;
            }
            CourseAttendance attendance = courseAttendanceRepository
                    .findByCourseIdAndStudentIdAndCourseDate(course.getId(), studentId, today)
                    .orElse(null);
            if ("待签到".equals(resolveAttendanceStatus(course, today, attendance))) {
                total++;
            }
        }
        return total;
    }

    public long countTeacherCoursesThisWeek(Long teacherId) {
        LocalDate weekStart = normalizeWeekStart(LocalDate.now());
        return buildOccurrences(courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active"),
                weekStart, weekStart.plusDays(6)).size();
    }

    public long countTeacherTodayCourses(Long teacherId) {
        LocalDate today = LocalDate.now();
        long total = 0;
        for (Course course : courseRepository.findByTeacherIdAndStatusOrderByWeekdayAscStartTimeAsc(teacherId, "active")) {
            if (isCourseActiveOnDate(course, today)) {
                total++;
            }
        }
        return total;
    }

    public long countAdminTodayCourseAttendances() {
        Map<String, Object> data = getAdminAttendance(null, LocalDate.now(), 1, Integer.MAX_VALUE);
        return data.containsKey("total") ? ((Number) data.get("total")).longValue() : 0;
    }

    private List<Map<String, Object>> buildScheduleResponse(List<Course> courses, LocalDate weekStart) {
        Map<Long, User> teacherMap = getUserMapByIds(extractTeacherIds(courses));
        List<Map<String, Object>> result = new ArrayList<>();
        for (Course course : courses) {
            LocalDate courseDate = weekStart.plusDays(course.getWeekday() - 1L);
            if (!isCourseActiveOnDate(course, courseDate)) {
                continue;
            }
            result.add(buildCourseOccurrenceMap(course, courseDate, teacherMap.get(course.getTeacherId())));
        }
        sortOccurrenceRows(result);
        return result;
    }

    private Map<String, Object> buildCourseOccurrenceMap(Course course, LocalDate courseDate, User teacher) {
        Map<String, Object> item = buildCourseBaseMap(course, teacher);
        item.put("courseDate", courseDate);
        item.put("startTime", course.getStartTime());
        item.put("endTime", course.getEndTime());
        item.put("courseTime", course.getStartTime() + "-" + course.getEndTime());
        item.put("weekday", course.getWeekday());
        return item;
    }

    private Map<String, Object> buildCourseBaseMap(Course course, User teacher) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("id", course.getId());
        item.put("courseCode", course.getCourseCode());
        item.put("courseName", course.getCourseName());
        item.put("semester", course.getSemester());
        item.put("weeks", course.getWeeks());
        item.put("weekday", course.getWeekday());
        item.put("startTime", course.getStartTime());
        item.put("endTime", course.getEndTime());
        item.put("location", course.getLocation());
        item.put("remark", course.getRemark());
        item.put("status", course.getStatus());
        item.put("teacherId", course.getTeacherId());
        item.put("teacherName", teacher == null ? null : teacher.getName());
        item.put("teacherUsername", teacher == null ? null : teacher.getUsername());
        item.put("termStartDate", course.getTermStartDate());
        return item;
    }

    private Map<String, Object> buildAttendanceManageRow(Course course, LocalDate courseDate, User student, CourseAttendance attendance) {
        Map<String, Object> item = new HashMap<String, Object>();
        item.put("courseId", course.getId());
        item.put("courseCode", course.getCourseCode());
        item.put("courseName", course.getCourseName());
        item.put("courseDate", courseDate);
        item.put("courseTime", course.getStartTime() + "-" + course.getEndTime());
        item.put("location", course.getLocation());
        item.put("username", student == null ? null : student.getUsername());
        item.put("name", student == null ? null : student.getName());
        item.put("studentId", student == null ? null : student.getId());
        item.put("status", resolveAttendanceStatus(course, courseDate, attendance));
        item.put("checkInTime", attendance == null ? null : attendance.getCheckInTime());
        item.put("remark", attendance == null ? null : attendance.getRemark());
        return item;
    }

    private List<Course> getStudentCourses(Long studentId) {
        List<CourseStudent> relations = courseStudentRepository.findByStudentId(studentId);
        if (relations.isEmpty()) {
            return Collections.emptyList();
        }
        return filterActiveCourses(courseRepository.findByIdIn(extractCourseIds(relations)));
    }

    private List<Course> filterActiveCourses(List<Course> courses) {
        List<Course> result = new ArrayList<>();
        for (Course course : courses) {
            if ("active".equals(course.getStatus())) {
                result.add(course);
            }
        }
        Collections.sort(result, new Comparator<Course>() {
            @Override
            public int compare(Course o1, Course o2) {
                int weekdayCompare = o1.getWeekday().compareTo(o2.getWeekday());
                if (weekdayCompare != 0) {
                    return weekdayCompare;
                }
                return o1.getStartTime().compareTo(o2.getStartTime());
            }
        });
        return result;
    }

    private void validateStudentCourse(Long studentId, Course course, LocalDate courseDate) {
        if (courseDate == null) {
            throw new BusinessException("缺少课程日期");
        }
        if (!LocalDate.now().equals(courseDate)) {
            throw new BusinessException("仅支持当天课程签到");
        }
        if (!courseStudentRepository.existsByCourseIdAndStudentId(course.getId(), studentId)) {
            throw new BusinessException(403, "只能签到自己的课程");
        }
        if (!isCourseActiveOnDate(course, courseDate)) {
            throw new BusinessException("当前日期不存在该课程安排");
        }
    }

    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow(new java.util.function.Supplier<BusinessException>() {
            @Override
            public BusinessException get() {
                return new BusinessException("课程不存在");
            }
        });
    }

    public boolean isStudentEnrolled(Long studentId, Long courseId) {
        return courseStudentRepository.existsByCourseIdAndStudentId(courseId, studentId);
    }

    public boolean isCourseActiveOnDate(Course course, LocalDate date) {
        if (course == null || date == null || !"active".equals(course.getStatus())) {
            return false;
        }
        if (course.getWeekday() == null || course.getTermStartDate() == null || course.getWeeks() == null) {
            return false;
        }
        if (date.getDayOfWeek().getValue() != course.getWeekday()) {
            return false;
        }
        long days = ChronoUnit.DAYS.between(course.getTermStartDate(), date);
        if (days < 0) {
            return false;
        }
        int weekNo = (int) (days / 7) + 1;
        return containsWeek(course.getWeeks(), weekNo);
    }

    public String resolveAttendanceStatus(Course course, LocalDate courseDate, CourseAttendance attendance) {
        if (attendance != null) {
            return attendance.getStatus();
        }
        LocalDate today = LocalDate.now();
        if (courseDate.isBefore(today)) {
            return "缺勤";
        }
        if (courseDate.isAfter(today)) {
            return "待签到";
        }
        LocalTime now = LocalTime.now();
        if (now.isAfter(course.getEndTime())) {
            return "缺勤";
        }
        return "待签到";
    }

    private List<CourseOccurrence> buildOccurrences(List<Course> courses, LocalDate startDate, LocalDate endDate) {
        List<CourseOccurrence> result = new ArrayList<>();
        for (Course course : courses) {
            LocalDate cursor = startDate;
            while (!cursor.isAfter(endDate)) {
                if (isCourseActiveOnDate(course, cursor)) {
                    result.add(new CourseOccurrence(course, cursor));
                }
                cursor = cursor.plusDays(1);
            }
        }
        Collections.sort(result, new Comparator<CourseOccurrence>() {
            @Override
            public int compare(CourseOccurrence o1, CourseOccurrence o2) {
                int dateCompare = o2.date.compareTo(o1.date);
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return o1.course.getStartTime().compareTo(o2.course.getStartTime());
            }
        });
        return result;
    }

    private boolean containsWeek(String weeks, int weekNo) {
        if (weeks == null || weeks.trim().isEmpty()) {
            return false;
        }
        String[] parts = weeks.split(",");
        for (String part : parts) {
            String trimmed = part.trim();
            if (trimmed.contains("-")) {
                String[] range = trimmed.split("-");
                if (range.length == 2) {
                    int start = parseInt(range[0]);
                    int end = parseInt(range[1]);
                    if (weekNo >= start && weekNo <= end) {
                        return true;
                    }
                }
            } else if (weekNo == parseInt(trimmed)) {
                return true;
            }
        }
        return false;
    }

    private int parseInt(String value) {
        try {
            return Integer.parseInt(value.trim());
        } catch (Exception ex) {
            return -1;
        }
    }

    private LocalDate normalizeWeekStart(LocalDate date) {
        LocalDate current = date == null ? LocalDate.now() : date;
        DayOfWeek dayOfWeek = current.getDayOfWeek();
        return current.minusDays(dayOfWeek.getValue() - 1L);
    }

    private Map<Long, User> getUserMapByIds(Collection<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return new HashMap<Long, User>();
        }
        List<Long> userIds = new ArrayList<>(new HashSet<Long>(ids));
        List<User> users = userRepository.findByIdIn(userIds);
        Map<Long, User> map = new HashMap<Long, User>();
        for (User user : users) {
            map.put(user.getId(), user);
        }
        return map;
    }

    private void fillUserMap(Map<Long, User> target, Collection<Long> ids) {
        Set<Long> needLoad = new HashSet<>();
        for (Long id : ids) {
            if (id != null && !target.containsKey(id)) {
                needLoad.add(id);
            }
        }
        if (needLoad.isEmpty()) {
            return;
        }
        for (User user : userRepository.findByIdIn(new ArrayList<Long>(needLoad))) {
            target.put(user.getId(), user);
        }
    }

    private List<Long> extractCourseIds(List<CourseStudent> relations) {
        List<Long> ids = new ArrayList<>();
        for (CourseStudent relation : relations) {
            ids.add(relation.getCourseId());
        }
        return ids;
    }

    private List<Long> extractCourseIdsFromCourses(List<Course> courses) {
        List<Long> ids = new ArrayList<>();
        for (Course course : courses) {
            ids.add(course.getId());
        }
        return ids;
    }

    private List<Long> extractTeacherIds(List<Course> courses) {
        List<Long> ids = new ArrayList<>();
        for (Course course : courses) {
            ids.add(course.getTeacherId());
        }
        return ids;
    }

    private List<Long> extractStudentIds(List<CourseStudent> relations) {
        List<Long> ids = new ArrayList<>();
        for (CourseStudent relation : relations) {
            ids.add(relation.getStudentId());
        }
        return ids;
    }

    private List<Long> extractCourseIds(Collection<Course> courses) {
        List<Long> ids = new ArrayList<>();
        for (Course course : courses) {
            ids.add(course.getId());
        }
        return ids;
    }

    private Map<Long, Integer> getStudentCountMap(List<Long> courseIds) {
        Map<Long, Integer> result = new HashMap<Long, Integer>();
        if (courseIds == null || courseIds.isEmpty()) {
            return result;
        }
        for (CourseStudent relation : courseStudentRepository.findByCourseIdIn(courseIds)) {
            Integer count = result.get(relation.getCourseId());
            result.put(relation.getCourseId(), count == null ? 1 : count + 1);
        }
        return result;
    }

    private boolean matchKeyword(String keyword, User student, Course course) {
        if (keyword == null || keyword.trim().isEmpty()) {
            return true;
        }
        String lower = keyword.trim().toLowerCase();
        if (student != null) {
            if (student.getUsername() != null && student.getUsername().toLowerCase().contains(lower)) {
                return true;
            }
            if (student.getName() != null && student.getName().toLowerCase().contains(lower)) {
                return true;
            }
        }
        return (course.getCourseName() != null && course.getCourseName().toLowerCase().contains(lower))
                || (course.getCourseCode() != null && course.getCourseCode().toLowerCase().contains(lower));
    }

    private Map<String, Object> pageResult(List<Map<String, Object>> rows, int page, int pageSize) {
        int actualPage = page <= 0 ? 1 : page;
        int actualPageSize = pageSize <= 0 ? 10 : pageSize;
        int fromIndex = Math.min((actualPage - 1) * actualPageSize, rows.size());
        int toIndex = Math.min(fromIndex + actualPageSize, rows.size());
        List<Map<String, Object>> list = rows.subList(fromIndex, toIndex);
        Map<String, Object> result = new HashMap<String, Object>();
        result.put("list", list);
        result.put("total", rows.size());
        result.put("page", actualPage);
        result.put("pageSize", actualPageSize);
        return result;
    }

    private void sortOccurrenceRows(List<Map<String, Object>> rows) {
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                String d1 = String.valueOf(o1.get("courseDate"));
                String d2 = String.valueOf(o2.get("courseDate"));
                int dateCompare = d2.compareTo(d1);
                if (dateCompare != 0) {
                    return dateCompare;
                }
                return String.valueOf(o1.get("startTime")).compareTo(String.valueOf(o2.get("startTime")));
            }
        });
    }

    private void sortManageRows(List<Map<String, Object>> rows) {
        Collections.sort(rows, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> o1, Map<String, Object> o2) {
                int courseCompare = String.valueOf(o1.get("courseCode")).compareTo(String.valueOf(o2.get("courseCode")));
                if (courseCompare != 0) {
                    return courseCompare;
                }
                return String.valueOf(o1.get("username")).compareTo(String.valueOf(o2.get("username")));
            }
        });
    }

    private static class CourseOccurrence {
        private final Course course;
        private final LocalDate date;

        private CourseOccurrence(Course course, LocalDate date) {
            this.course = course;
            this.date = date;
        }
    }
}
