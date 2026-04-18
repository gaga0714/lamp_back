package com.lamp.repository;

import com.lamp.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    List<User> findByIdIn(List<Long> ids);

    List<User> findByRoleOrderByIdAsc(String role);

    List<User> findByUsernameContainingOrNameContaining(String username, String name);
    Optional<User> findByUsername(String username);

    boolean existsByUsername(String username);

    Page<User> findByUsernameContainingOrNameContainingAndRole(String username, String name, String role, Pageable pageable);

    default Page<User> search(String keyword, String role, Pageable pageable) {
        if (keyword != null && !keyword.trim().isEmpty() && role != null && !role.trim().isEmpty()) {
            return findByUsernameContainingOrNameContainingAndRole(keyword, keyword, role, pageable);
        }
        if (keyword != null && !keyword.trim().isEmpty()) {
            return findByUsernameContainingOrNameContaining(keyword, keyword, pageable);
        }
        if (role != null && !role.trim().isEmpty()) {
            return findByRole(role, pageable);
        }
        return findAll(pageable);
    }

    Page<User> findByUsernameContainingOrNameContaining(String username, String name, Pageable pageable);

    Page<User> findByRole(String role, Pageable pageable);
}
