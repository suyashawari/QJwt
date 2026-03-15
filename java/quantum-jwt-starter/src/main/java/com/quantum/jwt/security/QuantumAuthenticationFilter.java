package com.quantum.jwt.security;

import com.quantum.jwt.core.QuantumJwtService;
import com.quantum.jwt.exception.HoneyTokenException;
import com.quantum.jwt.exception.InvalidTokenException;
import io.jsonwebtoken.Claims;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Collections;

/**
 * Servlet filter that extracts a Bearer token, validates it using QuantumJwtService,
 * and sets the Spring Security authentication context.
 * <p>
 * This filter requires spring-boot-starter-security on the classpath.
 * To use it, register it in your security configuration.
 */
public class QuantumAuthenticationFilter extends OncePerRequestFilter {
    private static final Logger logger = LoggerFactory.getLogger(QuantumAuthenticationFilter.class);
    private final QuantumJwtService jwtService;

    public QuantumAuthenticationFilter(QuantumJwtService jwtService) {
        this.jwtService = jwtService;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain chain) throws ServletException, IOException {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            chain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        String clientIp = request.getRemoteAddr();

        try {
            Claims claims = jwtService.validateToken(token, clientIp);

            UsernamePasswordAuthenticationToken auth =
                    new UsernamePasswordAuthenticationToken(claims.getSubject(), null, Collections.emptyList());
            SecurityContextHolder.getContext().setAuthentication(auth);
        } catch (HoneyTokenException e) {
            logger.warn("Honey token detected from IP: {}", clientIp);
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Security violation");
            return;
        } catch (InvalidTokenException e) {
            logger.debug("Invalid token: {}", e.getMessage());
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return;
        }

        chain.doFilter(request, response);
    }
}