package org.application.security;


import jakarta.servlet.*;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

public class UserContextFilter implements Filter {
    private final UserContext userContext;
    public UserContextFilter(UserContext userContext){
        this.userContext = userContext;
    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletRequest request = (HttpServletRequest) servletRequest;
        HttpServletResponse response = (HttpServletResponse) servletResponse;

        String userId = request.getHeader("X-User-Id");
        if(userId == null || userId.isBlank()){
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED,"Missing X-User-Id header");
            return;
        }

        userContext.setUserId(userId);
        filterChain.doFilter(request,response);
    }
}
