package com.lamp.service;

import com.lamp.entity.User;
import com.lamp.exception.BusinessException;
import com.lamp.repository.UserRepository;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User getByUsername(String username) {
        return userRepository.findByUsername(username).orElse(null);
    }

    public User getById(Long id) {
        return userRepository.findById(id).orElseThrow(() -> new BusinessException("用户不存在"));
    }

    @Transactional
    public User register(String username, String name, String password, String role) {
        if (userRepository.existsByUsername(username)) {
            throw new BusinessException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setName(name);
        user.setPassword(passwordEncoder.encode(password));
        user.setRole(role);
        user.setStatus(1);
        return userRepository.save(user);
    }

    public User login(String username, String password) {
        User user = userRepository.findByUsername(username).orElseThrow(() -> new BusinessException("用户名或密码错误"));
        if (user.getStatus() != null && user.getStatus() == 0) {
            throw new BusinessException("账号已被禁用");
        }
        if (!passwordEncoder.matches(password, user.getPassword())) {
            throw new BusinessException("用户名或密码错误");
        }
        return user;
    }

    @Transactional
    public void updatePassword(Long userId, String oldPassword, String newPassword) {
        User user = getById(userId);
        if (!passwordEncoder.matches(oldPassword, user.getPassword())) {
            throw new BusinessException("原密码错误");
        }
        user.setPassword(passwordEncoder.encode(newPassword));
        userRepository.save(user);
    }

    @Transactional
    public User updateProfile(Long userId, String name, String phone, String email) {
        User user = getById(userId);
        if (name != null) user.setName(name);
        if (phone != null) user.setPhone(phone);
        if (email != null) user.setEmail(email);
        return userRepository.save(user);
    }
}
