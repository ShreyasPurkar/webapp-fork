package com.webapp.shreyas_purkar_002325982.util;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class RequestFilter implements Filter {

    private static final List<String> DISALLOWED_METHODS = Arrays.asList("HEAD", "OPTIONS");

    @Override
    public void doFilter(ServletRequest servletRequest,
                         ServletResponse servletResponse,
                         FilterChain filterChain) throws IOException, ServletException {

        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        HttpServletResponse httpResponse = (HttpServletResponse) servletResponse;

        String method = httpRequest.getMethod();
        String requestURI = httpRequest.getRequestURI();

        httpResponse.setHeader("X-Content-Type-Options", "nosniff");
        httpResponse.setHeader("Cache-Control", "no-cache, no-store, must-revalidate");
        httpResponse.setHeader("Pragma", "no-cache");

        if (DISALLOWED_METHODS.contains(method) && (
                requestURI.startsWith("/v1/file") ||
                        requestURI.startsWith("/healthz"))) {

            httpResponse.setStatus(HttpServletResponse.SC_METHOD_NOT_ALLOWED);
            httpResponse.setContentType("application/json");
            httpResponse.getWriter().write("{ \"status\": 405, \"error\": \"Method Not Allowed\", \"message\": \"This HTTP method is not allowed for this endpoint.\" }");
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void destroy() {

    }
}