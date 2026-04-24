package com.thearena.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import com.thearena.service.TokenLifecycleService;
import java.io.IOException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class JwtAuthFilter extends OncePerRequestFilter {
    private final JwtService jwtService;
    private final TokenLifecycleService tokenLifecycleService;

    public JwtAuthFilter(JwtService jwtService, TokenLifecycleService tokenLifecycleService) {
        this.jwtService = jwtService;
        this.tokenLifecycleService = tokenLifecycleService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (jwtService.isValid(token) && !tokenLifecycleService.isAccessTokenRevoked(token)) {
                String username = jwtService.extractUsername(token);
                var auth = new UsernamePasswordAuthenticationToken(
                        username,
                        null,
                        AuthorityUtils.NO_AUTHORITIES
                );
                SecurityContextHolder.getContext().setAuthentication(auth);
            } else {
                SecurityContextHolder.clearContext();
                throw new BadCredentialsException("Invalid or expired Bearer token.");
            }
        }
        filterChain.doFilter(request, response);
    }
}
