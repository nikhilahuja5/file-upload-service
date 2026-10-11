package org.application.security;

import org.springframework.stereotype.Component;
import org.springframework.web.context.annotation.RequestScope;

@Component
@RequestScope
public class UserContext {
    private String userId;

    public String getUserId(){return userId;}
    public void setUserId(String userId){this.userId=userId;}
}

