package com.service;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.baidu.aip.ocr.AipOcr;
import com.baomidou.mybatisplus.mapper.EntityWrapper;
import com.entity.ConfigEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.poi.hslf.usermodel.HSLFSlide;
import org.apache.poi.hslf.usermodel.HSLFSlideShow;
import org.apache.poi.hslf.usermodel.HSLFTextParagraph;
import org.apache.poi.hslf.usermodel.HSLFTextRun;
import org.apache.poi.hwpf.HWPFDocument;
import org.apache.poi.hwpf.extractor.WordExtractor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xwpf.extractor.XWPFWordExtractor;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.json.JSONException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Timestamp;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service("aiLearningService")
public class AiLearningService {

    private static final List<String> AI_BASE_KEYS = Arrays.asList("ai_user_api_url", "ai_user_api_base", "ai_api_base", "ai_base_url", "openai_api_base", "llm_api_base");
    private static final List<String> AI_KEY_KEYS = Arrays.asList("ai_user_api_key", "ai_api_key", "ai_key", "openai_api_key", "llm_api_key");
    private static final List<String> AI_MODEL_KEYS = Arrays.asList("ai_user_model_name", "ai_model", "llm_model", "openai_model");
    private static final List<String> AI_PATH_KEYS = Arrays.asList("ai_user_api_path", "ai_api_path", "ai_path", "llm_api_path");
    private static final List<String> AI_PROMPT_KEYS = Arrays.asList("ai_user_system_prompt", "ai_system_prompt", "ai_prompt", "llm_system_prompt");
    private static final List<String> AI_TEMPERATURE_KEYS = Arrays.asList("ai_user_temperature", "ai_temperature", "llm_temperature", "openai_temperature");
    private static final Pattern CHINESE_BLOCK_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]{2,}");
    private static final Pattern ASCII_TOKEN_PATTERN = Pattern.compile("[A-Za-z0-9]{2,}");
    private static final Set<String> KEYWORD_STOP_WORDS = new LinkedHashSet<String>(Arrays.asList(
            "请问", "什么", "怎么", "一下", "这个", "那个", "这里", "可以", "是否", "一下子",
            "最近", "今天", "同学", "一下吧", "一下吗", "一下呢",
            "呢", "吗", "呀", "啊", "啦", "和", "与", "及", "的", "了", "是", "在", "把"
    ));

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private ConfigService configService;

    @Value("${file.upload-dir:${app.upload-dir:C:/w001/weixin_upload}}")
    private String uploadDir;

    public SessionUser buildSessionUser(Object userIdAttr, Object tableNameAttr, Object usernameAttr, Object roleAttr) {
        SessionUser user = new SessionUser();
        user.userId = parseLong(userIdAttr);
        user.tableName = normalizeTableName(stringValue(tableNameAttr), stringValue(roleAttr));
        user.username = stringValue(usernameAttr);
        user.roleLabel = stringValue(roleAttr);
        user.roleKey = normalizeRoleKey(user.tableName, user.roleLabel);
        user.displayName = user.username;

        if ("xuesheng".equals(user.tableName)) {
            Map<String, Object> profile = findSingle("select id, xuehao, xueshengxingming from xuesheng where id=? limit 1", user.userId);
            if (profile.isEmpty() && StringUtils.isNotBlank(user.username)) {
                profile = findSingle("select id, xuehao, xueshengxingming from xuesheng where xuehao=? limit 1", user.username);
            }
            user.userId = firstNonNull(user.userId, parseLong(profile.get("id")));
            user.studentNo = stringValue(profile.get("xuehao"));
            user.studentName = stringValue(profile.get("xueshengxingming"));
            user.displayName = StringUtils.defaultIfBlank(user.studentName, StringUtils.defaultIfBlank(user.studentNo, user.username));
        } else if ("jiaoshi".equals(user.tableName)) {
            Map<String, Object> profile = findSingle("select id, gonghao, jiaoshixingming from jiaoshi where id=? limit 1", user.userId);
            if (profile.isEmpty() && StringUtils.isNotBlank(user.username)) {
                profile = findSingle("select id, gonghao, jiaoshixingming from jiaoshi where gonghao=? limit 1", user.username);
            }
            user.userId = firstNonNull(user.userId, parseLong(profile.get("id")));
            user.teacherNo = stringValue(profile.get("gonghao"));
            user.teacherName = stringValue(profile.get("jiaoshixingming"));
            user.displayName = StringUtils.defaultIfBlank(user.teacherName, StringUtils.defaultIfBlank(user.teacherNo, user.username));
        }
        return user;
    }

    public Map<String, Object> listMaterials(Map<String, Object> params, SessionUser user) {
        int page = Math.max(parseInt(params.get("page"), 1), 1);
        int limit = Math.min(Math.max(parseInt(params.get("limit"), 10), 1), 50);
        int offset = (page - 1) * limit;
        String keyword = stringValue(params.get("keyword"));
        String subjectName = stringValue(params.get("subjectName"));
        boolean onlyMine = "true".equalsIgnoreCase(stringValue(params.get("onlyMine"))) || "1".equals(stringValue(params.get("onlyMine")));

        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<Object>();
        if (StringUtils.isNotBlank(keyword)) {
            where.append(" and (title like ? or subject_name like ? or tags like ? or summary like ? or content_text like ?)");
            for (int i = 0; i < 5; i++) {
                args.add(likeValue(keyword));
            }
        }
        if (StringUtils.isNotBlank(subjectName)) {
            where.append(" and subject_name like ?");
            args.add(likeValue(subjectName));
        }
        if (onlyMine && user != null) {
            if (user.isTeacher()) {
                where.append(" and (teacher_no=? or created_by=?)");
                args.add(user.teacherNo);
                args.add(user.userId);
            } else if (!user.isAdmin()) {
                where.append(" and created_by=?");
                args.add(user.userId);
            }
        }

        long total = jdbcTemplate.queryForObject("select count(*) from ai_material_center" + where, args.toArray(), Long.class);
        List<Object> pageArgs = new ArrayList<Object>(args);
        pageArgs.add(offset);
        pageArgs.add(limit);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
                "select * from ai_material_center" + where + " order by updatetime desc, addtime desc limit ?, ?",
                pageArgs.toArray()
        );
        for (Map<String, Object> item : list) {
            item.put("contentPreview", excerpt(item.get("content_text"), 120));
            item.put("summaryPreview", excerpt(firstNonBlank(stringValue(item.get("summary")), stringValue(item.get("content_text"))), 120));
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("limit", limit);
        data.put("canManage", user != null && (user.isTeacher() || user.isAdmin()));
        return data;
    }

    public Map<String, Object> saveMaterial(Map<String, Object> body, SessionUser user) {
        requireTeacherOrAdmin(user, "当前账号不能维护资料库");

        Long id = parseLong(body.get("id"));
        boolean update = id != null;
        if (!update) {
            id = newId();
        }

        String title = stringValue(body.get("title"));
        if (StringUtils.isBlank(title)) {
            throw new IllegalArgumentException("资料标题不能为空");
        }

        String materialType = firstNonBlank(stringValue(body.get("materialType")), "document");
        String subjectName = stringValue(body.get("subjectName"));
        String tags = stringValue(body.get("tags"));
        String summary = stringValue(body.get("summary"));
        String fileUrl = stringValue(body.get("fileUrl"));
        String fileName = stringValue(body.get("fileName"));
        String contentText = normalizePlainText(stringValue(body.get("contentText")));

        if (StringUtils.isBlank(contentText) && StringUtils.isNotBlank(fileUrl)) {
            contentText = normalizePlainText(extractTextFromAsset(fileUrl, fileName, materialType));
        }
        if (StringUtils.isBlank(summary)) {
            summary = excerpt(firstNonBlank(contentText, fileName), 160);
        }

        Date now = new Date();
        if (update) {
            jdbcTemplate.update(
                    "update ai_material_center set title=?, material_type=?, subject_name=?, tags=?, summary=?, file_url=?, file_name=?, content_text=?, teacher_no=?, teacher_name=?, created_by=?, created_role=?, updatetime=? where id=?",
                    title, materialType, nullIfBlank(subjectName), nullIfBlank(tags), nullIfBlank(summary),
                    nullIfBlank(fileUrl), nullIfBlank(fileName), nullIfBlank(contentText),
                    nullIfBlank(user.teacherNo), nullIfBlank(user.teacherName), user.userId,
                    user.roleKey, now, id
            );
        } else {
            jdbcTemplate.update(
                    "insert into ai_material_center(id, title, material_type, subject_name, tags, summary, file_url, file_name, content_text, teacher_no, teacher_name, created_by, created_role, addtime, updatetime) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    id, title, materialType, nullIfBlank(subjectName), nullIfBlank(tags), nullIfBlank(summary),
                    nullIfBlank(fileUrl), nullIfBlank(fileName), nullIfBlank(contentText),
                    nullIfBlank(user.teacherNo), nullIfBlank(user.teacherName), user.userId, user.roleKey, now, now
            );
        }

        Map<String, Object> saved = findSingle("select * from ai_material_center where id=? limit 1", id);
        saved.put("contentPreview", excerpt(saved.get("content_text"), 120));
        return saved;
    }

    public void deleteMaterials(Collection<Long> ids, SessionUser user) {
        requireTeacherOrAdmin(user, "当前账号不能删除资料");
        if (ids == null || ids.isEmpty()) {
            return;
        }
        for (Long id : ids) {
            if (id == null) {
                continue;
            }
            if (user.isAdmin()) {
                jdbcTemplate.update("delete from ai_material_center where id=?", id);
            } else {
                jdbcTemplate.update("delete from ai_material_center where id=? and (teacher_no=? or created_by=?)", id, user.teacherNo, user.userId);
            }
        }
    }

    public Map<String, Object> analyzeMedia(Map<String, Object> body, SessionUser user) {
        String fileUrl = stringValue(body.get("fileUrl"));
        String fileName = stringValue(body.get("fileName"));
        String mediaType = firstNonBlank(stringValue(body.get("mediaType")), detectMediaType(fileName, fileUrl));
        String extractedText = normalizePlainText(firstNonBlank(stringValue(body.get("contentText")), extractTextFromAsset(fileUrl, fileName, mediaType)));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("fileUrl", fileUrl);
        data.put("fileName", fileName);
        data.put("mediaType", mediaType);
        data.put("extractedText", extractedText);
        data.put("summary", excerpt(extractedText, 120));
        data.put("hint", buildMediaHint(mediaType, extractedText));
        data.put("supportsAutomaticParse", supportsAutomaticParse(fileName, mediaType));
        data.put("supportsOcr", supportsOcr());
        data.put("supportsAudioTranscription", false);
        return data;
    }

    public Map<String, Object> ask(Map<String, Object> body, SessionUser user) {
        String question = normalizePlainText(stringValue(body.get("question")));
        if (StringUtils.isBlank(question)) {
            throw new IllegalArgumentException("问题内容不能为空");
        }

        List<Map<String, Object>> attachments = normalizeAttachmentList(body.get("attachments"));
        String attachmentContext = buildAttachmentContext(attachments);
        String intent = detectIntent(question);
        List<Map<String, Object>> citations = searchKnowledge(question, attachmentContext, intent);
        List<String> actions = buildSuggestedActions(intent, citations, user);

        String aiAnswer = callConfiguredAi(question, citations, attachments);
        String answerMode = StringUtils.isNotBlank(aiAnswer) ? "configured_ai" : "local_retrieval";
        String answer = StringUtils.isNotBlank(aiAnswer)
                ? ensureCitationFooter(aiAnswer, citations)
                : buildLocalAnswer(question, citations, attachmentContext, actions);

        Map<String, Object> response = new LinkedHashMap<String, Object>();
        response.put("answer", answer);
        response.put("citations", citations);
        response.put("actions", actions);
        response.put("answerMode", answerMode);
        response.put("intent", intent);
        response.put("knowledgeSummary", buildKnowledgeSummary(citations));

        saveQuestionRecord(question, answer, citations, attachments, intent, answerMode, user);
        return response;
    }

    public Map<String, Object> buildTeacherDashboard(Map<String, Object> params, SessionUser user) {
        requireTeacherOrAdmin(user, "当前账号不能查看教师工作台");

        String teacherNo = user.isTeacher() ? user.teacherNo : stringValue(params.get("teacherNo"));
        List<Object> submissionArgs = new ArrayList<Object>();
        String submissionWhere = "";
        if (StringUtils.isNotBlank(teacherNo)) {
            submissionWhere = " where gonghao=?";
            submissionArgs.add(teacherNo);
        }

        Integer totalSubmissions = jdbcTemplate.queryForObject("select count(*) from zuoyetijiao" + submissionWhere, submissionArgs.toArray(), Integer.class);
        Integer reviewedSubmissions = jdbcTemplate.queryForObject(
                "select count(distinct submission_id) from ai_homework_review" + (StringUtils.isNotBlank(teacherNo) ? " where teacher_no=?" : ""),
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{},
                Integer.class
        );
        Integer materialCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_material_center" + (StringUtils.isNotBlank(teacherNo) ? " where teacher_no=?" : ""),
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{},
                Integer.class
        );
        Integer questionCount = jdbcTemplate.queryForObject(
                "select count(*) from ai_question_record" + (StringUtils.isNotBlank(teacherNo) ? " where related_teacher_nos like ?" : ""),
                StringUtils.isNotBlank(teacherNo) ? new Object[]{likeValue(teacherNo)} : new Object[]{},
                Integer.class
        );
        int pendingCount = Math.max((totalSubmissions == null ? 0 : totalSubmissions) - (reviewedSubmissions == null ? 0 : reviewedSubmissions), 0);

        List<Map<String, Object>> pending = jdbcTemplate.queryForList(
                "select z.id, z.zuoyemingcheng, z.kemu, z.xueshengxingming, z.xuehao, z.tijiaoshijian "
                        + "from zuoyetijiao z left join ai_homework_review r on z.id=r.submission_id "
                        + (StringUtils.isNotBlank(teacherNo) ? "where z.gonghao=? and r.id is null " : "where r.id is null ")
                        + "order by z.tijiaoshijian desc limit 8",
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{}
        );

        List<Map<String, Object>> heatRows = jdbcTemplate.queryForList(
                "select ifnull(kemu,'未分类') as subject_name, count(*) as total from zuoyetijiao"
                        + submissionWhere + " group by ifnull(kemu,'未分类') order by total desc limit 8",
                submissionArgs.toArray()
        );
        Map<String, Integer> weakCountMap = new HashMap<String, Integer>();
        List<Map<String, Object>> weakRows = jdbcTemplate.queryForList(
                "select ifnull(subject_name,'未分类') as subject_name, count(*) as total from ai_wrong_book"
                        + (StringUtils.isNotBlank(teacherNo) ? " where teacher_no=?" : "")
                        + " group by ifnull(subject_name,'未分类')",
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{}
        );
        for (Map<String, Object> row : weakRows) {
            weakCountMap.put(stringValue(row.get("subject_name")), parseInt(row.get("total"), 0));
        }
        List<Map<String, Object>> subjectHeat = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : heatRows) {
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            String subjectName = stringValue(row.get("subject_name"));
            item.put("subjectName", subjectName);
            item.put("submissionCount", parseInt(row.get("total"), 0));
            item.put("weakCount", weakCountMap.containsKey(subjectName) ? weakCountMap.get(subjectName) : 0);
            subjectHeat.add(item);
        }

        List<Map<String, Object>> recentQuestions = jdbcTemplate.queryForList(
                "select question_text from ai_question_record"
                        + (StringUtils.isNotBlank(teacherNo) ? " where related_teacher_nos like ?" : "")
                        + " order by addtime desc limit 80",
                StringUtils.isNotBlank(teacherNo) ? new Object[]{likeValue(teacherNo)} : new Object[]{}
        );
        List<Map<String, Object>> frequentQuestions = buildFrequentQuestions(recentQuestions);

        List<Map<String, Object>> weakFocus = jdbcTemplate.queryForList(
                "select weak_tag, count(*) as total from ai_wrong_book"
                        + (StringUtils.isNotBlank(teacherNo) ? " where teacher_no=? and weak_tag is not null and weak_tag<>''" : " where weak_tag is not null and weak_tag<>''")
                        + " group by weak_tag order by total desc limit 8",
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{}
        );

        List<Map<String, Object>> recentMaterials = jdbcTemplate.queryForList(
                "select id, title, subject_name, material_type, updatetime from ai_material_center"
                        + (StringUtils.isNotBlank(teacherNo) ? " where teacher_no=?" : "")
                        + " order by updatetime desc limit 5",
                StringUtils.isNotBlank(teacherNo) ? new Object[]{teacherNo} : new Object[]{}
        );

        Map<String, Object> overview = new LinkedHashMap<String, Object>();
        overview.put("totalSubmissions", valueOrZero(totalSubmissions));
        overview.put("reviewedSubmissions", valueOrZero(reviewedSubmissions));
        overview.put("pendingSubmissions", pendingCount);
        overview.put("materialCount", valueOrZero(materialCount));
        overview.put("questionCount", valueOrZero(questionCount));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("overview", overview);
        data.put("pendingSubmissions", pending);
        data.put("subjectHeat", subjectHeat);
        data.put("frequentQuestions", frequentQuestions);
        data.put("weakFocus", weakFocus);
        data.put("recentMaterials", recentMaterials);
        data.put("classroomSummary", buildClassroomSummary(overview, subjectHeat, weakFocus));
        return data;
    }

    public Map<String, Object> buildStudentReport(Map<String, Object> params, SessionUser user) {
        if (user == null || user.userId == null) {
            throw new IllegalArgumentException("请先登录后查看学习中心");
        }

        Long targetStudentId = user.userId;
        if (!user.isStudent() && params.get("studentId") != null) {
            targetStudentId = parseLong(params.get("studentId"));
        }

        Map<String, Object> profile = findSingle("select id, xuehao, xueshengxingming from xuesheng where id=? limit 1", targetStudentId);
        if (profile.isEmpty()) {
            throw new IllegalArgumentException("未找到对应的学生信息");
        }

        Integer reviewCount = jdbcTemplate.queryForObject("select count(*) from ai_homework_review where student_id=?", new Object[]{targetStudentId}, Integer.class);
        Integer wrongCount = jdbcTemplate.queryForObject("select count(*) from ai_wrong_book where student_id=?", new Object[]{targetStudentId}, Integer.class);
        Integer questionCount = jdbcTemplate.queryForObject("select count(*) from ai_question_record where user_id=?", new Object[]{targetStudentId}, Integer.class);
        Integer materialCount = jdbcTemplate.queryForObject("select count(*) from ai_material_center", Integer.class);

        List<Map<String, Object>> weakPoints = jdbcTemplate.queryForList(
                "select weak_tag, count(*) as total from ai_wrong_book where student_id=? and weak_tag is not null and weak_tag<>'' group by weak_tag order by total desc limit 8",
                targetStudentId
        );
        List<Map<String, Object>> latestReviews = jdbcTemplate.queryForList(
                "select submission_id, subject_name, score, review_level, overall_comment, updatetime from ai_homework_review where student_id=? order by updatetime desc limit 5",
                targetStudentId
        );
        List<Map<String, Object>> wrongBook = jdbcTemplate.queryForList(
                "select id, subject_name, question_text, error_reason, weak_tag, suggested_fix, addtime from ai_wrong_book where student_id=? order by addtime desc limit 20",
                targetStudentId
        );
        List<Map<String, Object>> subjectStats = jdbcTemplate.queryForList(
                "select ifnull(subject_name,'未分类') as subject_name, round(avg(score),1) as avg_score, count(*) as total from ai_homework_review where student_id=? group by ifnull(subject_name,'未分类') order by avg_score asc limit 8",
                targetStudentId
        );

        Map<String, Object> overview = new LinkedHashMap<String, Object>();
        overview.put("reviewCount", valueOrZero(reviewCount));
        overview.put("wrongCount", valueOrZero(wrongCount));
        overview.put("questionCount", valueOrZero(questionCount));
        overview.put("materialCount", valueOrZero(materialCount));

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("studentName", stringValue(profile.get("xueshengxingming")));
        data.put("studentNo", stringValue(profile.get("xuehao")));
        data.put("overview", overview);
        data.put("weakPoints", weakPoints);
        data.put("subjectStats", subjectStats);
        data.put("latestReviews", latestReviews);
        data.put("wrongBook", wrongBook);
        data.put("practiceList", buildPracticeList(weakPoints, subjectStats));
        data.put("growthSummary", buildGrowthSummary(profile, overview, weakPoints, latestReviews));
        return data;
    }

    public Map<String, Object> reviewHomework(Long submissionId, SessionUser user) {
        if (submissionId == null) {
            throw new IllegalArgumentException("缺少作业提交编号");
        }
        Map<String, Object> submission = findSingle("select * from zuoyetijiao where id=? limit 1", submissionId);
        if (submission.isEmpty()) {
            throw new IllegalArgumentException("未找到作业提交记录");
        }

        String homeworkName = stringValue(submission.get("zuoyemingcheng"));
        String subjectName = stringValue(submission.get("kemu"));
        String teacherNo = stringValue(submission.get("gonghao"));
        String teacherName = stringValue(submission.get("jiaoshixingming"));
        Long studentId = parseLong(submission.get("userid"));
        String studentNo = stringValue(submission.get("xuehao"));
        String studentName = stringValue(submission.get("xueshengxingming"));
        Date submitTime = parseDate(submission.get("tijiaoshijian"));
        String contentText = normalizePlainText(stripHtml(stringValue(submission.get("wanchengneirong"))));
        boolean hasImage = StringUtils.isNotBlank(stringValue(submission.get("zuoyetupian")));

        Map<String, Object> homework = findSingle(
                "select id, zuoyemingcheng, kemu, gonghao, jieshushijian, kaishishijian from zuoyexinxi where zuoyemingcheng=? and kemu=? order by addtime desc limit 1",
                homeworkName, subjectName
        );
        if (homework.isEmpty() && StringUtils.isNotBlank(teacherNo)) {
            homework = findSingle(
                    "select id, zuoyemingcheng, kemu, gonghao, jieshushijian, kaishishijian from zuoyexinxi where zuoyemingcheng=? and gonghao=? order by addtime desc limit 1",
                    homeworkName, teacherNo
            );
        }

        Date deadline = parseDate(homework.get("jieshushijian"));
        boolean isLate = deadline != null && submitTime != null && submitTime.after(deadline);
        boolean hasEnoughText = contentText.length() >= 80;
        boolean hasBasicText = contentText.length() >= 30;
        boolean hasStepWords = containsAny(contentText, Arrays.asList("第一", "首先", "然后", "最后", "步骤", "过程", "因为", "所以", "结论"));

        int score = 68;
        if (hasEnoughText) {
            score += 12;
        } else if (hasBasicText) {
            score += 6;
        } else {
            score -= 14;
        }
        if (hasImage) {
            score += 6;
        } else {
            score -= 4;
        }
        if (hasStepWords) {
            score += 6;
        } else {
            score -= 6;
        }
        if (isLate) {
            score -= 12;
        } else {
            score += 6;
        }
        score = Math.max(40, Math.min(score, 98));

        List<String> strengths = new ArrayList<String>();
        if (!isLate) {
            strengths.add("按时完成了本次作业提交，时间管理表现稳定。");
        }
        if (hasEnoughText) {
            strengths.add("文字说明比较完整，能看出你在认真梳理解题或完成过程。");
        } else if (hasBasicText) {
            strengths.add("已经写出了核心内容，具备进一步完善答案的基础。");
        }
        if (hasImage) {
            strengths.add("上传了作业图片或过程材料，方便老师和系统核对你的完成情况。");
        }
        if (hasStepWords) {
            strengths.add("作答中带有步骤意识，说明你在尝试把思路拆解出来。");
        }
        if (strengths.isEmpty()) {
            strengths.add("已经完成了这次提交，接下来重点提升表达完整度和过程说明。");
        }

        List<String> issues = new ArrayList<String>();
        if (!hasBasicText) {
            issues.add("文字说明偏少，当前提交还不足以完整呈现你的解题或完成思路。");
        }
        if (!hasStepWords) {
            issues.add("解题步骤和关键推理不够明确，老师复核时不容易判断你是如何得到结论的。");
        }
        if (!hasImage) {
            issues.add("缺少过程图片或截图，提交留痕还可以再加强。");
        }
        if (isLate) {
            issues.add("本次提交已经晚于作业截止时间，后续要特别注意作业节奏。");
        }
        if (issues.isEmpty()) {
            issues.add("整体完成度较好，下一步可以把关键知识点和最终结论写得更聚焦。");
        }

        LinkedHashSet<String> weakTags = new LinkedHashSet<String>();
        if (!hasBasicText) {
            weakTags.add("表达不完整");
        }
        if (!hasStepWords) {
            weakTags.add("解题步骤");
        }
        if (!hasImage) {
            weakTags.add("过程留痕");
        }
        if (isLate) {
            weakTags.add("时间管理");
        }
        if (StringUtils.isNotBlank(subjectName)) {
            weakTags.add(subjectName);
        }

        List<String> suggestions = new ArrayList<String>();
        suggestions.add(hasEnoughText
                ? "保留你现在的完整度，同时把每一步对应的知识点标出来，老师会更容易给出针对性反馈。"
                : "下次至少补足“题目要求、你的思路、最终结论”三段说明，让作业更完整。");
        if (!hasStepWords) {
            suggestions.add("建议按“已知条件 - 解题步骤 - 最终答案”三段写法组织作答。");
        }
        if (!hasImage) {
            suggestions.add("如果是手写或演算题，补充清晰图片能明显提升批改效率。");
        }
        if (isLate) {
            suggestions.add("把截止时间提前一天加入提醒，优先完成快到期作业。");
        }
        if (StringUtils.isNotBlank(subjectName)) {
            suggestions.add("完成后可以再去 AI 答疑里追问“请根据" + subjectName + "这次作业帮我生成 3 道强化练习”。");
        }

        List<String> stepFeedback = buildStepFeedback(contentText, subjectName);
        String reviewLevel = score >= 90 ? "A" : (score >= 75 ? "B" : "C");
        String reviewStatus = user != null && (user.isTeacher() || user.isAdmin()) ? "teacher_reviewed" : "self_reviewed";
        String overallComment = buildOverallReviewComment(homeworkName, subjectName, score, reviewLevel, strengths, issues, isLate);
        String weakTagValue = joinCollection(weakTags, ",");

        Date now = new Date();
        Map<String, Object> existing = findSingle("select id from ai_homework_review where submission_id=? limit 1", submissionId);
        Long reviewId = parseLong(existing.get("id"));
        if (reviewId == null) {
            reviewId = newId();
            jdbcTemplate.update(
                    "insert into ai_homework_review(id, submission_id, homework_id, student_id, student_no, student_name, teacher_no, teacher_name, subject_name, score, review_level, overall_comment, strengths_json, issues_json, suggestions_json, step_feedback_json, weak_tags, review_status, reviewed_by, reviewed_role, addtime, updatetime) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    reviewId, submissionId, parseLong(homework.get("id")), studentId, nullIfBlank(studentNo), nullIfBlank(studentName),
                    nullIfBlank(teacherNo), nullIfBlank(teacherName), nullIfBlank(subjectName), score, reviewLevel, overallComment,
                    JSON.toJSONString(strengths), JSON.toJSONString(issues), JSON.toJSONString(suggestions), JSON.toJSONString(stepFeedback),
                    weakTagValue, reviewStatus, user == null ? null : user.userId, user == null ? null : user.roleKey, now, now
            );
        } else {
            jdbcTemplate.update(
                    "update ai_homework_review set homework_id=?, student_id=?, student_no=?, student_name=?, teacher_no=?, teacher_name=?, subject_name=?, score=?, review_level=?, overall_comment=?, strengths_json=?, issues_json=?, suggestions_json=?, step_feedback_json=?, weak_tags=?, review_status=?, reviewed_by=?, reviewed_role=?, updatetime=? where id=?",
                    parseLong(homework.get("id")), studentId, nullIfBlank(studentNo), nullIfBlank(studentName), nullIfBlank(teacherNo),
                    nullIfBlank(teacherName), nullIfBlank(subjectName), score, reviewLevel, overallComment, JSON.toJSONString(strengths),
                    JSON.toJSONString(issues), JSON.toJSONString(suggestions), JSON.toJSONString(stepFeedback), weakTagValue, reviewStatus,
                    user == null ? null : user.userId, user == null ? null : user.roleKey, now, reviewId
            );
        }

        if (studentId != null) {
            jdbcTemplate.update("delete from ai_wrong_book where submission_id=?", submissionId);
            for (String issue : issues) {
                String weakTag = firstWeakTagForIssue(issue, weakTags);
                jdbcTemplate.update(
                        "insert into ai_wrong_book(id, student_id, student_no, student_name, teacher_no, teacher_name, submission_id, homework_id, subject_name, question_text, error_reason, weak_tag, suggested_fix, status, source_review_id, addtime) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        newId(), studentId, nullIfBlank(studentNo), nullIfBlank(studentName), nullIfBlank(teacherNo), nullIfBlank(teacherName),
                        submissionId, parseLong(homework.get("id")), nullIfBlank(subjectName), nullIfBlank(homeworkName), issue, nullIfBlank(weakTag),
                        suggestions.isEmpty() ? null : suggestions.get(0), "open", reviewId, now
                );
            }
        }

        return getHomeworkReview(submissionId, user);
    }

    public Map<String, Object> getHomeworkReview(Long submissionId, SessionUser user) {
        Map<String, Object> review = findSingle("select * from ai_homework_review where submission_id=? limit 1", submissionId);
        if (review.isEmpty()) {
            return new LinkedHashMap<String, Object>();
        }
        review.put("strengths", parseJsonArray(review.get("strengths_json")));
        review.put("issues", parseJsonArray(review.get("issues_json")));
        review.put("suggestions", parseJsonArray(review.get("suggestions_json")));
        review.put("stepFeedback", parseJsonArray(review.get("step_feedback_json")));
        review.put("wrongBook", jdbcTemplate.queryForList("select id, subject_name, question_text, error_reason, weak_tag, suggested_fix from ai_wrong_book where source_review_id=? order by addtime desc", review.get("id")));
        return review;
    }

    // ==================== 功能1：AI公告生成 ====================

    public Map<String, Object> generateNotice(Map<String, Object> body, SessionUser user) {
        requireTeacherOrAdmin(user, "只有教师或管理员可以生成公告");
        String keywords = normalizePlainText(stringValue(body.get("keywords")));
        String noticeType = firstNonBlank(stringValue(body.get("noticeType")), "通知");
        if (StringUtils.isBlank(keywords)) {
            throw new IllegalArgumentException("请输入公告关键信息");
        }
        String prompt = "请根据以下关键信息，生成一份正式的学校" + noticeType + "公告。\n"
                + "关键信息：" + keywords + "\n\n"
                + "要求：\n"
                + "1. 格式规范，包含标题、正文、落款\n"
                + "2. 语言正式、简洁\n"
                + "3. 标题直接写，不要加'标题：'前缀\n"
                + "4. 正文分段说明\n"
                + "5. 落款写'教务处'和当前日期\n"
                + "请直接输出公告内容，不要加任何解释。";

        String generated = callAiWithPrompt(prompt, "你是一位专业的学校教务公告撰写助手，擅长撰写正式、规范的学校通知公告。");
        if (StringUtils.isBlank(generated)) {
            generated = buildDefaultNotice(keywords, noticeType);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("content", generated);
        result.put("keywords", keywords);
        result.put("noticeType", noticeType);
        return result;
    }

    private String buildDefaultNotice(String keywords, String noticeType) {
        return "关于" + keywords.substring(0, Math.min(keywords.length(), 20)) + "的" + noticeType + "\n\n"
                + "各位同学、老师：\n\n"
                + "根据学校工作安排，现就" + keywords + "相关事项通知如下：\n\n"
                + "一、请相关人员按时完成上述事项；\n"
                + "二、如有疑问请及时联系教务处；\n"
                + "三、请认真配合，确保顺利完成。\n\n"
                + "特此通知，请知悉！\n\n"
                + "教务处\n"
                + new java.text.SimpleDateFormat("yyyy年MM月dd日").format(new Date());
    }

    // ==================== 功能2：AI作业反馈扩写 ====================

    public Map<String, Object> expandFeedback(Map<String, Object> body, SessionUser user) {
        requireTeacherOrAdmin(user, "只有教师或管理员可以使用反馈扩写");
        String briefComment = normalizePlainText(stringValue(body.get("briefComment")));
        String homeworkName = stringValue(body.get("homeworkName"));
        String studentName = stringValue(body.get("studentName"));
        String subjectName = stringValue(body.get("subjectName"));
        String submissionContent = normalizePlainText(stringValue(body.get("submissionContent")));
        if (StringUtils.isBlank(briefComment)) {
            throw new IllegalArgumentException("请先输入简短评语");
        }

        StringBuilder promptBuilder = new StringBuilder();
        promptBuilder.append("请将以下教师的简短评语，扩写为详细、专业、有建设性的作业反馈意见。\n\n");
        if (StringUtils.isNotBlank(homeworkName)) {
            promptBuilder.append("作业名称：").append(homeworkName).append("\n");
        }
        if (StringUtils.isNotBlank(subjectName)) {
            promptBuilder.append("科目：").append(subjectName).append("\n");
        }
        if (StringUtils.isNotBlank(studentName)) {
            promptBuilder.append("学生：").append(studentName).append("\n");
        }
        if (StringUtils.isNotBlank(submissionContent)) {
            promptBuilder.append("作业内容摘要：").append(excerpt(submissionContent, 200)).append("\n");
        }
        promptBuilder.append("教师简短评语：").append(briefComment).append("\n\n");
        promptBuilder.append("要求：\n");
        promptBuilder.append("1. 保留教师原意，不要改变评价方向\n");
        promptBuilder.append("2. 扩写为200字左右的详细反馈\n");
        promptBuilder.append("3. 包含：肯定优点、指出问题、改进建议三个层次\n");
        promptBuilder.append("4. 语言亲切专业，适合教师直接使用\n");
        promptBuilder.append("请直接输出扩写后的反馈内容。");

        String expanded = callAiWithPrompt(promptBuilder.toString(), "你是一位经验丰富的教师，擅长给学生写详细、有建设性的作业反馈。");
        if (StringUtils.isBlank(expanded)) {
            expanded = buildDefaultExpandedFeedback(briefComment, studentName, homeworkName);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("expandedFeedback", expanded);
        result.put("originalComment", briefComment);
        return result;
    }

    private String buildDefaultExpandedFeedback(String brief, String studentName, String homeworkName) {
        String name = StringUtils.isNotBlank(studentName) ? studentName + "同学" : "同学";
        return name + "，你好！\n\n"
                + "关于本次" + (StringUtils.isNotBlank(homeworkName) ? "《" + homeworkName + "》" : "") + "作业，老师的评价如下：\n\n"
                + brief + "\n\n"
                + "希望你认真思考老师的反馈意见，在今后的学习中加以改进。如有疑问，欢迎随时提问。\n\n"
                + "继续加油，相信你能做得更好！";
    }

    // ==================== 功能3：AI智能出题 ====================

    public Map<String, Object> generateQuiz(Map<String, Object> body, SessionUser user) {
        requireLogin(user, "请先登录后再生成题目");
        String sourceText = normalizePlainText(stringValue(body.get("sourceText")));
        String subjectName = stringValue(body.get("subjectName"));
        Long materialId = parseLong(body.get("materialId"));
        String materialTitle = stringValue(body.get("materialTitle"));
        int count = Math.min(Math.max(parseInt(body.get("count"), 5), 1), 10);
        String quizType = firstNonBlank(stringValue(body.get("quizType")), "choice");

        if (StringUtils.isBlank(sourceText)) {
            if (materialId != null) {
                Map<String, Object> material = findSingle("select title, content_text, subject_name from ai_material_center where id=? limit 1", materialId);
                if (!material.isEmpty()) {
                    sourceText = normalizePlainText(stringValue(material.get("content_text")));
                    if (StringUtils.isBlank(subjectName)) {
                        subjectName = stringValue(material.get("subject_name"));
                    }
                    if (StringUtils.isBlank(materialTitle)) {
                        materialTitle = stringValue(material.get("title"));
                    }
                }
            }
        }
        if (StringUtils.isBlank(sourceText)) {
            throw new IllegalArgumentException("请提供出题素材文本或选择资料库素材");
        }

        String typeDesc = "choice".equals(quizType) ? "单选题（4个选项，只有1个正确）" : "简答题";
        String prompt = "请根据以下教学内容，出" + count + "道" + typeDesc + "，并给出参考答案和解析。\n\n"
                + "教学内容：\n" + excerpt(sourceText, 900) + "\n\n"
                + "请严格按照以下JSON格式输出，不要有任何其他文字：\n"
                + "[\n"
                + "  {\n"
                + "    \"question\": \"题目内容\",\n"
                + ("choice".equals(quizType)
                    ? "    \"options\": [\"A. 选项一\", \"B. 选项二\", \"C. 选项三\", \"D. 选项四\"],\n"
                    : "    \"options\": [],\n")
                + "    \"answer\": \"" + ("choice".equals(quizType) ? "A" : "参考答案内容") + "\",\n"
                + "    \"explanation\": \"解析说明\"\n"
                + "  }\n"
                + "]";

        String aiResponse = callAiWithPrompt(prompt, "你是一位专业的出题老师，擅长根据教学内容设计考核题目。请严格按JSON格式输出。");
        List<Map<String, Object>> quizList = parseQuizFromAi(aiResponse, count, quizType);
        boolean fallbackUsed = false;
        if (quizList.isEmpty()) {
            quizList = buildFallbackQuizList(sourceText, count, quizType, subjectName);
            fallbackUsed = true;
        }
        if (quizList.size() > count) {
            quizList = new ArrayList<Map<String, Object>>(quizList.subList(0, count));
        }

        Date now = new Date();
        List<Map<String, Object>> saved = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> q : quizList) {
            Long id = newId();
            String optionsJson = JSON.toJSONString(q.get("options"));
            jdbcTemplate.update(
                "insert into ai_quiz(id, subject_name, quiz_type, question_text, options_json, answer, explanation, difficulty, source_material_id, source_material_title, teacher_no, teacher_name, created_by, created_role, addtime) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                id, nullIfBlank(subjectName), quizType, stringValue(q.get("question")),
                optionsJson, stringValue(q.get("answer")), stringValue(q.get("explanation")),
                "normal", materialId, nullIfBlank(materialTitle),
                nullIfBlank(user.teacherNo), nullIfBlank(user.teacherName), user.userId, user.roleKey, now
            );
            Map<String, Object> item = new LinkedHashMap<String, Object>(q);
            item.put("id", id);
            item.put("subjectName", subjectName);
            item.put("quizType", quizType);
            saved.add(item);
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("list", saved);
        result.put("total", saved.size());
        result.put("subjectName", subjectName);
        result.put("generatorMode", fallbackUsed ? "fallback" : "ai");
        return result;
    }

    private List<Map<String, Object>> parseQuizFromAi(String aiResponse, int expectedCount, String quizType) {
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (StringUtils.isBlank(aiResponse)) {
            return result;
        }
        try {
            String jsonStr = extractQuizJsonPayload(aiResponse);
            Object parsed = JSON.parse(jsonStr);
            if (parsed instanceof JSONArray) {
                appendParsedQuizItems(result, (JSONArray) parsed, quizType, expectedCount);
            } else if (parsed instanceof JSONObject) {
                appendParsedQuizItems(result, extractQuizArray((JSONObject) parsed), quizType, expectedCount);
            }
        } catch (Exception e) {
            // ignore and use local fallback later
        }
        return result;
    }

    private String extractQuizJsonPayload(String aiResponse) {
        String text = StringUtils.trimToEmpty(aiResponse)
                .replace("```json", "")
                .replace("```JSON", "")
                .replace("```", "")
                .trim();
        int arrayStart = text.indexOf('[');
        int arrayEnd = text.lastIndexOf(']');
        if (arrayStart >= 0 && arrayEnd > arrayStart) {
            return text.substring(arrayStart, arrayEnd + 1);
        }
        int objStart = text.indexOf('{');
        int objEnd = text.lastIndexOf('}');
        if (objStart >= 0 && objEnd > objStart) {
            return text.substring(objStart, objEnd + 1);
        }
        return text;
    }

    private JSONArray extractQuizArray(JSONObject object) {
        if (object == null) {
            return null;
        }
        JSONArray arr = object.getJSONArray("list");
        if (arr == null) {
            arr = object.getJSONArray("questions");
        }
        if (arr == null) {
            arr = object.getJSONArray("items");
        }
        if (arr == null) {
            arr = object.getJSONArray("data");
        }
        if (arr == null) {
            arr = object.getJSONArray("题目");
        }
        return arr;
    }

    private void appendParsedQuizItems(List<Map<String, Object>> result, JSONArray arr, String quizType, int expectedCount) {
        if (arr == null) {
            return;
        }
        for (int i = 0; i < arr.size(); i++) {
            JSONObject item = arr.getJSONObject(i);
            if (item == null) {
                continue;
            }
            Map<String, Object> normalized = normalizeParsedQuizItem(item, i, quizType);
            if (normalized == null || StringUtils.isBlank(stringValue(normalized.get("question")))) {
                continue;
            }
            result.add(normalized);
            if (result.size() >= expectedCount) {
                break;
            }
        }
    }

    private Map<String, Object> normalizeParsedQuizItem(JSONObject item, int index, String quizType) {
        String question = normalizePlainText(firstNonBlank(
                item.getString("question"),
                item.getString("题目"),
                item.getString("questionText"),
                item.getString("title"),
                item.getString("题干")
        ));
        if (StringUtils.isBlank(question)) {
            return null;
        }

        Map<String, Object> q = new LinkedHashMap<String, Object>();
        q.put("question", question);
        if ("choice".equals(quizType)) {
            List<String> options = ensureChoiceOptions(normalizeParsedOptions(item), question);
            String answer = normalizeChoiceAnswer(firstNonBlank(
                    item.getString("answer"),
                    item.getString("答案"),
                    item.getString("correctAnswer"),
                    item.getString("参考答案")
            ), options);
            if (StringUtils.isBlank(answer)) {
                answer = "A";
            }
            String explanation = normalizePlainText(firstNonBlank(
                    item.getString("explanation"),
                    item.getString("解析"),
                    item.getString("analysis"),
                    item.getString("explain")
            ));
            if (StringUtils.isBlank(explanation)) {
                explanation = "可根据题干中的关键信息直接判断正确答案。";
            }
            q.put("options", options);
            q.put("answer", answer);
            q.put("explanation", explanation);
        } else {
            String answer = normalizePlainText(firstNonBlank(
                    item.getString("answer"),
                    item.getString("答案"),
                    item.getString("correctAnswer"),
                    item.getString("参考答案")
            ));
            if (StringUtils.isBlank(answer)) {
                answer = excerpt(question, 80);
            }
            String explanation = normalizePlainText(firstNonBlank(
                    item.getString("explanation"),
                    item.getString("解析"),
                    item.getString("analysis"),
                    item.getString("explain")
            ));
            if (StringUtils.isBlank(explanation)) {
                explanation = "回答时应围绕题干中的关键知识点展开，做到观点完整、表述清楚。";
            }
            q.put("options", new ArrayList<String>());
            q.put("answer", answer);
            q.put("explanation", explanation);
        }
        return q;
    }

    private List<String> normalizeParsedOptions(JSONObject item) {
        LinkedHashSet<String> options = new LinkedHashSet<String>();
        Object rawOptions = item.get("options");
        if (rawOptions instanceof JSONArray) {
            JSONArray arr = (JSONArray) rawOptions;
            for (Object option : arr) {
                addOptionValue(options, option);
            }
        } else if (rawOptions instanceof Collection) {
            for (Object option : (Collection<?>) rawOptions) {
                addOptionValue(options, option);
            }
        } else {
            addOptionsFromText(options, firstNonBlank(
                    item.getString("options"),
                    item.getString("选项"),
                    item.getString("optionsText")
            ));
        }
        return new ArrayList<String>(options);
    }

    private void addOptionValue(Collection<String> options, Object raw) {
        if (raw == null) {
            return;
        }
        String text = normalizePlainText(String.valueOf(raw));
        if (StringUtils.isBlank(text)) {
            return;
        }
        options.add(text);
    }

    private void addOptionsFromText(Collection<String> options, String text) {
        String normalized = StringUtils.trimToEmpty(text)
                .replace("；", "\n")
                .replace(";", "\n")
                .replace("|", "\n");
        String[] parts = normalized.split("\\r?\\n");
        if (parts.length <= 1) {
            parts = normalized.split("(?=[A-DＡ-Ｄ][\\.．、:：])");
        }
        for (String part : parts) {
            String value = normalizePlainText(part);
            if (StringUtils.isNotBlank(value)) {
                options.add(value);
            }
        }
    }

    private List<String> ensureChoiceOptions(List<String> options, String question) {
        LinkedHashSet<String> normalized = new LinkedHashSet<String>();
        if (options != null) {
            for (String option : options) {
                String value = stripOptionPrefix(option);
                if (StringUtils.isNotBlank(value)) {
                    normalized.add(value);
                }
            }
        }
        List<String> fallback = Arrays.asList(
                excerpt(question, 28),
                "只要机械记忆即可，不需要理解关键概念",
                "该内容与当前课程主题无关，可以忽略",
                "学习时不需要关注题干中的重点信息"
        );
        for (String option : fallback) {
            if (normalized.size() >= 4) {
                break;
            }
            String value = stripOptionPrefix(option);
            if (StringUtils.isNotBlank(value)) {
                normalized.add(value);
            }
        }
        List<String> result = new ArrayList<String>();
        int index = 0;
        for (String option : normalized) {
            result.add(withOptionPrefix(index, option));
            index++;
            if (index >= 4) {
                break;
            }
        }
        while (result.size() < 4) {
            result.add(withOptionPrefix(result.size(), "补充选项" + (result.size() + 1)));
        }
        return result;
    }

    private String withOptionPrefix(int index, String text) {
        char letter = (char) ('A' + Math.max(index, 0));
        return letter + ". " + stripOptionPrefix(text);
    }

    private String stripOptionPrefix(String text) {
        return normalizePlainText(text).replaceFirst("^[A-DＡ-Ｄ][\\.．、:：]\\s*", "");
    }

    private String normalizeChoiceAnswer(String answer, List<String> options) {
        String value = StringUtils.trimToEmpty(answer);
        if (StringUtils.isBlank(value)) {
            return "";
        }
        String upper = value.toUpperCase(Locale.ROOT);
        Matcher matcher = Pattern.compile("[A-D]").matcher(upper);
        if (matcher.find()) {
            return matcher.group();
        }
        for (int i = 0; i < options.size(); i++) {
            String option = stripOptionPrefix(options.get(i));
            if (StringUtils.isBlank(option)) {
                continue;
            }
            if (value.contains(option) || option.contains(value)) {
                return String.valueOf((char) ('A' + i));
            }
        }
        return "";
    }

    private List<Map<String, Object>> buildFallbackQuizList(String sourceText, int count, String quizType, String subjectName) {
        List<String> segments = extractQuizSegments(sourceText);
        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        if (segments.isEmpty()) {
            segments.add(excerpt(sourceText, 80));
        }
        for (int i = 0; i < count; i++) {
            String segment = segments.get(i % segments.size());
            result.add("choice".equals(quizType)
                    ? buildFallbackChoiceQuestion(segment, i, subjectName)
                    : buildFallbackEssayQuestion(segment, i, subjectName));
        }
        return result;
    }

    private List<String> extractQuizSegments(String sourceText) {
        LinkedHashSet<String> segments = new LinkedHashSet<String>();
        String[] lines = StringUtils.defaultString(sourceText).split("\\r?\\n");
        for (String line : lines) {
            String value = normalizePlainText(line).replaceFirst("^[0-9一二三四五六七八九十]+[、.．)]\\s*", "");
            if (value.length() >= 8) {
                segments.add(value);
            }
        }
        if (segments.isEmpty()) {
            String[] parts = StringUtils.defaultString(sourceText).split("[。！？；;]");
            for (String part : parts) {
                String value = normalizePlainText(part).replaceFirst("^[0-9一二三四五六七八九十]+[、.．)]\\s*", "");
                if (value.length() >= 8) {
                    segments.add(value);
                }
            }
        }
        if (segments.isEmpty()) {
            String value = normalizePlainText(sourceText);
            if (StringUtils.isNotBlank(value)) {
                segments.add(value);
            }
        }
        return new ArrayList<String>(segments);
    }

    private Map<String, Object> buildFallbackChoiceQuestion(String segment, int index, String subjectName) {
        String focus = excerpt(segment, 32);
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("question", "根据" + firstNonBlank(subjectName, "本次教学内容") + "，下列哪项最符合“" + focus + "”？");
        item.put("options", Arrays.asList(
                "A. " + focus,
                "B. 只要死记硬背即可，不需要理解概念之间的联系",
                "C. 这部分内容与当前课程无关，可以忽略",
                "D. 学习时不需要关注题干中的关键词"
        ));
        item.put("answer", "A");
        item.put("explanation", "教学素材中明确围绕“" + focus + "”展开，因此 A 项与原文信息一致。");
        return item;
    }

    private Map<String, Object> buildFallbackEssayQuestion(String segment, int index, String subjectName) {
        String focus = excerpt(segment, 32);
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("question", "请结合" + firstNonBlank(subjectName, "本次教学内容") + "，简要说明“" + focus + "”的核心要点。");
        item.put("options", new ArrayList<String>());
        item.put("answer", excerpt(segment, 120));
        item.put("explanation", "作答时可以围绕概念、要求、作用或方法展开，结合课堂所学组织答案。");
        return item;
    }

    public Map<String, Object> listQuiz(Map<String, Object> params, SessionUser user) {
        int page = Math.max(parseInt(params.get("page"), 1), 1);
        int limit = Math.min(Math.max(parseInt(params.get("limit"), 10), 1), 50);
        int offset = (page - 1) * limit;
        String subjectName = stringValue(params.get("subjectName"));
        String quizType = stringValue(params.get("quizType"));
        Long quizId = parseLong(params.get("id"));

        StringBuilder where = new StringBuilder(" where 1=1");
        List<Object> args = new ArrayList<Object>();
        if (StringUtils.isNotBlank(subjectName)) {
            where.append(" and subject_name like ?");
            args.add(likeValue(subjectName));
        }
        if (StringUtils.isNotBlank(quizType)) {
            where.append(" and quiz_type=?");
            args.add(quizType);
        }
        if (quizId != null) {
            where.append(" and id=?");
            args.add(quizId);
        }
        if (user != null && user.isTeacher()) {
            where.append(" and teacher_no=?");
            args.add(user.teacherNo);
        }

        long total = jdbcTemplate.queryForObject("select count(*) from ai_quiz" + where, args.toArray(), Long.class);
        List<Object> pageArgs = new ArrayList<Object>(args);
        pageArgs.add(offset);
        pageArgs.add(limit);
        List<Map<String, Object>> list = jdbcTemplate.queryForList(
            "select id, subject_name, quiz_type, question_text, options_json, answer, explanation, difficulty, source_material_title, teacher_name, addtime from ai_quiz" + where + " order by addtime desc limit ?,?",
            pageArgs.toArray()
        );
        for (Map<String, Object> item : list) {
            item.put("options", parseJsonArray(item.get("options_json")));
        }

        boolean isStudent = user != null && user.isStudent();
        if (isStudent) {
            for (Map<String, Object> item : list) {
                item.remove("answer");
                item.remove("explanation");
            }
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("list", list);
        data.put("total", total);
        data.put("page", page);
        data.put("limit", limit);
        return data;
    }

    public void deleteQuiz(Long[] ids, SessionUser user) {
        requireTeacherOrAdmin(user, "只有教师或管理员可以删除题目");
        if (ids == null || ids.length == 0) return;
        for (Long id : ids) {
            if (id == null) continue;
            if (user.isAdmin()) {
                jdbcTemplate.update("delete from ai_quiz where id=?", id);
            } else {
                jdbcTemplate.update("delete from ai_quiz where id=? and teacher_no=?", id, user.teacherNo);
            }
        }
    }

    public Map<String, Object> submitQuizAnswer(Map<String, Object> body, SessionUser user) {
        if (user == null || user.userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        Long quizId = parseLong(body.get("quizId"));
        String userAnswer = normalizePlainText(stringValue(body.get("answer")));
        if (quizId == null) {
            throw new IllegalArgumentException("题目ID不能为空");
        }

        Map<String, Object> quiz = findSingle("select * from ai_quiz where id=? limit 1", quizId);
        if (quiz.isEmpty()) {
            throw new IllegalArgumentException("题目不存在");
        }

        String correctAnswer = stringValue(quiz.get("answer")).trim();
        String quizType = stringValue(quiz.get("quiz_type"));
        boolean isCorrect = false;
        if ("choice".equals(quizType)) {
            isCorrect = correctAnswer.equalsIgnoreCase(userAnswer.trim())
                    || correctAnswer.toLowerCase().startsWith(userAnswer.trim().toLowerCase());
        } else {
            isCorrect = StringUtils.isNotBlank(userAnswer) && userAnswer.length() >= 10;
        }

        double score = isCorrect ? 10.0 : 0.0;
        Date now = new Date();
        jdbcTemplate.update(
            "insert into ai_quiz_answer(id, quiz_id, student_id, student_no, student_name, answer, is_correct, score, addtime) values(?,?,?,?,?,?,?,?,?)",
            newId(), quizId, user.userId, nullIfBlank(user.studentNo), nullIfBlank(user.studentName),
            userAnswer, isCorrect ? 1 : 0, score, now
        );

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("isCorrect", isCorrect);
        result.put("score", score);
        result.put("correctAnswer", correctAnswer);
        result.put("explanation", quiz.get("explanation"));
        result.put("userAnswer", userAnswer);
        return result;
    }

    // ==================== 功能4：学情图表数据增强 ====================

    public Map<String, Object> getStudentChartData(Map<String, Object> params, SessionUser user) {
        if (user == null || user.userId == null) {
            throw new IllegalArgumentException("请先登录");
        }
        Long targetId = user.userId;
        if (!user.isStudent() && params.get("studentId") != null) {
            targetId = parseLong(params.get("studentId"));
        }

        List<Map<String, Object>> subjectStats = jdbcTemplate.queryForList(
            "select ifnull(subject_name,'未分类') as subject_name, round(avg(score),1) as avg_score, count(*) as total, max(score) as max_score from ai_homework_review where student_id=? group by ifnull(subject_name,'未分类') order by avg_score desc limit 8",
            targetId
        );

        List<Map<String, Object>> weeklyTrend = jdbcTemplate.queryForList(
            "select date(updatetime) as stat_date, date_format(updatetime,'%m/%d') as week_label, round(avg(score),1) as avg_score, count(*) as total "
                + "from ai_homework_review "
                + "where student_id=? and updatetime >= date_sub(now(), interval 30 day) "
                + "group by date(updatetime), date_format(updatetime,'%m/%d') "
                + "order by stat_date asc limit 15",
            targetId
        );
        for (Map<String, Object> item : weeklyTrend) {
            item.remove("stat_date");
        }

        List<Map<String, Object>> weakTags = jdbcTemplate.queryForList(
            "select weak_tag, count(*) as total from ai_wrong_book where student_id=? and weak_tag is not null and weak_tag<>'' group by weak_tag order by total desc limit 8",
            targetId
        );

        List<Map<String, Object>> quizStats = jdbcTemplate.queryForList(
            "select q.subject_name, count(*) as total, sum(a.is_correct) as correct from ai_quiz_answer a join ai_quiz q on a.quiz_id=q.id where a.student_id=? group by q.subject_name order by total desc limit 6",
            targetId
        );

        List<String> radarLabels = new ArrayList<String>();
        List<Integer> radarValues = new ArrayList<Integer>();
        int maxRadarScore = 100;
        for (Map<String, Object> stat : subjectStats) {
            radarLabels.add(stringValue(stat.get("subject_name")));
            double avg = toDouble(stat.get("avg_score"));
            radarValues.add((int) Math.round(avg));
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("subjectStats", subjectStats);
        data.put("weeklyTrend", weeklyTrend);
        data.put("weakTags", weakTags);
        data.put("quizStats", quizStats);
        data.put("radarLabels", radarLabels);
        data.put("radarValues", radarValues);
        data.put("radarMax", maxRadarScore);
        return data;
    }

    // ==================== 功能5：课程视频观看记录 ====================

    public Map<String, Object> logVideoWatch(Map<String, Object> body, SessionUser user) {
        if (user == null || user.userId == null) {
            return new LinkedHashMap<String, Object>();
        }
        Long videoId = parseLong(body.get("videoId"));
        if (videoId == null) return new LinkedHashMap<String, Object>();

        int watchedSeconds = parseInt(body.get("watchedSeconds"), 0);
        int totalSeconds = parseInt(body.get("totalSeconds"), 0);
        int lastPosition = parseInt(body.get("lastPosition"), 0);
        boolean isFinished = totalSeconds > 0 && lastPosition >= totalSeconds * 9 / 10;

        Map<String, Object> existing = findSingle(
            "select id, watched_seconds from video_watch_log where video_id=? and student_id=? limit 1",
            videoId, user.userId
        );

        Date now = new Date();
        if (existing.isEmpty()) {
            jdbcTemplate.update(
                "insert into video_watch_log(id, video_id, student_id, student_no, student_name, watched_seconds, total_seconds, last_position, is_finished, addtime, updatetime) values(?,?,?,?,?,?,?,?,?,?,?)",
                newId(), videoId, user.userId, nullIfBlank(user.studentNo), nullIfBlank(user.studentName),
                watchedSeconds, totalSeconds, lastPosition, isFinished ? 1 : 0, now, now
            );
        } else {
            int prevWatched = parseInt(existing.get("watched_seconds"), 0);
            int newWatched = Math.max(prevWatched, watchedSeconds);
            jdbcTemplate.update(
                "update video_watch_log set watched_seconds=?, total_seconds=?, last_position=?, is_finished=?, updatetime=? where id=?",
                newWatched, totalSeconds, lastPosition, isFinished ? 1 : 0, now, existing.get("id")
            );
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("logged", true);
        result.put("isFinished", isFinished);
        return result;
    }

    public Map<String, Object> getVideoWatchStats(Map<String, Object> params, SessionUser user) {
        requireTeacherOrAdmin(user, "只有教师或管理员可以查看观看统计");
        Long videoId = parseLong(params.get("videoId"));

        List<Map<String, Object>> stats;
        if (videoId != null) {
            stats = jdbcTemplate.queryForList(
                "select v.student_name, v.student_no, v.watched_seconds, v.total_seconds, v.last_position, v.is_finished, v.updatetime from video_watch_log v where v.video_id=? order by v.updatetime desc",
                videoId
            );
        } else {
            stats = jdbcTemplate.queryForList(
                "select k.kechengmingcheng as video_title, count(v.id) as watch_count, sum(v.is_finished) as finish_count, round(avg(v.watched_seconds/greatest(v.total_seconds,1)*100),1) as avg_progress from kechengshipin k left join video_watch_log v on k.id=v.video_id group by k.id, k.kechengmingcheng order by watch_count desc limit 20"
            );
        }

        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("list", stats);
        data.put("videoId", videoId);
        return data;
    }

    // ==================== 通用AI调用（不依赖知识库） ====================

    private String callAiWithPrompt(String userPrompt, String systemPrompt) {
        Map<String, String> configMap = getConfigValueMap();
        String baseUrl = pickConfigValue(configMap, AI_BASE_KEYS);
        String apiKey = pickConfigValue(configMap, AI_KEY_KEYS);
        String model = pickConfigValue(configMap, AI_MODEL_KEYS);
        String apiPath = firstNonBlank(pickConfigValue(configMap, AI_PATH_KEYS), "chat/completions");
        double temperature = pickConfigDouble(configMap, AI_TEMPERATURE_KEYS, 0.6D);
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiKey) || StringUtils.isBlank(model)) {
            return "";
        }
        String endpoint = resolveAiEndpoint(baseUrl, apiPath);
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        JSONArray messages = new JSONArray();
        messages.add(buildMessage("system", systemPrompt));
        messages.add(buildMessage("user", userPrompt));
        requestBody.put("messages", messages);
        try {
            String response = postJson(endpoint, requestBody.toJSONString(), apiKey);
            return parseAiResponse(response);
        } catch (Exception e) {
            return "";
        }
    }

    public Map<String, Object> getStorageConfig() {
        Map<String, String> configMap = getConfigValueMap();
        Map<String, Object> data = new LinkedHashMap<String, Object>();
        data.put("assetPublicBase", pickConfigValue(configMap, Arrays.asList("asset_public_base")));
        data.put("assetCdnBase", pickConfigValue(configMap, Arrays.asList("asset_cdn_base")));
        data.put("assetStorageProvider", pickConfigValue(configMap, Arrays.asList("asset_storage_provider")));
        data.put("assetBucketName", pickConfigValue(configMap, Arrays.asList("asset_bucket_name")));
        data.put("assetDeliveryNote", pickConfigValue(configMap, Arrays.asList("asset_delivery_note")));
        return data;
    }

    public Map<String, Object> saveStorageConfig(Map<String, Object> body, SessionUser user) {
        requireAdmin(user, "只有管理员可以维护资源配置");
        saveConfigValue("asset_public_base", stringValue(body.get("assetPublicBase")));
        saveConfigValue("asset_cdn_base", stringValue(body.get("assetCdnBase")));
        saveConfigValue("asset_storage_provider", stringValue(body.get("assetStorageProvider")));
        saveConfigValue("asset_bucket_name", stringValue(body.get("assetBucketName")));
        saveConfigValue("asset_delivery_note", stringValue(body.get("assetDeliveryNote")));
        return getStorageConfig();
    }

    public Map<String, Object> testAiConfig(Map<String, Object> body, SessionUser user) throws Exception {
        requireAdmin(user, "只有管理员可以测试 AI 配置");

        Map<String, String> configMap = getConfigValueMap();
        String baseUrl = firstNonBlank(body.get("ai_user_api_url"), body.get("apiUrl"), pickConfigValue(configMap, AI_BASE_KEYS));
        String apiKey = firstNonBlank(body.get("ai_user_api_key"), body.get("apiKey"), pickConfigValue(configMap, AI_KEY_KEYS));
        String model = firstNonBlank(body.get("ai_user_model_name"), body.get("modelName"), pickConfigValue(configMap, AI_MODEL_KEYS));
        String apiPath = firstNonBlank(body.get("ai_user_api_path"), body.get("apiPath"), pickConfigValue(configMap, AI_PATH_KEYS), "chat/completions");
        String systemPrompt = firstNonBlank(
                body.get("ai_user_system_prompt"),
                body.get("systemPrompt"),
                pickConfigValue(configMap, AI_PROMPT_KEYS),
                "你是一个 AI 配置连通性测试助手，请直接回复“AI连通性测试成功”，并附上一句不超过20字的确认。"
        );

        double temperature = pickConfigDouble(configMap, AI_TEMPERATURE_KEYS, 0.2D);
        String rawTemperature = firstNonBlank(body.get("ai_user_temperature"), body.get("temperature"));
        if (StringUtils.isNotBlank(rawTemperature)) {
            try {
                temperature = Double.parseDouble(rawTemperature.trim());
            } catch (NumberFormatException ignored) {
                throw new IllegalArgumentException("温度参数格式不正确，请输入 0 到 2 之间的数字");
            }
        }

        if (StringUtils.isBlank(baseUrl)) {
            throw new IllegalArgumentException("请先填写 AI 接口地址");
        }
        if (StringUtils.isBlank(apiKey)) {
            throw new IllegalArgumentException("请先填写 AI 接口密钥");
        }
        if (StringUtils.isBlank(model)) {
            throw new IllegalArgumentException("请先填写 AI 模型 / 接入点 ID");
        }
        if (apiKey.startsWith("ep-")) {
            throw new IllegalArgumentException("接口密钥看起来像接入点 ID。请把 ep- 开头的值填到“模型 / 接入点 ID”，接口密钥处填写真实 API Key");
        }

        String endpoint = resolveAiEndpoint(baseUrl, apiPath);
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);

        JSONArray messages = new JSONArray();
        messages.add(buildMessage("system", systemPrompt));
        messages.add(buildMessage("user", "请进行一次后台连通性测试，只回复测试结果，不要返回 JSON。"));
        requestBody.put("messages", messages);

        long startedAt = System.currentTimeMillis();
        String response = postJson(endpoint, requestBody.toJSONString(), apiKey);
        long durationMs = System.currentTimeMillis() - startedAt;
        String content = parseAiResponse(response);
        if (StringUtils.isBlank(content)) {
            throw new IllegalArgumentException("接口已请求成功，但没有解析到有效文本回复。请检查模型是否兼容 chat/completions。响应片段：" + excerpt(response, 120));
        }

        Map<String, Object> result = new LinkedHashMap<String, Object>();
        result.put("success", true);
        result.put("endpoint", endpoint);
        result.put("model", model);
        result.put("temperature", temperature);
        result.put("durationMs", durationMs);
        result.put("content", content);
        result.put("preview", excerpt(content, 120));
        return result;
    }

    private void saveQuestionRecord(String question, String answer, List<Map<String, Object>> citations, List<Map<String, Object>> attachments,
                                    String intent, String answerMode, SessionUser user) {
        LinkedHashSet<String> subjectSet = new LinkedHashSet<String>();
        LinkedHashSet<String> teacherNoSet = new LinkedHashSet<String>();
        for (Map<String, Object> citation : citations) {
            String subjectName = stringValue(citation.get("subjectName"));
            String teacherNo = stringValue(citation.get("teacherNo"));
            if (StringUtils.isNotBlank(subjectName)) {
                subjectSet.add(subjectName);
            }
            if (StringUtils.isNotBlank(teacherNo)) {
                teacherNoSet.add(teacherNo);
            }
        }
        jdbcTemplate.update(
                "insert into ai_question_record(id, user_id, user_role, username, display_name, question_text, answer_text, citations_json, attachments_json, knowledge_json, intent, answer_mode, related_subjects, related_teacher_nos, addtime) values(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                newId(),
                user == null ? null : user.userId,
                user == null ? null : user.roleKey,
                user == null ? null : user.username,
                user == null ? null : user.displayName,
                question,
                answer,
                JSON.toJSONString(citations),
                JSON.toJSONString(attachments),
                JSON.toJSONString(buildKnowledgeSummaryMap(citations)),
                intent,
                answerMode,
                joinCollection(subjectSet, ","),
                joinCollection(teacherNoSet, ","),
                new Date()
        );
    }

    private String buildKnowledgeSummary(List<Map<String, Object>> citations) {
        Map<String, Object> summary = buildKnowledgeSummaryMap(citations);
        StringBuilder builder = new StringBuilder();
        builder.append("已引用 ").append(summary.get("citationCount")).append(" 条系统资料");
        if (summary.get("subjects") instanceof Collection && !((Collection<?>) summary.get("subjects")).isEmpty()) {
            builder.append("，覆盖科目：").append(joinCollection((Collection<?>) summary.get("subjects"), "、"));
        }
        return builder.toString();
    }

    private Map<String, Object> buildKnowledgeSummaryMap(List<Map<String, Object>> citations) {
        LinkedHashSet<String> subjects = new LinkedHashSet<String>();
        LinkedHashSet<String> sourceTypes = new LinkedHashSet<String>();
        for (Map<String, Object> citation : citations) {
            String subjectName = stringValue(citation.get("subjectName"));
            String sourceTypeLabel = stringValue(citation.get("sourceTypeLabel"));
            if (StringUtils.isNotBlank(subjectName)) {
                subjects.add(subjectName);
            }
            if (StringUtils.isNotBlank(sourceTypeLabel)) {
                sourceTypes.add(sourceTypeLabel);
            }
        }
        Map<String, Object> summary = new LinkedHashMap<String, Object>();
        summary.put("citationCount", citations.size());
        summary.put("subjects", new ArrayList<String>(subjects));
        summary.put("sourceTypes", new ArrayList<String>(sourceTypes));
        return summary;
    }

    private List<Map<String, Object>> searchKnowledge(String question, String attachmentContext, String intent) {
        String retrievalText = firstNonBlank(question, "") + " " + firstNonBlank(attachmentContext, "");
        List<String> keywords = extractKeywords(retrievalText);
        List<Map<String, Object>> candidates = new ArrayList<Map<String, Object>>();

        candidates.addAll(loadMaterialCandidates());
        candidates.addAll(loadCourseCandidates());
        candidates.addAll(loadHomeworkCandidates());
        candidates.addAll(loadNoticeCandidates());
        candidates.addAll(loadQaCandidates());

        List<Map<String, Object>> ranked = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> candidate : candidates) {
            double score = scoreCandidate(candidate, keywords, intent);
            if (score <= 0) {
                continue;
            }
            candidate.put("score", score);
            candidate.put("snippet", excerpt(firstNonBlank(candidate.get("snippet"), candidate.get("content")), 100));
            ranked.add(candidate);
        }

        Collections.sort(ranked, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                double diff = toDouble(right.get("score")) - toDouble(left.get("score"));
                return diff > 0 ? 1 : (diff < 0 ? -1 : 0);
            }
        });

        List<Map<String, Object>> result = new ArrayList<Map<String, Object>>();
        LinkedHashSet<String> uniqueKeys = new LinkedHashSet<String>();
        int max = Math.min(ranked.size(), 4);
        for (int i = 0; i < ranked.size() && result.size() < max; i++) {
            Map<String, Object> candidate = new LinkedHashMap<String, Object>(ranked.get(i));
            String uniqueKey = stringValue(candidate.get("sourceType")) + "-" + stringValue(candidate.get("sourceId")) + "-" + stringValue(candidate.get("title"));
            if (uniqueKeys.contains(uniqueKey)) {
                continue;
            }
            uniqueKeys.add(uniqueKey);
            candidate.put("label", "资料" + (result.size() + 1));
            result.add(candidate);
        }
        return result;
    }

    private List<Map<String, Object>> loadMaterialCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, title, subject_name, tags, summary, content_text, file_url, file_name, teacher_no, teacher_name, addtime from ai_material_center order by updatetime desc limit 120"
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(buildCandidate(
                    "material",
                    "资料库",
                    row.get("id"),
                    row.get("title"),
                    firstNonBlank(row.get("summary"), row.get("content_text")),
                    row.get("content_text"),
                    row.get("subject_name"),
                    row.get("teacher_no"),
                    row.get("teacher_name"),
                    row.get("addtime"),
                    "/pages/material-center/list",
                    row.get("tags")
            ));
        }
        return list;
    }

    private List<Map<String, Object>> loadCourseCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, kechengmingcheng, kechengleixing, kechengxiangqing, gonghao, jiaoshixingming, faburiqi from kechengshipin order by faburiqi desc limit 120"
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(buildCandidate(
                    "course",
                    "课程视频",
                    row.get("id"),
                    row.get("kechengmingcheng"),
                    row.get("kechengxiangqing"),
                    row.get("kechengxiangqing"),
                    row.get("kechengleixing"),
                    row.get("gonghao"),
                    row.get("jiaoshixingming"),
                    row.get("faburiqi"),
                    "/pages/kechengshipin/detail?id=" + row.get("id"),
                    null
            ));
        }
        return list;
    }

    private List<Map<String, Object>> loadHomeworkCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, zuoyemingcheng, kemu, zuoyaoqiu, gonghao, jiaoshixingming, kaishishijian, jieshushijian from zuoyexinxi order by addtime desc limit 120"
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            String timeText = buildHomeworkTimeText(row.get("kaishishijian"), row.get("jieshushijian"));
            String homeworkSummary = firstNonBlank(row.get("zuoyaoqiu"), timeText);
            list.add(buildCandidate(
                    "homework",
                    "作业信息",
                    row.get("id"),
                    row.get("zuoyemingcheng"),
                    homeworkSummary,
                    homeworkSummary,
                    row.get("kemu"),
                    row.get("gonghao"),
                    row.get("jiaoshixingming"),
                    row.get("jieshushijian"),
                    "/pages/zuoyexinxi/detail?id=" + row.get("id"),
                    null
            ));
        }
        return list;
    }

    private List<Map<String, Object>> loadNoticeCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select id, title, content, publish_time, addtime from gonggao where status=1 order by publish_time desc, addtime desc limit 80"
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            list.add(buildCandidate(
                    "notice",
                    "通知公告",
                    row.get("id"),
                    row.get("title"),
                    row.get("content"),
                    row.get("content"),
                    null,
                    null,
                    null,
                    firstNonBlank(row.get("publish_time"), row.get("addtime")),
                    "/pages/gonggao/detail?id=" + row.get("id"),
                    null
            ));
        }
        return list;
    }

    private List<Map<String, Object>> loadQaCandidates() {
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(
                "select x.id, x.biaoti, x.tiwenneirong, x.gonghao, x.jiaoshixingming, x.tiwenshijian, h.huidaneirong "
                        + "from tiwenxinxi x left join tiwenhuida h on x.bianhao=h.bianhao order by x.tiwenshijian desc limit 80"
        );
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> row : rows) {
            String answerText = firstNonBlank(row.get("huidaneirong"), row.get("tiwenneirong"));
            list.add(buildCandidate(
                    "qa",
                    "历史问答",
                    row.get("id"),
                    row.get("biaoti"),
                    answerText,
                    firstNonBlank(row.get("tiwenneirong"), "") + " " + firstNonBlank(row.get("huidaneirong"), ""),
                    null,
                    row.get("gonghao"),
                    row.get("jiaoshixingming"),
                    row.get("tiwenshijian"),
                    "/pages/tiwenxinxi/detail?id=" + row.get("id"),
                    null
            ));
        }
        return list;
    }

    private Map<String, Object> buildCandidate(String sourceType, String sourceTypeLabel, Object sourceId, Object title,
                                               Object snippet, Object content, Object subjectName, Object teacherNo,
                                               Object teacherName, Object timeValue, String pageUrl, Object tags) {
        Map<String, Object> item = new LinkedHashMap<String, Object>();
        item.put("sourceType", sourceType);
        item.put("sourceTypeLabel", sourceTypeLabel);
        item.put("sourceId", sourceId);
        item.put("title", stringValue(title));
        item.put("snippet", normalizePlainText(stringValue(snippet)));
        item.put("content", normalizePlainText(stringValue(content)));
        item.put("subjectName", stringValue(subjectName));
        item.put("teacherNo", stringValue(teacherNo));
        item.put("teacherName", stringValue(teacherName));
        item.put("timeLabel", formatDateTime(timeValue));
        item.put("pageUrl", pageUrl);
        item.put("tags", stringValue(tags));
        return item;
    }

    private double scoreCandidate(Map<String, Object> candidate, List<String> keywords, String intent) {
        String title = lower(candidate.get("title"));
        String content = lower(candidate.get("content"));
        String subject = lower(candidate.get("subjectName"));
        String tags = lower(candidate.get("tags"));
        String sourceType = stringValue(candidate.get("sourceType"));

        double score = 0;
        for (String keyword : keywords) {
            if (title.contains(keyword)) {
                score += keyword.length() >= 4 ? 3.8 : 2.3;
            }
            if (subject.contains(keyword)) {
                score += keyword.length() >= 4 ? 2.8 : 1.8;
            }
            if (content.contains(keyword)) {
                score += keyword.length() >= 4 ? 2.5 : 1.2;
            }
            if (tags.contains(keyword)) {
                score += 1.5;
            }
        }

        if ("time".equals(intent) && ("homework".equals(sourceType) || "notice".equals(sourceType))) {
            score += 2.8;
        }
        if ("course".equals(intent) && ("course".equals(sourceType) || "material".equals(sourceType))) {
            score += 2.6;
        }
        if ("notice".equals(intent) && "notice".equals(sourceType)) {
            score += 3.0;
        }
        if ("review".equals(intent) && ("qa".equals(sourceType) || "homework".equals(sourceType))) {
            score += 2.0;
        }

        if (score > 0 && StringUtils.isNotBlank(stringValue(candidate.get("timeLabel")))) {
            score += 0.3;
        }
        return score;
    }

    private List<String> buildSuggestedActions(String intent, List<Map<String, Object>> citations, SessionUser user) {
        List<String> actions = new ArrayList<String>();
        if ("time".equals(intent)) {
            actions.add("打开对应作业详情页，再核对一次截止时间和提交要求。");
        }
        if ("course".equals(intent)) {
            actions.add("进入课程视频详情页继续看完整讲解，再回到 AI 答疑追问不懂的步骤。");
        }
        if ("notice".equals(intent)) {
            actions.add("结合公告详情确认是否有补充说明、发布时间或老师要求。");
        }
        if (user != null && user.isStudent()) {
            actions.add("如果还是不确定，可以把题目截图、作业要求或你自己的思路一起发给我继续追问。");
        }
        if (citations.isEmpty()) {
            actions.add("当前系统资料不足，建议补充课件内容、作业截图或老师说明后再追问。");
        }
        if (actions.isEmpty()) {
            actions.add("如果你想继续深入，我可以基于这批资料继续帮你生成练习、总结重点或改写答案。");
        }
        return actions;
    }

    private String buildLocalAnswer(String question, List<Map<String, Object>> citations, String attachmentContext, List<String> actions) {
        StringBuilder builder = new StringBuilder();
        if (StringUtils.isNotBlank(attachmentContext)) {
            builder.append("我先结合了你上传附件里提取出来的信息，再去系统资料里找对应内容。").append("\n\n");
        }
        if (citations.isEmpty()) {
            builder.append("系统里的课件、作业、公告和历史问答里，暂时没有找到能直接支撑这个问题的证据。")
                    .append("你可以继续补充课程名、作业名、老师姓名，或者直接上传截图/PDF，我会继续帮你缩小范围。");
        } else {
            builder.append("我先基于系统内资料给你一个引用式回答：").append("\n");
            for (int i = 0; i < citations.size(); i++) {
                Map<String, Object> citation = citations.get(i);
                builder.append(i + 1).append(". [").append(citation.get("label")).append("] ")
                        .append(firstNonBlank(citation.get("title"), "未命名资料")).append("：")
                        .append(firstNonBlank(citation.get("snippet"), "暂无摘要"));
                if (StringUtils.isNotBlank(stringValue(citation.get("timeLabel")))) {
                    builder.append("（时间：").append(citation.get("timeLabel")).append("）");
                }
                builder.append("\n");
            }
            builder.append("\n");
            if (!actions.isEmpty()) {
                builder.append("接下来建议你：").append("\n");
                for (String action : actions) {
                    builder.append("- ").append(action).append("\n");
                }
            }
        }
        builder.append("\n引用来源：");
        if (citations.isEmpty()) {
            builder.append(" 当前没有可直接引用的系统资料。");
        } else {
            builder.append("\n");
            for (Map<String, Object> citation : citations) {
                builder.append("[").append(citation.get("label")).append("] ")
                        .append(citation.get("sourceTypeLabel")).append(" - ")
                        .append(citation.get("title")).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String callConfiguredAi(String question, List<Map<String, Object>> citations, List<Map<String, Object>> attachments) {
        Map<String, String> configMap = getConfigValueMap();
        String baseUrl = pickConfigValue(configMap, AI_BASE_KEYS);
        String apiKey = pickConfigValue(configMap, AI_KEY_KEYS);
        String model = pickConfigValue(configMap, AI_MODEL_KEYS);
        String apiPath = firstNonBlank(pickConfigValue(configMap, AI_PATH_KEYS), "chat/completions");
        String systemPrompt = firstNonBlank(pickConfigValue(configMap, AI_PROMPT_KEYS),
                "你是课程答疑助教。优先根据系统资料回答；如果系统资料不足，可以基于通用知识补充回答，但要明确说明哪些内容来自系统资料、哪些内容属于通用知识，不要编造。若回答中引用了系统资料，请保留资料编号，例如 [资料1][资料2]。") ;
        double temperature = pickConfigDouble(configMap, AI_TEMPERATURE_KEYS, 0.2D);
        if (StringUtils.isBlank(baseUrl) || StringUtils.isBlank(apiKey) || StringUtils.isBlank(model)) {
            return "";
        }
        if (apiKey.startsWith("ep-")) {
            return "";
        }

        String endpoint = resolveAiEndpoint(baseUrl, apiPath);
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);

        JSONArray messages = new JSONArray();
        messages.add(buildMessage("system", systemPrompt));
        messages.add(buildMessage("user", buildAiUserPrompt(question, citations, attachments)));
        requestBody.put("messages", messages);

        try {
            String response = postJson(endpoint, requestBody.toJSONString(), apiKey);
            return parseAiResponse(response);
        } catch (Exception e) {
            return "";
        }
    }

    private JSONObject buildMessage(String role, String content) {
        JSONObject message = new JSONObject();
        message.put("role", role);
        message.put("content", content);
        return message;
    }

    private String buildAiUserPrompt(String question, List<Map<String, Object>> citations, List<Map<String, Object>> attachments) {
        StringBuilder builder = new StringBuilder();
        builder.append("用户问题：").append(question).append("\n\n");
        if (attachments != null && !attachments.isEmpty()) {
            builder.append("附件信息：").append("\n");
            for (int i = 0; i < attachments.size(); i++) {
                Map<String, Object> attachment = attachments.get(i);
                builder.append("[附件").append(i + 1).append("] ")
                        .append(firstNonBlank(attachment.get("name"), "未命名附件"))
                        .append(" / ").append(firstNonBlank(attachment.get("type"), "未知类型"))
                        .append(" / 提取内容：").append(firstNonBlank(attachment.get("extractedText"), "暂无可解析文本"))
                        .append("\n");
            }
            builder.append("\n");
        }

        if (citations == null || citations.isEmpty()) {
            builder.append("当前没有检索到可直接引用的系统资料。你可以基于通用知识回答，但必须明确说明“当前没有系统资料证据支持，以下为通用知识说明”。");
            return builder.toString();
        }

        builder.append("系统资料：").append("\n");
        for (Map<String, Object> citation : citations) {
            builder.append("[").append(citation.get("label")).append("] ")
                    .append(citation.get("sourceTypeLabel")).append(" / 标题：")
                    .append(firstNonBlank(citation.get("title"), "未命名资料"));
            if (StringUtils.isNotBlank(stringValue(citation.get("subjectName")))) {
                builder.append(" / 科目：").append(citation.get("subjectName"));
            }
            if (StringUtils.isNotBlank(stringValue(citation.get("timeLabel")))) {
                builder.append(" / 时间：").append(citation.get("timeLabel"));
            }
            builder.append("\n")
                    .append("摘要：").append(firstNonBlank(citation.get("snippet"), "暂无摘要"))
                    .append("\n\n");
        }
        builder.append("请优先基于以上资料回答；如果资料不足，可以补充通用知识，但要明确区分，并在引用系统资料时保留资料编号。");
        return builder.toString();
    }

    private String ensureCitationFooter(String answer, List<Map<String, Object>> citations) {
        if (StringUtils.isBlank(answer)) {
            return "";
        }
        if (answer.contains("[资料")) {
            return answer.trim();
        }
        StringBuilder builder = new StringBuilder(answer.trim());
        builder.append("\n\n引用来源：");
        if (citations == null || citations.isEmpty()) {
            builder.append(" 当前没有可直接引用的系统资料，本次回答可能包含通用知识说明。");
        } else {
            builder.append("\n");
            for (Map<String, Object> citation : citations) {
                builder.append("[").append(citation.get("label")).append("] ")
                        .append(citation.get("sourceTypeLabel")).append(" - ")
                        .append(citation.get("title")).append("\n");
            }
        }
        return builder.toString().trim();
    }

    private String parseAiResponse(String response) {
        if (StringUtils.isBlank(response)) {
            return "";
        }
        JSONObject json = JSON.parseObject(response);
        if (json == null) {
            return "";
        }
        JSONArray choices = json.getJSONArray("choices");
        if (choices != null && !choices.isEmpty()) {
            JSONObject first = choices.getJSONObject(0);
            if (first != null && first.getJSONObject("message") != null) {
                Object content = first.getJSONObject("message").get("content");
                if (content instanceof String) {
                    return ((String) content).trim();
                }
                if (content instanceof JSONArray) {
                    StringBuilder builder = new StringBuilder();
                    JSONArray contentArray = (JSONArray) content;
                    for (int i = 0; i < contentArray.size(); i++) {
                        JSONObject part = contentArray.getJSONObject(i);
                        if (part != null) {
                            builder.append(firstNonBlank(part.getString("text"), part.getString("content"))).append("\n");
                        }
                    }
                    return builder.toString().trim();
                }
            }
        }
        if (StringUtils.isNotBlank(json.getString("output_text"))) {
            return json.getString("output_text").trim();
        }
        return "";
    }

    private String postJson(String endpoint, String payload, String apiKey) throws Exception {
        HttpURLConnection connection = (HttpURLConnection) new URL(endpoint).openConnection();
        connection.setRequestMethod("POST");
        connection.setConnectTimeout(30000);
        connection.setReadTimeout(90000);
        connection.setDoOutput(true);
        connection.setRequestProperty("Content-Type", "application/json; charset=UTF-8");
        connection.setRequestProperty("Authorization", "Bearer " + apiKey);
        byte[] body = payload.getBytes(StandardCharsets.UTF_8);
        connection.setRequestProperty("Content-Length", String.valueOf(body.length));

        DataOutputStream outputStream = new DataOutputStream(connection.getOutputStream());
        outputStream.write(body);
        outputStream.flush();
        outputStream.close();

        InputStream inputStream = connection.getResponseCode() >= 200 && connection.getResponseCode() < 300
                ? connection.getInputStream()
                : connection.getErrorStream();
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        StringBuilder response = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) {
            response.append(line);
        }
        reader.close();
        return response.toString();
    }

    private String resolveAiEndpoint(String baseUrl, String apiPath) {
        String normalizedBase = baseUrl.trim().replaceAll("/+$", "");
        if (normalizedBase.endsWith("/chat/completions") || normalizedBase.endsWith("/responses")) {
            return normalizedBase;
        }
        return normalizedBase + "/" + apiPath.trim().replaceAll("^/+", "");
    }

    private List<String> extractKeywords(String text) {
        String normalized = normalizePlainText(text);
        LinkedHashSet<String> keywords = new LinkedHashSet<String>();

        Matcher chineseMatcher = CHINESE_BLOCK_PATTERN.matcher(normalized);
        while (chineseMatcher.find()) {
            addKeywordVariants(chineseMatcher.group(), keywords);
        }

        Matcher asciiMatcher = ASCII_TOKEN_PATTERN.matcher(normalized.toLowerCase(Locale.ROOT));
        while (asciiMatcher.find()) {
            addKeyword(keywords, asciiMatcher.group());
        }

        if (keywords.isEmpty() && normalized.length() >= 2) {
            addKeywordVariants(normalized, keywords);
        }
        return new ArrayList<String>(keywords);
    }

    private void addKeywordVariants(String text, Set<String> keywords) {
        String value = normalizePlainText(text);
        addKeyword(keywords, value);
        if (value.length() <= 12) {
            for (int size = 2; size <= Math.min(4, value.length()); size++) {
                for (int index = 0; index + size <= value.length(); index++) {
                    addKeyword(keywords, value.substring(index, index + size));
                }
            }
        }
    }

    private void addKeyword(Set<String> keywords, String raw) {
        String keyword = stringValue(raw).trim().toLowerCase(Locale.ROOT);
        if (keyword.length() < 2 || KEYWORD_STOP_WORDS.contains(keyword)) {
            return;
        }
        keywords.add(keyword);
    }

    private String buildAttachmentContext(List<Map<String, Object>> attachments) {
        StringBuilder builder = new StringBuilder();
        for (Map<String, Object> attachment : attachments) {
            String extractedText = stringValue(attachment.get("extractedText"));
            String summary = firstNonBlank(extractedText, stringValue(attachment.get("summary")), stringValue(attachment.get("name")));
            if (StringUtils.isNotBlank(summary)) {
                builder.append(summary).append(' ');
            }
        }
        return normalizePlainText(builder.toString());
    }

    private List<Map<String, Object>> normalizeAttachmentList(Object raw) {
        if (!(raw instanceof Collection)) {
            return new ArrayList<Map<String, Object>>();
        }
        List<Map<String, Object>> attachments = new ArrayList<Map<String, Object>>();
        Collection<?> collection = (Collection<?>) raw;
        for (Object item : collection) {
            if (!(item instanceof Map)) {
                continue;
            }
            Map<?, ?> source = (Map<?, ?>) item;
            Map<String, Object> attachment = new LinkedHashMap<String, Object>();
            attachment.put("name", stringValue(source.get("name")));
            attachment.put("url", stringValue(source.get("url")));
            attachment.put("type", firstNonBlank(stringValue(source.get("type")), detectMediaType(stringValue(source.get("name")), stringValue(source.get("url")))));
            attachment.put("summary", stringValue(source.get("summary")));
            attachment.put("extractedText", normalizePlainText(firstNonBlank(source.get("extractedText"), source.get("analysisText"))));
            attachments.add(attachment);
        }
        return attachments;
    }

    private String extractTextFromAsset(String fileUrl, String fileName, String mediaType) {
        Path filePath = resolveUploadPath(fileUrl, fileName);
        if (filePath == null || !Files.exists(filePath)) {
            return "";
        }
        String extension = fileExtension(firstNonBlank(fileName, filePath.getFileName().toString()));
        try {
            if (isImageExtension(extension)) {
                return extractImageText(filePath);
            }
            if (Arrays.asList("txt", "md", "csv", "json", "xml").contains(extension)) {
                return new String(Files.readAllBytes(filePath), StandardCharsets.UTF_8);
            }
            if ("pdf".equals(extension)) {
                return extractPdfText(filePath);
            }
            if ("docx".equals(extension)) {
                return extractDocxText(filePath);
            }
            if ("doc".equals(extension)) {
                return extractDocText(filePath);
            }
            if ("pptx".equals(extension)) {
                return extractPptxText(filePath);
            }
            if ("ppt".equals(extension)) {
                return extractPptText(filePath);
            }
            if ("xls".equals(extension) || "xlsx".equals(extension)) {
                return extractWorkbookText(filePath);
            }
            if (Arrays.asList("mp3", "wav", "m4a", "aac", "amr").contains(extension) || "audio".equals(mediaType)) {
                return "";
            }
        } catch (Throwable e) {
            return "";
        }
        return "";
    }

    private String extractPdfText(Path filePath) throws Exception {
        PDDocument document = PDDocument.load(filePath.toFile());
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        } finally {
            document.close();
        }
    }

    private String extractDocxText(Path filePath) throws Exception {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        try {
            XWPFDocument document = new XWPFDocument(inputStream);
            try {
                XWPFWordExtractor extractor = new XWPFWordExtractor(document);
                try {
                    return extractor.getText();
                } finally {
                    extractor.close();
                }
            } finally {
                document.close();
            }
        } finally {
            inputStream.close();
        }
    }

    private String extractDocText(Path filePath) throws Exception {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        try {
            HWPFDocument document = new HWPFDocument(inputStream);
            try {
                WordExtractor extractor = new WordExtractor(document);
                try {
                    return extractor.getText();
                } finally {
                    extractor.close();
                }
            } finally {
                document.close();
            }
        } finally {
            inputStream.close();
        }
    }

    private String extractPptxText(Path filePath) throws Exception {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        try {
            XMLSlideShow slideShow = new XMLSlideShow(inputStream);
            try {
                StringBuilder builder = new StringBuilder();
                for (XSLFSlide slide : slideShow.getSlides()) {
                    if (slide == null) {
                        continue;
                    }
                    for (XSLFShape shape : slide.getShapes()) {
                        if (shape instanceof XSLFTextShape) {
                            builder.append(((XSLFTextShape) shape).getText()).append('\n');
                        }
                    }
                }
                return builder.toString();
            } finally {
                slideShow.close();
            }
        } finally {
            inputStream.close();
        }
    }

    private String extractPptText(Path filePath) throws Exception {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        try {
            HSLFSlideShow slideShow = new HSLFSlideShow(inputStream);
            try {
                StringBuilder builder = new StringBuilder();
                for (HSLFSlide slide : slideShow.getSlides()) {
                    if (slide == null) {
                        continue;
                    }
                    builder.append(StringUtils.defaultString(slide.getTitle())).append('\n');
                    List<List<HSLFTextParagraph>> paragraphs = slide.getTextParagraphs();
                    if (paragraphs == null) {
                        continue;
                    }
                    for (List<HSLFTextParagraph> paragraphGroup : paragraphs) {
                        if (paragraphGroup == null) {
                            continue;
                        }
                        for (HSLFTextParagraph paragraph : paragraphGroup) {
                            if (paragraph == null) {
                                continue;
                            }
                            for (HSLFTextRun textRun : paragraph.getTextRuns()) {
                                if (textRun != null) {
                                    builder.append(StringUtils.defaultString(textRun.getRawText())).append('\n');
                                }
                            }
                        }
                    }
                }
                return builder.toString();
            } finally {
                slideShow.close();
            }
        } finally {
            inputStream.close();
        }
    }

    private String extractWorkbookText(Path filePath) throws Exception {
        FileInputStream inputStream = new FileInputStream(filePath.toFile());
        DataFormatter formatter = new DataFormatter();
        StringBuilder builder = new StringBuilder();
        try {
            Workbook workbook = WorkbookFactory.create(inputStream);
            try {
                for (int i = 0; i < workbook.getNumberOfSheets(); i++) {
                    Sheet sheet = workbook.getSheetAt(i);
                    if (sheet == null) {
                        continue;
                    }
                    builder.append(sheet.getSheetName()).append('\n');
                    for (Row row : sheet) {
                        StringBuilder rowBuilder = new StringBuilder();
                        for (Cell cell : row) {
                            if (rowBuilder.length() > 0) {
                                rowBuilder.append(" | ");
                            }
                            rowBuilder.append(formatter.formatCellValue(cell));
                        }
                        builder.append(rowBuilder).append('\n');
                    }
                }
            } finally {
                workbook.close();
            }
        } finally {
            inputStream.close();
        }
        return builder.toString();
    }

    private String extractImageText(Path filePath) {
        if (!supportsOcr()) {
            return "";
        }
        Map<String, String> configMap = getConfigValueMap();
        String appId = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_app_id")), pickConfigValue(configMap, Arrays.asList("appid", "AppID")));
        String apiKey = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_api_key")), pickConfigValue(configMap, Arrays.asList("apikey", "APIKey")));
        String secretKey = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_secret_key")), pickConfigValue(configMap, Arrays.asList("secretkey", "SecretKey")));
        if (StringUtils.isBlank(appId) || StringUtils.isBlank(apiKey) || StringUtils.isBlank(secretKey)) {
            return "";
        }
        AipOcr client = new AipOcr(appId, apiKey, secretKey);
        HashMap<String, String> options = new HashMap<String, String>();
        options.put("language_type", "CHN_ENG");
        org.json.JSONObject result = client.basicGeneral(filePath.toString(), options);
        if (result == null) {
            return "";
        }
        try {
            org.json.JSONArray wordsResult = result.optJSONArray("words_result");
            if (wordsResult == null || wordsResult.length() == 0) {
                return "";
            }
            StringBuilder builder = new StringBuilder();
            for (int i = 0; i < wordsResult.length(); i++) {
                org.json.JSONObject line = wordsResult.optJSONObject(i);
                if (line != null) {
                    builder.append(line.optString("words")).append('\n');
                }
            }
            return builder.toString();
        } catch (JSONException e) {
            return "";
        }
    }

    private boolean supportsAutomaticParse(String fileName, String mediaType) {
        String extension = fileExtension(fileName);
        return supportsOcr() || Arrays.asList("pdf", "doc", "docx", "ppt", "pptx", "txt", "md", "csv", "json", "xls", "xlsx").contains(extension)
                || isImageExtension(extension) || "image".equals(mediaType);
    }

    private boolean supportsOcr() {
        Map<String, String> configMap = getConfigValueMap();
        String appId = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_app_id")), pickConfigValue(configMap, Arrays.asList("appid", "AppID")));
        String apiKey = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_api_key")), pickConfigValue(configMap, Arrays.asList("apikey", "APIKey")));
        String secretKey = firstNonBlank(pickConfigValue(configMap, Arrays.asList("ocr_secret_key")), pickConfigValue(configMap, Arrays.asList("secretkey", "SecretKey")));
        return StringUtils.isNotBlank(appId) && StringUtils.isNotBlank(apiKey) && StringUtils.isNotBlank(secretKey);
    }

    private String buildMediaHint(String mediaType, String extractedText) {
        if ("audio".equals(mediaType)) {
            return "语音文件已经接入附件链路，但自动转写需要额外配置语音识别服务。";
        }
        if (StringUtils.isNotBlank(extractedText)) {
            return "已自动提取附件里的文本内容，可以直接用于资料入库或答疑引用。";
        }
        if ("image".equals(mediaType)) {
            return supportsOcr() ? "这张图片暂时没有识别出可用文字，你也可以手动补充说明。" : "当前未配置 OCR，可先手动补充图片里的关键文字。";
        }
        return "这份附件已接入资料链路，如果没有自动内容，你可以手动补充摘要。";
    }

    private Path resolveUploadPath(String fileUrl, String fileName) {
        String resolvedName = fileNameFromUrl(fileUrl);
        if (StringUtils.isBlank(resolvedName)) {
            resolvedName = stringValue(fileName);
        }
        if (StringUtils.isBlank(resolvedName)) {
            return null;
        }
        return Paths.get(uploadDir, resolvedName);
    }

    private String fileNameFromUrl(String fileUrl) {
        String value = stringValue(fileUrl);
        if (StringUtils.isBlank(value)) {
            return "";
        }
        int queryIndex = value.indexOf('?');
        if (queryIndex >= 0) {
            value = value.substring(0, queryIndex);
        }
        int slashIndex = value.lastIndexOf('/');
        return slashIndex >= 0 ? value.substring(slashIndex + 1) : value;
    }

    private String fileExtension(String fileName) {
        String value = stringValue(fileName);
        int dotIndex = value.lastIndexOf('.');
        return dotIndex >= 0 ? value.substring(dotIndex + 1).toLowerCase(Locale.ROOT) : "";
    }

    private boolean isImageExtension(String extension) {
        return Arrays.asList("png", "jpg", "jpeg", "gif", "bmp", "webp").contains(extension);
    }

    private String detectMediaType(String fileName, String fileUrl) {
        String extension = fileExtension(firstNonBlank(fileName, fileUrl));
        if (isImageExtension(extension)) {
            return "image";
        }
        if (Arrays.asList("mp3", "wav", "m4a", "aac", "amr").contains(extension)) {
            return "audio";
        }
        if (Arrays.asList("pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx", "txt", "md", "csv", "json").contains(extension)) {
            return "document";
        }
        return "file";
    }

    private List<String> buildStepFeedback(String contentText, String subjectName) {
        List<String> feedback = new ArrayList<String>();
        if (StringUtils.isBlank(contentText)) {
            feedback.add("当前提交几乎没有正文说明，建议先补充你是怎么完成这道题或这次任务的。");
            feedback.add("补齐过程后，再让我继续帮你检查逻辑是否顺畅。");
            return feedback;
        }
        String[] parts = contentText.split("[\\n。！？；;]");
        for (String part : parts) {
            String segment = normalizePlainText(part);
            if (segment.length() < 6) {
                continue;
            }
            feedback.add("这一步提到了“" + excerpt(segment, 22) + "”，建议再补一句为什么这样做。");
            if (feedback.size() >= 4) {
                break;
            }
        }
        if (feedback.isEmpty()) {
            feedback.add("建议把" + firstNonBlank(subjectName, "本次作业") + "的完成过程拆成 2 到 4 个小步骤再提交。");
        }
        return feedback;
    }

    private String buildOverallReviewComment(String homeworkName, String subjectName, int score, String reviewLevel,
                                             List<String> strengths, List<String> issues, boolean isLate) {
        StringBuilder builder = new StringBuilder();
        builder.append("这次《").append(firstNonBlank(homeworkName, "作业")).append("》的 AI 复核评分约为 ")
                .append(score).append(" 分，当前等级 ").append(reviewLevel).append("。");
        builder.append(" 你在").append(firstNonBlank(subjectName, "本次内容")).append("上已经有基本完成度");
        builder.append(isLate ? "，但提交节奏需要明显加强。" : "，提交节奏也比较稳定。");
        builder.append(" 当前最值得优先优化的是：").append(firstNonBlank(firstItem(issues), "把关键步骤写清楚")).append(" ");
        builder.append("保留优势：").append(firstNonBlank(firstItem(strengths), "已经完成提交")).append("。");
        return builder.toString().trim();
    }

    private List<Map<String, Object>> buildPracticeList(List<Map<String, Object>> weakPoints, List<Map<String, Object>> subjectStats) {
        List<Map<String, Object>> practiceList = new ArrayList<Map<String, Object>>();
        for (Map<String, Object> weakPoint : weakPoints) {
            String weakTag = stringValue(weakPoint.get("weak_tag"));
            int total = parseInt(weakPoint.get("total"), 0);
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("title", weakTag + " 强化练习");
            item.put("focus", "最近有 " + total + " 次记录指向这个薄弱点。");
            item.put("prompt", "请围绕“" + weakTag + "”给我生成 3 道循序渐进的练习，并逐题讲解。");
            practiceList.add(item);
            if (practiceList.size() >= 4) {
                return practiceList;
            }
        }
        for (Map<String, Object> subjectStat : subjectStats) {
            String subjectName = stringValue(subjectStat.get("subject_name"));
            Map<String, Object> item = new LinkedHashMap<String, Object>();
            item.put("title", subjectName + " 复盘训练");
            item.put("focus", "当前平均得分 " + subjectStat.get("avg_score") + "，可以继续巩固。");
            item.put("prompt", "请根据我最近的" + subjectName + "作业表现，给我一份 15 分钟的复习清单。");
            practiceList.add(item);
            if (practiceList.size() >= 4) {
                break;
            }
        }
        return practiceList;
    }

    private String buildGrowthSummary(Map<String, Object> profile, Map<String, Object> overview,
                                      List<Map<String, Object>> weakPoints, List<Map<String, Object>> latestReviews) {
        StringBuilder builder = new StringBuilder();
        builder.append(firstNonBlank(profile.get("xueshengxingming"), "你")).append("最近累计有 ")
                .append(overview.get("reviewCount")).append(" 次作业 AI 复核、")
                .append(overview.get("wrongCount")).append(" 条错题沉淀。");
        if (!weakPoints.isEmpty()) {
            builder.append(" 当前最需要优先补的薄弱点是 ")
                    .append(firstNonBlank(weakPoints.get(0).get("weak_tag"), "表达完整度")).append("。");
        }
        if (!latestReviews.isEmpty()) {
            builder.append(" 建议先复盘最近一次点评，再结合个性化练习继续强化。");
        }
        return builder.toString();
    }

    private String buildClassroomSummary(Map<String, Object> overview, List<Map<String, Object>> subjectHeat, List<Map<String, Object>> weakFocus) {
        StringBuilder builder = new StringBuilder();
        builder.append("当前共沉淀 ").append(overview.get("totalSubmissions")).append(" 份提交，")
                .append(overview.get("pendingSubmissions")).append(" 份尚未完成 AI 复核。");
        if (!subjectHeat.isEmpty()) {
            builder.append(" 提交最集中的科目是 ").append(firstNonBlank(subjectHeat.get(0).get("subjectName"), "未分类")).append("。");
        }
        if (!weakFocus.isEmpty()) {
            builder.append(" 最近最突出的共性问题是 ").append(firstNonBlank(weakFocus.get(0).get("weak_tag"), "过程表达")).append("。");
        }
        builder.append(" 建议老师优先处理待批改作业，再围绕高频薄弱点补一次针对性讲解。");
        return builder.toString();
    }

    private List<Map<String, Object>> buildFrequentQuestions(List<Map<String, Object>> questionRows) {
        LinkedHashMap<String, Map<String, Object>> aggregate = new LinkedHashMap<String, Map<String, Object>>();
        for (Map<String, Object> row : questionRows) {
            String question = normalizePlainText(stringValue(row.get("question_text")));
            if (!isReadableQuestionText(question)) {
                continue;
            }
            String key = question.length() > 18 ? question.substring(0, 18) : question;
            Map<String, Object> item = aggregate.get(key);
            if (item == null) {
                item = new LinkedHashMap<String, Object>();
                item.put("question", question);
                item.put("count", 1);
                aggregate.put(key, item);
            } else {
                item.put("count", parseInt(item.get("count"), 0) + 1);
            }
        }
        List<Map<String, Object>> list = new ArrayList<Map<String, Object>>(aggregate.values());
        Collections.sort(list, new Comparator<Map<String, Object>>() {
            @Override
            public int compare(Map<String, Object> left, Map<String, Object> right) {
                return parseInt(right.get("count"), 0) - parseInt(left.get("count"), 0);
            }
        });
        return list.size() > 6 ? list.subList(0, 6) : list;
    }

    private boolean isReadableQuestionText(String question) {
        if (StringUtils.isBlank(question)) {
            return false;
        }
        String cleaned = question
                .replaceAll("[\\s\\?？!！,，.。;；:：\"“”'‘’、/\\\\|()（）\\[\\]{}【】<>《》_-]+", "")
                .replace("�", "");
        return StringUtils.isNotBlank(cleaned);
    }

    private String firstWeakTagForIssue(String issue, Set<String> weakTags) {
        if (issue.contains("步骤")) {
            return "解题步骤";
        }
        if (issue.contains("时间")) {
            return "时间管理";
        }
        if (issue.contains("图片")) {
            return "过程留痕";
        }
        if (issue.contains("说明") || issue.contains("表达")) {
            return "表达不完整";
        }
        return weakTags.isEmpty() ? "" : weakTags.iterator().next();
    }

    private List<String> parseJsonArray(Object value) {
        if (value == null) {
            return new ArrayList<String>();
        }
        try {
            return JSON.parseArray(String.valueOf(value), String.class);
        } catch (Exception e) {
            return new ArrayList<String>();
        }
    }

    private Map<String, String> getConfigValueMap() {
        List<ConfigEntity> configs = configService.selectList(null);
        Map<String, String> map = new HashMap<String, String>();
        for (ConfigEntity config : configs) {
            if (config == null || StringUtils.isBlank(config.getName())) {
                continue;
            }
            map.put(config.getName().toLowerCase(Locale.ROOT), StringUtils.trimToEmpty(config.getValue()));
        }
        return map;
    }

    private String pickConfigValue(Map<String, String> configMap, List<String> keys) {
        for (String key : keys) {
            String value = configMap.get(key.toLowerCase(Locale.ROOT));
            if (StringUtils.isNotBlank(value)) {
                return value;
            }
        }
        return "";
    }

    private double pickConfigDouble(Map<String, String> configMap, List<String> keys, double fallback) {
        String rawValue = pickConfigValue(configMap, keys);
        if (StringUtils.isBlank(rawValue)) {
            return fallback;
        }
        try {
            return Double.parseDouble(rawValue.trim());
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private void saveConfigValue(String name, String value) {
        ConfigEntity config = configService.selectOne(new EntityWrapper<ConfigEntity>().eq("name", name));
        if (config == null) {
            config = new ConfigEntity();
            config.setName(name);
            config.setValue(StringUtils.trimToEmpty(value));
            configService.insert(config);
        } else {
            config.setValue(StringUtils.trimToEmpty(value));
            configService.updateById(config);
        }
    }

    private String buildHomeworkTimeText(Object startTime, Object endTime) {
        List<String> parts = new ArrayList<String>();
        if (startTime != null) {
            parts.add("开始时间：" + formatDateTime(startTime));
        }
        if (endTime != null) {
            parts.add("截止时间：" + formatDateTime(endTime));
        }
        return joinCollection(parts, "；");
    }

    private String detectIntent(String question) {
        if (containsAny(question, Arrays.asList("截止", "什么时候", "几号", "时间", "日期", "多久"))) {
            return "time";
        }
        if (containsAny(question, Arrays.asList("课程", "视频", "讲了什么", "讲解", "知识点"))) {
            return "course";
        }
        if (containsAny(question, Arrays.asList("公告", "通知", "发布", "安排"))) {
            return "notice";
        }
        if (containsAny(question, Arrays.asList("批改", "点评", "复核", "改作业", "错题"))) {
            return "review";
        }
        return "general";
    }

    private boolean containsAny(String text, List<String> words) {
        String value = stringValue(text);
        for (String word : words) {
            if (value.contains(word)) {
                return true;
            }
        }
        return false;
    }

    private Map<String, Object> findSingle(String sql, Object... args) {
        List<Map<String, Object>> list = jdbcTemplate.queryForList(sql, args);
        return list.isEmpty() ? new LinkedHashMap<String, Object>() : list.get(0);
    }

    private void requireTeacherOrAdmin(SessionUser user, String message) {
        if (user == null || (!user.isTeacher() && !user.isAdmin())) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireLogin(SessionUser user, String message) {
        if (user == null || user.getUserId() == null) {
            throw new IllegalArgumentException(message);
        }
    }

    private void requireAdmin(SessionUser user, String message) {
        if (user == null || !user.isAdmin()) {
            throw new IllegalArgumentException(message);
        }
    }

    private String normalizeTableName(String tableName, String roleLabel) {
        String value = stringValue(tableName).toLowerCase(Locale.ROOT);
        if (StringUtils.isNotBlank(value)) {
            return value;
        }
        String role = stringValue(roleLabel);
        if (role.contains("教师") || "teacher".equalsIgnoreCase(role)) {
            return "jiaoshi";
        }
        if (role.contains("学生") || "student".equalsIgnoreCase(role)) {
            return "xuesheng";
        }
        return "users";
    }

    private String normalizeRoleKey(String tableName, String roleLabel) {
        if ("jiaoshi".equals(tableName)) {
            return "teacher";
        }
        if ("xuesheng".equals(tableName)) {
            return "student";
        }
        String role = stringValue(roleLabel).toLowerCase(Locale.ROOT);
        if ("teacher".equals(role) || role.contains("教师")) {
            return "teacher";
        }
        if ("student".equals(role) || role.contains("学生")) {
            return "student";
        }
        return "admin";
    }

    private String normalizePlainText(String value) {
        return stringValue(value)
                .replaceAll("<[^>]+>", " ")
                .replace("&nbsp;", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private String stripHtml(String value) {
        return stringValue(value).replaceAll("<[^>]+>", " ");
    }

    private String lower(Object value) {
        return stringValue(value).toLowerCase(Locale.ROOT);
    }

    private String excerpt(Object value, int maxLength) {
        String text = normalizePlainText(stringValue(value));
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, Math.max(maxLength, 0)) + "...";
    }

    private String firstItem(List<String> list) {
        return list == null || list.isEmpty() ? "" : list.get(0);
    }

    private String firstNonBlank(Object... values) {
        for (Object value : values) {
            if (StringUtils.isNotBlank(stringValue(value))) {
                return stringValue(value);
            }
        }
        return "";
    }

    private String joinCollection(Collection<?> values, String delimiter) {
        StringBuilder builder = new StringBuilder();
        if (values == null) {
            return "";
        }
        for (Object value : values) {
            String text = stringValue(value);
            if (StringUtils.isBlank(text)) {
                continue;
            }
            if (builder.length() > 0) {
                builder.append(delimiter);
            }
            builder.append(text);
        }
        return builder.toString();
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value).trim();
    }

    private String nullIfBlank(String value) {
        return StringUtils.isBlank(value) ? null : value.trim();
    }

    private String likeValue(String value) {
        return "%" + stringValue(value) + "%";
    }

    private int parseInt(Object value, int defaultValue) {
        if (value == null) {
            return defaultValue;
        }
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private Long parseLong(Object value) {
        if (value == null || StringUtils.isBlank(String.valueOf(value))) {
            return null;
        }
        try {
            return Long.valueOf(String.valueOf(value));
        } catch (Exception e) {
            return null;
        }
    }

    private Long firstNonNull(Long preferred, Long backup) {
        return preferred != null ? preferred : backup;
    }

    private Date parseDate(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Date) {
            return (Date) value;
        }
        if (value instanceof Timestamp) {
            return new Date(((Timestamp) value).getTime());
        }
        if (value instanceof Number) {
            long timestamp = ((Number) value).longValue();
            if (timestamp < 100000000000L) {
                timestamp = timestamp * 1000;
            }
            return new Date(timestamp);
        }
        String text = stringValue(value);
        if (StringUtils.isBlank(text)) {
            return null;
        }
        List<String> patterns = Arrays.asList("yyyy-MM-dd HH:mm:ss", "yyyy-MM-dd", "yyyy/MM/dd HH:mm:ss", "yyyy/MM/dd");
        for (String pattern : patterns) {
            try {
                return new SimpleDateFormat(pattern).parse(text);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String formatDateTime(Object value) {
        Date date = parseDate(value);
        if (date == null) {
            return "";
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(date);
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private double toDouble(Object value) {
        if (value == null) {
            return 0D;
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (Exception e) {
            return 0D;
        }
    }

    private long newId() {
        return System.currentTimeMillis() + ThreadLocalRandom.current().nextInt(1000);
    }

    public static class SessionUser {
        private Long userId;
        private String tableName;
        private String username;
        private String roleKey;
        private String roleLabel;
        private String displayName;
        private String teacherNo;
        private String teacherName;
        private String studentNo;
        private String studentName;

        public boolean isAdmin() {
            return "users".equals(tableName) || "admin".equals(roleKey);
        }

        public boolean isTeacher() {
            return "jiaoshi".equals(tableName) || "teacher".equals(roleKey);
        }

        public boolean isStudent() {
            return "xuesheng".equals(tableName) || "student".equals(roleKey);
        }

        public Long getUserId() {
            return userId;
        }

        public String getTableName() {
            return tableName;
        }

        public String getUsername() {
            return username;
        }

        public String getRoleKey() {
            return roleKey;
        }

        public String getDisplayName() {
            return displayName;
        }

        public String getTeacherNo() {
            return teacherNo;
        }

        public String getTeacherName() {
            return teacherName;
        }
    }
}
