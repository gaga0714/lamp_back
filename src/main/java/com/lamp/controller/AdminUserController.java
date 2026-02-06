package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.repository.UserRepository;
import com.lamp.security.UserContext;
import com.lamp.service.UserService;
import com.lamp.vo.UserVO;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final UserService userService;

    public AdminUserController(UserRepository userRepository, UserService userService) {
        this.userRepository = userRepository;
        this.userService = userService;
    }

    private void requireAdmin() {
        String role = UserContext.getRole();
        if (!"admin".equals(role)) {
            throw new BusinessException(403, "无权限");
        }
    }

    @GetMapping
    public Result<Map<String, Object>> list(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        requireAdmin();
        Pageable p = PageRequest.of(page - 1, pageSize);
        Page<User> pg = userRepository.search(keyword, role, p);
        List<Map<String, Object>> list = pg.getContent().stream()
                .map(UserVO::from)
                .map(UserVO::toMap)
                .collect(Collectors.toList());
        Map<String, Object> data = new HashMap<>();
        data.put("list", list);
        data.put("total", pg.getTotalElements());
        return Result.ok(data);
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        requireAdmin();
        String username = (String) body.get("username");
        String name = (String) body.get("name");
        String password = body.get("password") != null ? body.get("password").toString() : "123456";
        String role = (String) body.get("role");
        String phone = body.get("phone") != null ? body.get("phone").toString() : null;
        String email = body.get("email") != null ? body.get("email").toString() : null;
        if (username == null || name == null || role == null) {
            throw new BusinessException("用户名、姓名、角色不能为空");
        }
        User user = userService.register(username, name, password, role);
        return Result.ok(UserVO.from(user).toMap());
    }

    @PutMapping
    public Result<Map<String, Object>> update(@RequestBody Map<String, Object> body) {
        requireAdmin();
        Long id = body.get("id") instanceof Number ? ((Number) body.get("id")).longValue() : null;
        if (id == null) throw new BusinessException("id不能为空");
        User user = userService.getById(id);
        if (body.get("name") != null) user.setName(body.get("name").toString());
        if (body.get("phone") != null) user.setPhone(body.get("phone").toString());
        if (body.get("email") != null) user.setEmail(body.get("email").toString());
        if (body.get("role") != null) user.setRole(body.get("role").toString());
        if (body.get("status") != null) user.setStatus(((Number) body.get("status")).intValue());
        user = userRepository.save(user);
        return Result.ok(UserVO.from(user).toMap());
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        if (UserContext.getUserId().equals(id)) {
            throw new BusinessException("不能删除自己");
        }
        userRepository.deleteById(id);
        return Result.ok();
    }
}
