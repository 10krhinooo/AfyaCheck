-- Expand the question bank with 17 additional questions grounded in real KENPHIA 2018
-- survey items, chosen for well-documented HIV/STI epidemiological predictive value
-- (see python-service/train_kenphia_decision_tree.py for the KENPHIA-column grounding).
-- Keep this in sync with get_questions_data() in that script and the mirrored logic in
-- python-service/decision_tree_service.py.

INSERT INTO question (question_key, question_text, question_type, section_title, options, display_order, is_active, question_text_sw, section_title_sw)
VALUES
    ('residence_type', 'Do you live in an urban or rural area?', 'multiple_choice', 'Personal Info', '["Urban","Rural"]'::jsonb, 45, true,
     'Je, unaishi mjini au vijijini?', 'Taarifa Binafsi'),
    ('age_first_sex', 'At what age did you first have sexual intercourse?', 'number', 'Sexual History', NULL, 46, true,
     'Ulikuwa na umri gani ulipofanya ngono kwa mara ya kwanza?', 'Historia ya Ngono'),
    ('male_circumcision', 'Are you circumcised?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 47, true,
     'Je, umetahiriwa?', 'Historia ya Matibabu'),
    ('partner_age_gap', 'How much older is your most recent partner than you?', 'multiple_choice', 'Sexual History', '["Same age or younger","1-5 years older","6-9 years older","10+ years older"]'::jsonb, 48, true,
     'Mpenzi wako wa hivi karibuni ni mkubwa kwako kwa kiasi gani?', 'Historia ya Ngono'),
    ('needle_sharing', 'Have you ever shared needles or syringes with anyone?', 'yes_no', 'Lifestyle', '["Yes","No"]'::jsonb, 49, true,
     'Je, umewahi kutumia sindano au sindano za sindano pamoja na mtu mwingine?', 'Mtindo wa Maisha'),
    ('sti_test_frequency', 'How often do you get tested for STIs?', 'multiple_choice', 'Medical History', '["Never","Once","Yearly","More than once a year"]'::jsonb, 50, true,
     'Unapimwa magonjwa ya zinaa mara ngapi?', 'Historia ya Matibabu'),
    ('blood_transfusion_history', 'Have you ever received a blood transfusion?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 51, true,
     'Je, umewahi kuongezewa damu?', 'Historia ya Matibabu'),
    ('hiv_knowledge_myths', 'How would you rate your knowledge of how HIV is and is not transmitted?', 'multiple_choice', 'Education', '["High comprehensive knowledge","Some misconceptions","Low knowledge"]'::jsonb, 52, true,
     'Ungetathminije uelewa wako wa jinsi VVU inavyoambukizwa na jinsi isivyoambukizwa?', 'Elimu'),
    ('tb_history', 'Have you ever been diagnosed with or treated for tuberculosis (TB)?', 'yes_no', 'Medical History', '["Yes","No"]'::jsonb, 53, true,
     'Je, umewahi kugundulika au kutibiwa kifua kikuu (TB)?', 'Historia ya Matibabu'),
    ('migration_history', 'Have you been away from home for more than one month in the last 12 months?', 'yes_no', 'Lifestyle', '["Yes","No"]'::jsonb, 54, true,
     'Je, umekuwa mbali na nyumbani kwa zaidi ya mwezi mmoja katika miezi 12 iliyopita?', 'Mtindo wa Maisha'),
    ('intimate_partner_violence', 'Have you ever experienced physical violence from a partner?', 'yes_no', 'Safety', '["Yes","No"]'::jsonb, 55, true,
     'Je, umewahi kupata unyanyasaji wa kimwili kutoka kwa mpenzi?', 'Usalama'),
    ('tobacco_use', 'Do you currently use tobacco products?', 'yes_no', 'Lifestyle', '["Yes","No"]'::jsonb, 56, true,
     'Je, kwa sasa unatumia bidhaa za tumbaku?', 'Mtindo wa Maisha'),
    ('age_first_marriage', 'At what age did you first get married or start living with a partner?', 'number', 'Personal Info', NULL, 57, true,
     'Ulikuwa na umri gani ulipooa/olewa au kuanza kuishi na mpenzi kwa mara ya kwanza?', 'Taarifa Binafsi'),
    ('polygamous_relationship', 'Does your spouse have other spouses (co-wives/co-husbands)?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 58, true,
     'Je, mwenzi wako ana wenzi wengine (mke/mume mwenza)?', 'Historia ya Ngono'),
    ('paid_for_sex', 'Have you ever paid someone in exchange for sex?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 59, true,
     'Je, umewahi kulipa mtu ili kufanya naye ngono?', 'Historia ya Ngono'),
    ('hiv_test_frequency', 'How often do you get tested for HIV?', 'multiple_choice', 'HIV Testing', '["Never","Once","Yearly","More than once a year"]'::jsonb, 60, true,
     'Unapimwa VVU mara ngapi?', 'Upimaji wa VVU'),
    ('relationship_duration', 'How long have you been with your current or most recent partner?', 'multiple_choice', 'Sexual History', '["Less than 6 months","6-12 months","1-3 years","3+ years"]'::jsonb, 61, true,
     'Umekuwa na mpenzi wako wa sasa au wa hivi karibuni kwa muda gani?', 'Historia ya Ngono')
ON CONFLICT (question_key) DO NOTHING;
