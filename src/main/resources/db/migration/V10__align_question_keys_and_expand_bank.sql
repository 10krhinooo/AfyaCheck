-- Two fixes for decision-tree adaptivity (see python-service/decision_tree_service.py):
--
-- 1. Key alignment: the DB seeded 'hiv_test' but the decision-tree model, DecisionService's
--    ML_REQUIRED_FIELDS/risk logic, and the training data all use 'hiv_tested' — so that
--    answer never registered in the model's features. Rename it everywhere it's stored.
--
-- 2. Question-bank expansion: the tree was trained on 44 question keys but the DB only
--    seeded 27, so a third of the model's signal could never be collected. Seed the missing
--    17 with option values matching the model's _encode_answer() canon (option values are
--    canonical English; the SPA translates labels client-side).

UPDATE question SET question_key = 'hiv_tested' WHERE question_key = 'hiv_test';
UPDATE answer SET question_key = 'hiv_tested' WHERE question_key = 'hiv_test';

INSERT INTO question (question_key, question_text, question_type, section_title, options, display_order, is_active, question_text_sw, section_title_sw)
VALUES
    ('sexual_partners', 'How many sexual partners have you had in the past 12 months?', 'number', 'Sexual History', NULL, 28, true,
     'Umekuwa na wapenzi wangapi wa ngono katika miezi 12 iliyopita?', 'Historia ya Ngono'),
    ('multiple_partners', 'Have you had more than one sexual partner in the past year?', 'yes_no', 'Sexual History', '["Yes","No"]'::jsonb, 29, true,
     'Je, umekuwa na zaidi ya mpenzi mmoja wa ngono katika mwaka uliopita?', 'Historia ya Ngono'),
    ('discharge_symptom', 'Are you experiencing any unusual genital discharge?', 'yes_no', 'Symptoms and Health History', '["Yes","No"]'::jsonb, 30, true,
     'Je, unaona uchafu usio wa kawaida sehemu za siri?', 'Dalili na Historia ya Afya'),
    ('painful_urination', 'Are you experiencing pain or burning during urination?', 'yes_no', 'Symptoms and Health History', '["Yes","No"]'::jsonb, 31, true,
     'Je, unahisi maumivu au muwasho unapokojoa?', 'Dalili na Historia ya Afya'),
    ('genital_sores', 'Do you have any sores, bumps, or rashes in the genital area?', 'yes_no', 'Symptoms and Health History', '["Yes","No"]'::jsonb, 32, true,
     'Je, una vidonda, vipele, au upele sehemu za siri?', 'Dalili na Historia ya Afya'),
    ('symptom_duration', 'How long have you been experiencing these symptoms?', 'multiple_choice', 'Symptoms and Health History', '["Less than a week","1-4 weeks","1-3 months","More than 3 months"]'::jsonb, 33, true,
     'Umekuwa na dalili hizi kwa muda gani?', 'Dalili na Historia ya Afya'),
    ('partner_symptoms', 'Is your partner experiencing any similar symptoms?', 'yes_no', 'Symptoms and Health History', '["Yes","No"]'::jsonb, 34, true,
     'Je, mpenzi wako ana dalili zinazofanana?', 'Dalili na Historia ya Afya'),
    ('willing_to_test', 'Would you be willing to get tested for STIs if recommended?', 'yes_no', 'Testing History', '["Yes","No"]'::jsonb, 35, true,
     'Je, ungekuwa tayari kupimwa magonjwa ya zinaa ukishauriwa?', 'Upimaji wa VVU'),
    ('last_pap_smear', 'When was your last Pap smear or cervical cancer screening?', 'multiple_choice', 'Female Reproductive Health', '["Never","More than 3 years ago","1-3 years ago","Within the last year"]'::jsonb, 36, true,
     'Kipimo chako cha mwisho cha saratani ya shingo ya kizazi (Pap smear) kilikuwa lini?', 'Afya ya Uzazi'),
    ('alcohol_frequency', 'How often do you drink alcohol?', 'multiple_choice', 'Substance Use', '["Never","Monthly or less","2-4 times per month","2-3 times per week","4 or more times per week"]'::jsonb, 37, true,
     'Unakunywa pombe mara ngapi?', 'Mtindo wa Maisha'),
    ('partner_communication', 'Do you feel comfortable discussing sexual health with your partners?', 'yes_no', 'Relationship and Safety', '["Yes","No"]'::jsonb, 38, true,
     'Je, unajisikia huru kuzungumzia afya ya ngono na wapenzi wako?', 'Usalama'),
    ('partner_concurrency', 'Have any of your partners had other partners during your relationship?', 'yes_no', 'Relationship and Safety', '["Yes","No"]'::jsonb, 39, true,
     'Je, mpenzi wako yeyote amekuwa na wapenzi wengine mkiwa pamoja?', 'Usalama'),
    ('prevention_methods', 'Which STI prevention methods are you familiar with?', 'multiple_choice', 'Knowledge and Prevention', '["Condoms","PrEP","Regular testing","Abstinence","Mutual monogamy","Not familiar with any"]'::jsonb, 40, true,
     'Unafahamu njia zipi za kuzuia magonjwa ya zinaa?', 'Elimu'),
    ('hiv_prep', 'Have you heard of PrEP (Pre-Exposure Prophylaxis) for HIV prevention?', 'yes_no', 'Knowledge and Prevention', '["Yes","No"]'::jsonb, 41, true,
     'Je, umewahi kusikia kuhusu PrEP (kinga kabla ya maambukizi) ya kuzuia VVU?', 'Elimu'),
    ('health_priorities', 'How important is sexual health to your overall wellbeing?', 'multiple_choice', 'Knowledge and Prevention', '["Not important at all","Not very important","Somewhat important","Very important"]'::jsonb, 42, true,
     'Afya ya ngono ina umuhimu gani kwa ustawi wako kwa ujumla?', 'Elimu'),
    ('regular_provider', 'Do you have a regular healthcare provider?', 'yes_no', 'Healthcare Access', '["Yes","No"]'::jsonb, 43, true,
     'Je, una mtoa huduma za afya wa kawaida?', 'Upatikanaji wa Huduma'),
    ('testing_barriers', 'What are the main barriers to getting tested for you?', 'multiple_choice', 'Healthcare Access', '["Cost","Lack of time","Fear of results","Stigma","Not knowing where to go","No symptoms","Other"]'::jsonb, 44, true,
     'Ni vikwazo vipi vikuu vinavyokuzuia kupimwa?', 'Upatikanaji wa Huduma')
ON CONFLICT (question_key) DO NOTHING;
