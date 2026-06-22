INSERT INTO config(name, value)
SELECT 'ai_user_enabled', '1' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_enabled');
INSERT INTO config(name, value)
SELECT 'ai_user_theme_color', '#009688' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_theme_color');
INSERT INTO config(name, value)
SELECT 'ai_user_entry_title', 'AI 智能助手' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_entry_title');
INSERT INTO config(name, value)
SELECT 'ai_user_entry_subtitle', '课程、作业、学习问题都可以问我' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_entry_subtitle');
INSERT INTO config(name, value)
SELECT 'ai_user_placeholder', '请输入你想咨询的问题' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_placeholder');
INSERT INTO config(name, value)
SELECT 'ai_user_fallback_text', '我暂时没理解清楚，请换个说法再试试。' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_fallback_text');
INSERT INTO config(name, value)
SELECT 'ai_user_avatar', '' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_avatar');
INSERT INTO config(name, value)
SELECT 'ai_user_welcome_text', '你好，我是你的课程 AI 助手。课程学习、作业提交、功能使用问题都可以直接问我。' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_welcome_text');
INSERT INTO config(name, value)
SELECT 'ai_user_system_prompt', '你是面向学生端的课程答疑助手。回答要简洁、友好、准确，优先围绕课程学习、作业和平台使用进行帮助；不确定时明确说明并引导用户补充信息。' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_system_prompt');
INSERT INTO config(name, value)
SELECT 'ai_user_quick_questions', '如何开始学习这门课？\n作业在哪里提交？\n课程视频打不开怎么办？' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_quick_questions');
INSERT INTO config(name, value)
SELECT 'ai_user_model_name', '' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_model_name');
INSERT INTO config(name, value)
SELECT 'ai_user_api_url', '' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_api_url');
INSERT INTO config(name, value)
SELECT 'ai_user_api_key', '' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_api_key');
INSERT INTO config(name, value)
SELECT 'ai_user_temperature', '0.7' FROM DUAL WHERE NOT EXISTS (SELECT 1 FROM config WHERE name='ai_user_temperature');
SELECT id, name, value FROM config WHERE name LIKE 'ai_user_%' ORDER BY id;
