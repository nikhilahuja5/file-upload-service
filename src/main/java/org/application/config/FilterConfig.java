package org.application.config;

import org.application.security.RequestIdFilter;
import org.application.security.UserContext;
import org.application.security.UserContextFilter;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class FilterConfig {
    @Bean
    public FilterRegistrationBean<UserContextFilter>userContextFilterFilterRegistration(UserContext userContext){
        FilterRegistrationBean<UserContextFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new UserContextFilter(userContext));
        registration.addUrlPatterns("/api/*");
        registration.setOrder(1);
        return registration;
    }

    @Bean
    public FilterRegistrationBean<RequestIdFilter>requestIdFilterFilterRegistration(){
        FilterRegistrationBean<RequestIdFilter> registration = new FilterRegistrationBean<>();
        registration.setFilter(new RequestIdFilter());
        registration.addUrlPatterns("/api/*");
        registration.setOrder(0);
        return registration;
    }
}
