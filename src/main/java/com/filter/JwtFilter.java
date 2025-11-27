package com.filter;

import com.enums.Role;
import com.security.AuthUserPrincipal;
import com.services.JwtService;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        System.out.println("JwtFilter: request URI = " + req.getRequestURI());
        System.out.println("JwtFilter: Authorization header = " + req.getHeader(HttpHeaders.AUTHORIZATION));
        String header = req.getHeader(HttpHeaders.AUTHORIZATION);
        if (header == null || !header.startsWith("Bearer ")) {
            chain.doFilter(req, res);
            return;
        }

        String token = header.substring(7);
        System.out.println("JwtFilter: token = " + token);

        if (!jwtService.validate(token)) {
            res.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid or expired token");
            return;
        }

        Claims claims = jwtService.parse(token);
        System.out.println("JwtFilter: claims = " + claims);
        System.out.println("JwtFilter: role from claims = " + claims.get("role", String.class));
        System.out.println("JwtFilter: subject from claims = " + claims.getSubject());
        Long userId = Long.valueOf(claims.getSubject());
        Role role = Role.valueOf(claims.get("role", String.class)); // конвертируем роль в Enum

        var principal = new AuthUserPrincipal(userId, role);
        var auth = new UsernamePasswordAuthenticationToken(
                principal,
                null,
                List.of(principal.getAuthorities().iterator().next()) // Spring Security expects GrantedAuthority
        );

        SecurityContextHolder.getContext().setAuthentication(auth);
        chain.doFilter(req, res);
    }
}
