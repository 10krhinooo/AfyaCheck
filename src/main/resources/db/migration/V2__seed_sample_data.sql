-- Sample data so the admin dashboard's KPIs and charts (user growth, answer completions,
-- question-type/section distribution, recent users) have something real to show in a fresh
-- environment, instead of the fallback static numbers baked into AdminService. Purely
-- additive and idempotent (ON CONFLICT DO NOTHING) so it's safe to run against a DB that
-- already has real data.

-- Questions matching the actual question_key values referenced by decision_tree.json (the
-- live decision tree driving the adaptive questionnaire), plus marital_status/education/
-- wealth_index which DecisionService.ML_REQUIRED_FIELDS needs for the ML risk model but
-- which aren't asked by the tree itself.
INSERT INTO question (question_key, question_text, question_type, section_title, options, display_order, is_active)
VALUES
    ('consent', 'Do you consent to take this confidential risk assessment?', 'yes_no', 'Consent', '["Yes","No"]'::jsonb, 1, true),
    ('age', 'What is your age?', 'number', 'Personal Info', NULL, 2, true),
    ('gender', 'What is your gender?', 'multiple_choice', 'Personal Info', '["Male","Female","Other"]'::jsonb, 3, true),
    ('marital_status', 'What is your marital status?', 'multiple_choice', 'Personal Info', '["Single","Married","Divorced","Widowed"]'::jsonb, 4, true),
    ('education', 'What is your highest level of education?', 'multiple_choice', 'Personal Info', '["None","Primary","Secondary","Tertiary"]'::jsonb, 5, true),
    ('wealth_index', 'How would you describe your household income level?', 'multiple_choice', 'Personal Info', '["Low","Middle","High"]'::jsonb, 6, true),
    ('sexual_activity', 'Are you currently sexually active?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 7, true),
    ('pregnancy_status', 'Are you currently pregnant?', 'yes_no', 'Reproductive Health', '["Yes","No"]'::jsonb, 8, true),
    ('contraception_use', 'What form of contraception do you currently use, if any?', 'multiple_choice', 'Reproductive Health', '["None","Condoms","Pills","Injection","IUD","Other"]'::jsonb, 9, true),
    ('recent_partners', 'How many sexual partners have you had in the last 3 months?', 'number', 'Sexual History', NULL, 10, true),
    ('condom_use', 'How often do you use condoms?', 'multiple_choice', 'Sexual History', '["Always","Sometimes","Never"]'::jsonb, 11, true),
    ('high_risk_partner', 'Do you believe any of your partners are at high risk for STIs/HIV?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 12, true),
    ('transactional_sex', 'Have you exchanged sex for money, goods, or favors?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 13, true),
    ('sti_symptoms', 'Are you currently experiencing any STI symptoms (discharge, painful urination, sores)?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 14, true),
    ('previous_sti', 'Have you ever been diagnosed with an STI?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 15, true),
    ('sti_treatment', 'Did you complete treatment for that STI?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 16, true),
    ('hiv_test', 'Have you ever been tested for HIV?', 'yes_no', 'HIV Testing', '["Yes","No"]'::jsonb, 17, true),
    ('last_hiv_test', 'When was your most recent HIV test?', 'multiple_choice', 'HIV Testing', '["Within last 3 months","3-6 months ago","6-12 months ago","Over a year ago"]'::jsonb, 18, true),
    ('other_sti_tests', 'Have you been tested for other STIs in the past year?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 19, true),
    ('sti_knowledge', 'How would you rate your knowledge of STI prevention?', 'multiple_choice', 'Education', '["Very knowledgeable","Somewhat knowledgeable","Not knowledgeable"]'::jsonb, 20, true),
    ('substance_sex', 'Have you used alcohol or drugs before sex in the last 3 months?', 'yes_no', 'Lifestyle', '["Yes","No"]'::jsonb, 21, true),
    ('drug_use', 'Have you used injectable drugs?', 'yes_no', 'Lifestyle', '["Yes","No"]'::jsonb, 22, true),
    ('sexual_coercion', 'Have you ever been pressured or forced into sexual activity?', 'yes_no', 'Safety', '["Yes","No"]'::jsonb, 23, true),
    ('partner_testing', 'Have your recent partner(s) been tested for HIV/STIs?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 24, true),
    ('insurance_coverage', 'Do you have health insurance coverage?', 'yes_no', 'Access to Care', '["Yes","No"]'::jsonb, 25, true),
    ('cost_barrier', 'Has cost ever prevented you from seeking sexual health care?', 'yes_no', 'Access to Care', '["Yes","No"]'::jsonb, 26, true),
    ('preferred_testing', 'Where would you prefer to get tested?', 'multiple_choice', 'Access to Care', '["Clinic","Home test kit","Community outreach","No preference"]'::jsonb, 27, true)
ON CONFLICT (question_key) DO NOTHING;

-- Sample users spread across the last 6 months, for the user-growth chart and the users
-- management table. Password for every seeded LOCAL account is "Password123!" (bcrypt hash
-- below) — for local dev/demo login only, never used in a real deployment.
INSERT INTO users (email, name, username, password, provider, email_verified, enabled, role, created_at, updated_at)
VALUES
    ('amina.mwangi@example.com', 'Amina Mwangi', 'amina.mwangi', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '5 months', CURRENT_TIMESTAMP - INTERVAL '5 months'),
    ('brian.otieno@example.com', 'Brian Otieno', 'brian.otieno', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '5 months', CURRENT_TIMESTAMP - INTERVAL '5 months'),
    ('cynthia.wanjiru@example.com', 'Cynthia Wanjiru', 'cynthia.wanjiru', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '4 months', CURRENT_TIMESTAMP - INTERVAL '4 months'),
    ('david.kiptoo@example.com', 'David Kiptoo', 'david.kiptoo', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '3 months', CURRENT_TIMESTAMP - INTERVAL '3 months'),
    ('esther.njeri@example.com', 'Esther Njeri', 'esther.njeri', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '2 months', CURRENT_TIMESTAMP - INTERVAL '2 months'),
    ('felix.mutua@example.com', 'Felix Mutua', 'felix.mutua', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '1 month', CURRENT_TIMESTAMP - INTERVAL '1 month'),
    ('grace.achieng@example.com', 'Grace Achieng', 'grace.achieng', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '20 days', CURRENT_TIMESTAMP - INTERVAL '20 days'),
    ('henry.kamau@example.com', 'Henry Kamau', 'henry.kamau', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'USER', CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ('irene.chebet@example.com', 'Irene Chebet', 'irene.chebet', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, false, 'USER', CURRENT_TIMESTAMP - INTERVAL '3 days', CURRENT_TIMESTAMP - INTERVAL '3 days'),
    ('admin.demo@example.com', 'Demo Administrator', 'admin.demo', '$2b$10$suO7SYBeuDERgdsrbqpVw.g8D91/KCbvatjaWHhH5VN9HcA5OK0wK', 'LOCAL', true, true, 'ADMIN', CURRENT_TIMESTAMP - INTERVAL '6 months', CURRENT_TIMESTAMP - INTERVAL '6 months')
ON CONFLICT (email) DO NOTHING;

-- Sample completed and in-progress questionnaire sessions over the last 30 days, for the
-- answer-completions chart.
INSERT INTO session (session_id, status, risk_score, created_at, updated_at)
VALUES
    ('seed-session-01', 'completed', 72, CURRENT_TIMESTAMP - INTERVAL '28 days', CURRENT_TIMESTAMP - INTERVAL '28 days'),
    ('seed-session-02', 'completed', 18, CURRENT_TIMESTAMP - INTERVAL '25 days', CURRENT_TIMESTAMP - INTERVAL '25 days'),
    ('seed-session-03', 'completed', 45, CURRENT_TIMESTAMP - INTERVAL '21 days', CURRENT_TIMESTAMP - INTERVAL '21 days'),
    ('seed-session-04', 'completed', 60, CURRENT_TIMESTAMP - INTERVAL '17 days', CURRENT_TIMESTAMP - INTERVAL '17 days'),
    ('seed-session-05', 'completed', 12, CURRENT_TIMESTAMP - INTERVAL '14 days', CURRENT_TIMESTAMP - INTERVAL '14 days'),
    ('seed-session-06', 'completed', 33, CURRENT_TIMESTAMP - INTERVAL '10 days', CURRENT_TIMESTAMP - INTERVAL '10 days'),
    ('seed-session-07', 'completed', 55, CURRENT_TIMESTAMP - INTERVAL '7 days', CURRENT_TIMESTAMP - INTERVAL '7 days'),
    ('seed-session-08', 'completed', 8, CURRENT_TIMESTAMP - INTERVAL '4 days', CURRENT_TIMESTAMP - INTERVAL '4 days'),
    ('seed-session-09', 'completed', 41, CURRENT_TIMESTAMP - INTERVAL '2 days', CURRENT_TIMESTAMP - INTERVAL '2 days'),
    ('seed-session-10', 'active', NULL, CURRENT_TIMESTAMP - INTERVAL '1 day', CURRENT_TIMESTAMP - INTERVAL '1 day')
ON CONFLICT (session_id) DO NOTHING;

-- A handful of answers per completed session, following the actual decision-tree path (not
-- the full set — just enough to give the answer-completions chart and question-level stats
-- real numbers).
INSERT INTO answer (session_id, question_key, answer_value, created_at, updated_at)
SELECT s.id, v.question_key, v.answer_value, s.created_at, s.created_at
FROM session s
JOIN (VALUES
    ('seed-session-01', 'consent', 'Yes'), ('seed-session-01', 'age', '29'), ('seed-session-01', 'gender', 'Male'), ('seed-session-01', 'sexual_activity', 'Yes'), ('seed-session-01', 'condom_use', 'Never'),
    ('seed-session-02', 'consent', 'Yes'), ('seed-session-02', 'age', '22'), ('seed-session-02', 'gender', 'Female'), ('seed-session-02', 'sexual_activity', 'No'),
    ('seed-session-03', 'consent', 'Yes'), ('seed-session-03', 'age', '34'), ('seed-session-03', 'gender', 'Female'), ('seed-session-03', 'sexual_activity', 'Yes'), ('seed-session-03', 'condom_use', 'Sometimes'),
    ('seed-session-04', 'consent', 'Yes'), ('seed-session-04', 'age', '27'), ('seed-session-04', 'gender', 'Male'), ('seed-session-04', 'sexual_activity', 'Yes'), ('seed-session-04', 'condom_use', 'Sometimes'), ('seed-session-04', 'high_risk_partner', 'Yes'),
    ('seed-session-05', 'consent', 'Yes'), ('seed-session-05', 'age', '24'), ('seed-session-05', 'gender', 'Female'), ('seed-session-05', 'sexual_activity', 'Yes'), ('seed-session-05', 'condom_use', 'Always'),
    ('seed-session-06', 'consent', 'Yes'), ('seed-session-06', 'age', '31'), ('seed-session-06', 'gender', 'Other'), ('seed-session-06', 'sexual_activity', 'Yes'), ('seed-session-06', 'condom_use', 'Sometimes'),
    ('seed-session-07', 'consent', 'Yes'), ('seed-session-07', 'age', '26'), ('seed-session-07', 'gender', 'Male'), ('seed-session-07', 'sexual_activity', 'Yes'), ('seed-session-07', 'condom_use', 'Never'), ('seed-session-07', 'transactional_sex', 'Yes'),
    ('seed-session-08', 'consent', 'Yes'), ('seed-session-08', 'age', '38'), ('seed-session-08', 'gender', 'Female'), ('seed-session-08', 'sexual_activity', 'Yes'), ('seed-session-08', 'condom_use', 'Always'),
    ('seed-session-09', 'consent', 'Yes'), ('seed-session-09', 'age', '30'), ('seed-session-09', 'gender', 'Male'), ('seed-session-09', 'sexual_activity', 'Yes'), ('seed-session-09', 'condom_use', 'Sometimes'),
    ('seed-session-10', 'consent', 'Yes'), ('seed-session-10', 'age', '25'), ('seed-session-10', 'gender', 'Female')
) AS v(session_id, question_key, answer_value) ON v.session_id = s.session_id
WHERE NOT EXISTS (
    SELECT 1 FROM answer a WHERE a.session_id = s.id AND a.question_key = v.question_key
);

-- Risk assessments for the completed sessions, driving the results/risk-level distribution
-- and giving the dashboard's "total questionnaires" stat something nonzero to show.
INSERT INTO risk_assessment (session_id, risk_level, risk_score, recommendations, created_at)
SELECT s.id, v.risk_level, v.risk_score, v.recommendations::jsonb, s.created_at
FROM session s
JOIN (VALUES
    ('seed-session-01', 'High', 72, '["High risk detected: Consider immediate STI testing","Discuss PrEP options with your doctor"]'),
    ('seed-session-02', 'Low', 18, '["Continue practicing safer sex","Consider regular screening every 12 months"]'),
    ('seed-session-03', 'Medium', 45, '["Moderate risk: Schedule STI testing soon","Review safer sex practices"]'),
    ('seed-session-04', 'Medium', 60, '["Moderate risk: Schedule STI testing soon","Consider regular screening every 6 months"]'),
    ('seed-session-05', 'Low', 12, '["Continue practicing safer sex"]'),
    ('seed-session-06', 'Medium', 33, '["Moderate risk: Schedule STI testing soon"]'),
    ('seed-session-07', 'High', 55, '["High risk detected: Consider immediate STI testing"]'),
    ('seed-session-08', 'Low', 8, '["Continue practicing safer sex"]'),
    ('seed-session-09', 'Medium', 41, '["Moderate risk: Schedule STI testing soon"]')
) AS v(session_id, risk_level, risk_score, recommendations) ON v.session_id = s.session_id
WHERE NOT EXISTS (
    SELECT 1 FROM risk_assessment ra WHERE ra.session_id = s.id
);
