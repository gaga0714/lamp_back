package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.dto.LoginDTO;
import com.lamp.dto.PasswordDTO;
import com.lamp.dto.RegisterDTO;
import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.security.UserContext;
import com.lamp.service.UserService;
import com.lamp.util.JwtUtil;
import com.lamp.vo.UserVO;
import javax.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
public class AuthController {

    private final UserService userService;
    private final JwtUtil jwtUtil;

    public AuthController(UserService userService, JwtUtil jwtUtil) {
        this.userService = userService;
        this.jwtUtil = jwtUtil;
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Valid @RequestBody LoginDTO dto) {
        User user = userService.login(dto.getUsername(), dto.getPassword());
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());
        Map<String, Object> data = new HashMap<>();
        data.put("token", token);
        data.put("user", UserVO.from(user).toMap());
        return Result.ok(data);
    }

    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        if (!"student".equals(dto.getRole()) && !"teacher".equals(dto.getRole())) {
            return Result.fail("角色只能是研究生或教师");
        }
        userService.register(dto.getUsername(), dto.getName(), dto.getPassword(), dto.getRole());
        return Result.ok();
    }

    @GetMapping("/user")
    public Result<Map<String, Object>> getUser() {
        Long userId = requireLogin();
        User user = userService.getById(userId);
        return Result.ok(UserVO.from(user).toMap());
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody PasswordDTO dto) {
        userService.updatePassword(requireLogin(), dto.getOldPassword(), dto.getNewPassword());
        return Result.ok();
    }

    private Long requireLogin() {
        Long userId = UserContext.getUserId();
        if (userId == null) {
            throw new BusinessException(401, "未登录或登录已过期");
        }
        return userId;
    }
}
