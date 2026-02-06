package com.lamp.controller;

import com.lamp.common.Result;
import com.lamp.entity.User;
import com.lamp.security.UserContext;
import com.lamp.service.UserService;
import com.lamp.vo.UserVO;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @PutMapping("/profile")
    public Result<Map<String, Object>> updateProfile(@RequestBody Map<String, String> body) {
        Long userId = UserContext.getUserId();
        String name = body.get("name");
        String phone = body.get("phone");
        String email = body.get("email");
        User user = userService.updateProfile(userId, name, phone, email);
        return Result.ok(UserVO.from(user).toMap());
    }
}
