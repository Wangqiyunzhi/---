package com.interceptor;

import java.io.PrintWriter;

import com.alibaba.fastjson.JSONObject;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerInterceptor;

import com.annotation.IgnoreAuth;
import com.entity.TokenEntity;
import com.service.TokenService;
import com.utils.R;

/**
 * 权限(Token)验证
 */
@Component
public class AuthorizationInterceptor implements HandlerInterceptor {

    public static final String LOGIN_TOKEN_KEY = "Token";

    @Autowired
    private TokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        response.setHeader("Access-Control-Allow-Methods", "POST, GET, OPTIONS, DELETE");
        response.setHeader("Access-Control-Max-Age", "3600");
        response.setHeader("Access-Control-Allow-Credentials", "true");
        response.setHeader("Access-Control-Allow-Headers",
                "x-requested-with,request-source,Token, Origin,imgType, Content-Type, cache-control,postman-token,Cookie, Accept,authorization");
        response.setHeader("Access-Control-Allow-Origin", request.getHeader("Origin"));

        // ========= 0) OPTIONS 预检请求直接放行 =========
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }

        // ========= 1) 公告相关：学生端公开接口放行（不需要登录）=========
        // 这里按“URL包含”匹配，兼容 /weixin/gonggao/list 这种带项目名的情况
        String uri = request.getRequestURI();
        if (uri != null) {
            // 放行：公告列表、公告详情（学生端使用）
            // 你如果详情接口不是 info，而是 detail，就把 contains("/gonggao/detail") 也加上
            if (uri.contains("/gonggao/list")
                    || uri.contains("/gonggao/info")
                    || uri.contains("/gonggao/detail")) {
                return true;
            }
        }

        IgnoreAuth annotation;
        if (handler instanceof HandlerMethod) {
            annotation = ((HandlerMethod) handler).getMethodAnnotation(IgnoreAuth.class);
        } else {
            return true;
        }

        // 从header中获取token
        String token = request.getHeader(LOGIN_TOKEN_KEY);

        /**
         * 不需要验证权限的方法直接放过
         */
        if (annotation != null) {
            return true;
        }

        TokenEntity tokenEntity = null;
        if (StringUtils.isNotBlank(token)) {
            tokenEntity = tokenService.getTokenEntity(token);
        }

        if (tokenEntity != null) {
            request.getSession().setAttribute("userId", tokenEntity.getUserid());
            request.getSession().setAttribute("role", tokenEntity.getRole());
            request.getSession().setAttribute("tableName", tokenEntity.getTablename());
            request.getSession().setAttribute("username", tokenEntity.getUsername());
            return true;
        }

        PrintWriter writer = null;
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json; charset=utf-8");
        try {
            writer = response.getWriter();
            writer.print(JSONObject.toJSONString(R.error(401, "请先登录")));
        } finally {
            if (writer != null) {
                writer.close();
            }
        }

        return false;
    }
}
