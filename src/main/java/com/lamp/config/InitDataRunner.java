package com.lamp.config;

import com.lamp.entity.User;
import com.lamp.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
public class InitDataRunner {

    @Bean
    public CommandLineRunner initAdmin(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        return args -> {
            if (!userRepository.findByUsername("admin").isPresent()) {
                User admin = new User();
                admin.setUsername("admin");
                admin.setName("系统管理员");
                admin.setPassword(passwordEncoder.encode("admin123"));
                admin.setRole("admin");
                admin.setStatus(1);
                userRepository.save(admin);
            }
        };
    }
}
