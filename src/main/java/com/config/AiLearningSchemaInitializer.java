package com.config;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class AiLearningSchemaInitializer implements InitializingBean {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Override
    public void afterPropertiesSet() {
        ensureColumn(
                "zuoyexinxi",
                "zuoyaoqiu",
                "ALTER TABLE zuoyexinxi ADD COLUMN zuoyaoqiu LONGTEXT"
        );
        ensureColumn(
                "zuoyetijiao",
                "pigaizhuangtai",
                "ALTER TABLE zuoyetijiao ADD COLUMN pigaizhuangtai VARCHAR(64) DEFAULT '待评分'"
        );
        ensureColumn(
                "zuoyetijiao",
                "pingfen",
                "ALTER TABLE zuoyetijiao ADD COLUMN pingfen DECIMAL(6,2) DEFAULT NULL"
        );
        ensureColumn(
                "zuoyetijiao",
                "pingyu",
                "ALTER TABLE zuoyetijiao ADD COLUMN pingyu LONGTEXT"
        );
        ensureColumn(
                "zuoyetijiao",
                "pigaishijian",
                "ALTER TABLE zuoyetijiao ADD COLUMN pigaishijian DATETIME"
        );
        ensureColumn(
                "zuoyetijiao",
                "pigaigonghao",
                "ALTER TABLE zuoyetijiao ADD COLUMN pigaigonghao VARCHAR(64)"
        );
        ensureColumn(
                "zuoyetijiao",
                "pigaijiaoshixingming",
                "ALTER TABLE zuoyetijiao ADD COLUMN pigaijiaoshixingming VARCHAR(128)"
        );

        ensureConfigValue("ai_user_enabled", "1");
        ensureConfigValue("ai_user_theme_color", "#009688");
        ensureConfigValue("ai_user_entry_title", "AI 智能助手");
        ensureConfigValue("ai_user_entry_subtitle", "课程、作业、学习问题都可以问我");
        ensureConfigValue("ai_user_placeholder", "请输入你想咨询的问题");
        ensureConfigValue("ai_user_fallback_text", "我暂时没理解清楚，请换个说法再试试。");
        ensureConfigValue("ai_user_avatar", "");
        ensureConfigValue("ai_user_welcome_text", "你好，我是你的课程 AI 助手。课程学习、作业提交、功能使用问题都可以直接问我。");
        ensureConfigValue("ai_user_system_prompt", "你是面向学生端的课程答疑助手。回答要简洁、友好、准确，优先围绕课程学习、作业和平台使用进行帮助；不确定时明确说明并引导用户补充信息。");
        ensureConfigValue("ai_user_quick_questions", "如何开始学习这门课？\n作业在哪里提交？\n课程视频打不开怎么办？");
        ensureConfigValue("ai_user_model_name", "");
        ensureConfigValue("ai_user_api_url", "");
        ensureConfigValue("ai_user_api_key", "");
        ensureConfigValue("ai_user_temperature", "0.7");

        execute("CREATE TABLE IF NOT EXISTS ai_material_center ("
                + "id BIGINT PRIMARY KEY,"
                + "title VARCHAR(255) NOT NULL,"
                + "material_type VARCHAR(64) DEFAULT 'document',"
                + "subject_name VARCHAR(128),"
                + "tags VARCHAR(255),"
                + "summary TEXT,"
                + "file_url VARCHAR(500),"
                + "file_name VARCHAR(255),"
                + "content_text LONGTEXT,"
                + "source_type VARCHAR(64),"
                + "source_table_name VARCHAR(64),"
                + "source_id BIGINT,"
                + "teacher_no VARCHAR(64),"
                + "teacher_name VARCHAR(64),"
                + "created_by BIGINT,"
                + "created_role VARCHAR(32),"
                + "addtime DATETIME,"
                + "updatetime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS ai_question_record ("
                + "id BIGINT PRIMARY KEY,"
                + "user_id BIGINT,"
                + "user_role VARCHAR(32),"
                + "username VARCHAR(64),"
                + "display_name VARCHAR(64),"
                + "question_text LONGTEXT,"
                + "answer_text LONGTEXT,"
                + "citations_json LONGTEXT,"
                + "attachments_json LONGTEXT,"
                + "knowledge_json LONGTEXT,"
                + "intent VARCHAR(64),"
                + "answer_mode VARCHAR(64),"
                + "related_subjects VARCHAR(255),"
                + "related_teacher_nos VARCHAR(255),"
                + "addtime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS ai_homework_review ("
                + "id BIGINT PRIMARY KEY,"
                + "submission_id BIGINT NOT NULL,"
                + "homework_id BIGINT,"
                + "student_id BIGINT,"
                + "student_no VARCHAR(64),"
                + "student_name VARCHAR(64),"
                + "teacher_no VARCHAR(64),"
                + "teacher_name VARCHAR(64),"
                + "subject_name VARCHAR(128),"
                + "score DECIMAL(6,2),"
                + "review_level VARCHAR(32),"
                + "overall_comment TEXT,"
                + "strengths_json LONGTEXT,"
                + "issues_json LONGTEXT,"
                + "suggestions_json LONGTEXT,"
                + "step_feedback_json LONGTEXT,"
                + "weak_tags VARCHAR(255),"
                + "review_status VARCHAR(32),"
                + "reviewed_by BIGINT,"
                + "reviewed_role VARCHAR(32),"
                + "addtime DATETIME,"
                + "updatetime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS ai_wrong_book ("
                + "id BIGINT PRIMARY KEY,"
                + "student_id BIGINT,"
                + "student_no VARCHAR(64),"
                + "student_name VARCHAR(64),"
                + "teacher_no VARCHAR(64),"
                + "teacher_name VARCHAR(64),"
                + "submission_id BIGINT,"
                + "homework_id BIGINT,"
                + "subject_name VARCHAR(128),"
                + "question_text TEXT,"
                + "error_reason TEXT,"
                + "weak_tag VARCHAR(128),"
                + "suggested_fix TEXT,"
                + "status VARCHAR(32),"
                + "source_review_id BIGINT,"
                + "addtime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS ai_quiz ("
                + "id BIGINT PRIMARY KEY,"
                + "subject_name VARCHAR(128),"
                + "quiz_type VARCHAR(32) DEFAULT 'choice',"
                + "question_text TEXT NOT NULL,"
                + "options_json LONGTEXT,"
                + "answer TEXT,"
                + "explanation TEXT,"
                + "difficulty VARCHAR(32) DEFAULT 'normal',"
                + "source_material_id BIGINT,"
                + "source_material_title VARCHAR(255),"
                + "teacher_no VARCHAR(64),"
                + "teacher_name VARCHAR(64),"
                + "created_by BIGINT,"
                + "created_role VARCHAR(32),"
                + "addtime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS ai_quiz_answer ("
                + "id BIGINT PRIMARY KEY,"
                + "quiz_id BIGINT NOT NULL,"
                + "student_id BIGINT,"
                + "student_no VARCHAR(64),"
                + "student_name VARCHAR(64),"
                + "answer TEXT,"
                + "is_correct TINYINT(1) DEFAULT 0,"
                + "score DECIMAL(6,2) DEFAULT 0,"
                + "addtime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");

        execute("CREATE TABLE IF NOT EXISTS video_watch_log ("
                + "id BIGINT PRIMARY KEY,"
                + "video_id BIGINT NOT NULL,"
                + "student_id BIGINT,"
                + "student_no VARCHAR(64),"
                + "student_name VARCHAR(64),"
                + "watched_seconds INT DEFAULT 0,"
                + "total_seconds INT DEFAULT 0,"
                + "last_position INT DEFAULT 0,"
                + "is_finished TINYINT(1) DEFAULT 0,"
                + "addtime DATETIME,"
                + "updatetime DATETIME"
                + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4");
    }

    private void execute(String sql) {
        jdbcTemplate.execute(sql);
    }

    private void ensureColumn(String tableName, String columnName, String alterSql) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM information_schema.COLUMNS WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = ? AND COLUMN_NAME = ?",
                Integer.class,
                tableName,
                columnName
        );
        if (count == null || count == 0) {
            jdbcTemplate.execute(alterSql);
        }
    }

    private void ensureConfigValue(String name, String value) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM config WHERE name = ?",
                Integer.class,
                name
        );
        if (count == null || count == 0) {
            jdbcTemplate.update("INSERT INTO config(name, value) VALUES(?, ?)", name, value);
        }
    }
}
