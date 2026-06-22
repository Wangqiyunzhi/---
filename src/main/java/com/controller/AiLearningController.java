package com.controller;

import com.service.AiLearningService;
import com.utils.R;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Arrays;
import java.util.Collection;
import java.util.Map;

@RestController
@RequestMapping("/ai-learning")
public class AiLearningController {

    @Autowired
    private AiLearningService aiLearningService;

    @GetMapping("/materials")
    public R listMaterials(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.listMaterials(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/materials/save")
    public R saveMaterial(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.saveMaterial(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/materials/delete")
    public R deleteMaterials(@RequestBody Long[] ids, HttpServletRequest request) {
        try {
            aiLearningService.deleteMaterials(ids == null ? Arrays.<Long>asList() : Arrays.asList(ids), currentUser(request));
            return R.ok();
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/analyze-media")
    public R analyzeMedia(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.analyzeMedia(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/ask")
    public R ask(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.ask(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/teacher-dashboard")
    public R teacherDashboard(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.buildTeacherDashboard(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/student-report")
    public R studentReport(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.buildStudentReport(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/review-homework")
    public R reviewHomework(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            Long submissionId = body.get("submissionId") == null ? null : Long.valueOf(String.valueOf(body.get("submissionId")));
            return R.ok().put("data", aiLearningService.reviewHomework(submissionId, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/review-homework/{submissionId}")
    public R reviewHomeworkDetail(@PathVariable("submissionId") Long submissionId, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.getHomeworkReview(submissionId, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/storage-config")
    public R storageConfig(HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.getStorageConfig());
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/storage-config/save")
    public R saveStorageConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.saveStorageConfig(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/test-config")
    public R testAiConfig(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.testAiConfig(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    // -------- 功能1：AI公告生成 --------
    @PostMapping("/generate-notice")
    public R generateNotice(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.generateNotice(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    // -------- 功能2：作业反馈扩写 --------
    @PostMapping("/expand-feedback")
    public R expandFeedback(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.expandFeedback(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    // -------- 功能3：AI智能出题 --------
    @PostMapping("/quiz/generate")
    public R generateQuiz(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.generateQuiz(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/quiz/list")
    public R listQuiz(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.listQuiz(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/quiz/delete")
    public R deleteQuiz(@RequestBody Long[] ids, HttpServletRequest request) {
        try {
            aiLearningService.deleteQuiz(ids, currentUser(request));
            return R.ok();
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @PostMapping("/quiz/submit")
    public R submitQuizAnswer(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.submitQuizAnswer(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    // -------- 功能4：学情图表数据 --------
    @GetMapping("/student-chart")
    public R studentChart(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.getStudentChartData(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    // -------- 功能5：视频观看记录 --------
    @PostMapping("/video-watch/log")
    public R logVideoWatch(@RequestBody Map<String, Object> body, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.logVideoWatch(body, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    @GetMapping("/video-watch/stats")
    public R videoWatchStats(@RequestParam Map<String, Object> params, HttpServletRequest request) {
        try {
            return R.ok().put("data", aiLearningService.getVideoWatchStats(params, currentUser(request)));
        } catch (Exception e) {
            return R.error(e.getMessage());
        }
    }

    private AiLearningService.SessionUser currentUser(HttpServletRequest request) {
        Object userId = request.getAttribute("userId");
        Object tableName = request.getAttribute("tableName");
        Object username = request.getAttribute("username");
        Object role = request.getAttribute("role");

        if (userId == null) {
            userId = request.getSession().getAttribute("userId");
        }
        if (tableName == null) {
            tableName = request.getSession().getAttribute("tableName");
        }
        if (username == null) {
            username = request.getSession().getAttribute("username");
        }
        if (role == null) {
            role = request.getSession().getAttribute("role");
        }
        return aiLearningService.buildSessionUser(userId, tableName, username, role);
    }
}
