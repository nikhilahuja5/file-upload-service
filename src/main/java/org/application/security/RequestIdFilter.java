package org.application.security;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;

import java.io.IOException;
import java.util.UUID;


public class RequestIdFilter implements Filter {

    private static final String MDC_KEY = "requestId";
    private static final String HEADER_NAME = "X-Request-Id";

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        HttpServletResponse response = (HttpServletResponse) servletResponse;
        String requestId = UUID.randomUUID().toString();
        try{
            MDC.put(MDC_KEY,requestId);
            response.setHeader(HEADER_NAME,requestId);
            filterChain.doFilter(servletRequest,servletResponse);
        }finally {
            MDC.remove(MDC_KEY);
        }

    }
}
