package com.controller;

import com.annotation.IgnoreAuth;
import com.entity.GonggaoEntity;
import com.service.GonggaoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/gonggao")
public class GonggaoController {

    @Autowired
    private GonggaoService gonggaoService;

    @IgnoreAuth
    @GetMapping("/list")
    public Map<String, Object> list() {
        List<GonggaoEntity> data = gonggaoService.studentList();
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("data", data);
        return r;
    }

    @IgnoreAuth
    @GetMapping("/detail")
    public Map<String, Object> detail(@RequestParam Long id) {
        GonggaoEntity data = gonggaoService.detail(id);
        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("data", data);
        return r;
    }

    @GetMapping("/page")
    public Map<String, Object> page(HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        List<GonggaoEntity> data;
        if ("admin".equals(role)) {
            data = gonggaoService.adminList();
        } else if ("teacher".equals(role)) {
            data = gonggaoService.teacherList(u.userId);
        } else {
            // 理论上不会走到这（因为 /page 需要登录），但兜底
            data = gonggaoService.studentList();
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", 0);
        r.put("data", data);
        return r;
    }

    @PostMapping("/save")
    public Map<String, Object> save(@RequestBody GonggaoEntity g, HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        int n = 0;
        if ("admin".equals(role)) {
            n = gonggaoService.adminAdd(g, u.userId);
        } else if ("teacher".equals(role)) {
            n = gonggaoService.teacherAdd(g, u.userId);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", n > 0 ? 0 : 1);
        return r;
    }

    @PostMapping("/update")
    public Map<String, Object> update(@RequestBody GonggaoEntity g, HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        int n = 0;
        if ("admin".equals(role)) {
            n = gonggaoService.adminUpdate(g);
        } else if ("teacher".equals(role)) {
            n = gonggaoService.teacherUpdate(g, u.userId);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", n > 0 ? 0 : 1);
        return r;
    }

    @PostMapping("/delete")
    public Map<String, Object> delete(@RequestParam Long id, HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        int n = 0;
        if ("admin".equals(role)) {
            n = gonggaoService.adminDelete(id);
        } else if ("teacher".equals(role)) {
            n = gonggaoService.teacherDelete(id, u.userId);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", n > 0 ? 0 : 1);
        return r;
    }

    @PostMapping("/publish")
    public Map<String, Object> publish(@RequestParam Long id, HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        int n = 0;
        if ("admin".equals(role)) {
            n = gonggaoService.adminPublish(id);
        } else if ("teacher".equals(role)) {
            n = gonggaoService.teacherPublish(id, u.userId);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", n > 0 ? 0 : 1);
        return r;
    }

    @PostMapping("/unpublish")
    public Map<String, Object> unpublish(@RequestParam Long id, HttpServletRequest request) {
        LoginUser u = getLoginUser(request);
        String role = normRole(u.role);

        int n = 0;
        if ("admin".equals(role)) {
            n = gonggaoService.adminUnpublish(id);
        } else if ("teacher".equals(role)) {
            n = gonggaoService.teacherUnpublish(id, u.userId);
        }

        Map<String, Object> r = new HashMap<>();
        r.put("code", n > 0 ? 0 : 1);
        return r;
    }

    private static class LoginUser {
        String role;
        String username;
        Long userId;
    }

    /**
     * 关键：优先 request.getAttribute（多数 Token 拦截器 setAttribute）
     *      再兜底 session.getAttribute
     */
    private LoginUser getLoginUser(HttpServletRequest request) {
        // 1) request attribute
        Object role = request.getAttribute("role");
        Object username = request.getAttribute("username");
        Object userId = request.getAttribute("userId");

        if (role == null) role = request.getSession().getAttribute("role");
        if (username == null) username = request.getSession().getAttribute("username");
        if (userId == null) userId = request.getSession().getAttribute("userId");

        LoginUser u = new LoginUser();
        u.role = role == null ? "student" : String.valueOf(role);
        u.username = username == null ? "" : String.valueOf(username);

        if (userId == null || String.valueOf(userId).trim().length() == 0) {
            u.userId = null;
        } else {
            u.userId = Long.valueOf(String.valueOf(userId));
        }
        return u;
    }

    /**
     * 统一角色：中文/英文都兼容，最终只输出 admin/teacher/student
     */
    private String normRole(String role) {
        if (role == null) return "student";
        role = role.trim();
        if ("管理员".equals(role)) return "admin";
        if ("教师".equals(role)) return "teacher";
        if ("学生".equals(role)) return "student";
        return role.toLowerCase();
    }
}
