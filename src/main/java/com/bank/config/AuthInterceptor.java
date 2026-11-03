package com.bank.config;

import com.bank.service.AuthService;
import com.bank.service.AuthService.Session;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Runs BEFORE every /api/** request (except login/register).
 * It reads the header "Authorization: Bearer <token>", finds who is logged in,
 * and stores the userId on the request so controllers can use @RequestAttribute("userId").
 */
@Component
public class AuthInterceptor implements HandlerInterceptor {

    private final AuthService authService;

    public AuthInterceptor(AuthService authService) {
        this.authService = authService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String header = request.getHeader("Authorization");
        String token = (header != null && header.startsWith("Bearer ")) ? header.substring(7) : null;
        Session session = authService.getSession(token);

        if (session == null) {
            return reject(response, 401, "Please login again");
        }
        if (request.getRequestURI().startsWith("/api/admin") && !"ADMIN".equals(session.role())) {
            return reject(response, 403, "Admin access only");
        }
        request.setAttribute("userId", session.userId());
        return true;   // continue to the controller
    }

    private boolean reject(HttpServletResponse response, int status, String message) throws Exception {
        response.setStatus(status);
        response.setContentType("application/json");
        response.getWriter().write("{\"message\":\"" + message + "\"}");
        return false;  // stop here, controller is NOT called
    }
}
