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
        throw new BusinessException(403, "实验室管理员无权限");
    }

    @PostMapping
    public Result<Map<String, Object>> create(@RequestBody Map<String, Object> body) {
        requireAdmin();
        throw new BusinessException(403, "实验室管理员无权限");
    }

    @PutMapping
    public Result<Map<String, Object>> update(@RequestBody Map<String, Object> body) {
        requireAdmin();
        throw new BusinessException(403, "实验室管理员无权限");
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        requireAdmin();
        throw new BusinessException(403, "实验室管理员无权限");
    }
}
