package com.lamp.vo;

import com.lamp.entity.User;
import lombok.Data;

import java.util.HashMap;
import java.util.Map;

@Data
public class UserVO {
    private Long id;
    private String username;
    private String name;
    private String phone;
    private String email;
    private String role;
    private Integer status;

    public static UserVO from(User user) {
        if (user == null) return null;
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setName(user.getName());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        return vo;
    }

    public Map<String, Object> toMap() {
        Map<String, Object> m = new HashMap<>();
        m.put("id", id);
        m.put("username", username);
        m.put("name", name);
        m.put("phone", phone);
        m.put("email", email);
        m.put("role", role);
        m.put("status", status);
        return m;
    }
}
