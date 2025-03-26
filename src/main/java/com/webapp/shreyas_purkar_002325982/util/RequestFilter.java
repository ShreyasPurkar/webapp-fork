package com.webapp.shreyas_purkar_002325982.util;

import com.webapp.shreyas_purkar_002325982.service.impl.S3ServiceImpl;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@Component
public class RequestFilter implements Filter {

    private static final Logger log = LoggerFactory.getLogger(RequestFilter.class);

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
            log.warn("Method Not Allowed");
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