package com.lamp.security;

import com.lamp.util.JwtUtil;
import io.jsonwebtoken.Claims;
import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private static final List<String> SKIP_PATHS = Arrays.asList(
            "/auth/login",
            "/auth/register"
    );

    private final JwtUtil jwtUtil;

    public JwtAuthFilter(JwtUtil jwtUtil) {
        this.jwtUtil = jwtUtil;
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {
        String path = request.getRequestURI();
        if (path != null && path.startsWith("/api")) {
            path = path.substring(4);
        }
        if (path == null) path = "";

        boolean skip = SKIP_PATHS.stream().anyMatch(path::equals);
        if (skip) {
            filterChain.doFilter(request, response);
            return;
        }

        String auth = request.getHeader("Authorization");
        String token = null;
        if (StringUtils.hasText(auth) && auth.startsWith("Bearer ")) {
            token = auth.substring(7);
        }
        if (StringUtils.hasText(token)) {
            try {
                Claims claims = jwtUtil.parseToken(token);
                Long userId = Long.parseLong(claims.getSubject());
                String username = claims.get("username", String.class);
                String role = claims.get("role", String.class);
                UserContext.set(userId, username, role);
            } catch (Exception ignored) {
            }
        }
        if (!skip && !UserContext.isLoggedIn()) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType("application/json;charset=UTF-8");
            response.getWriter().write("{\"code\":401,\"message\":\"未登录或登录已过期\",\"data\":null}");
            return;
        }
        try {
            filterChain.doFilter(request, response);
        } finally {
            UserContext.clear();
        }
    }
}
