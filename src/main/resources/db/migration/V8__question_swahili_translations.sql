-- Swahili translations for questionnaire content. Option VALUES stay canonical English —
-- they are what the ML pipeline (MLService's option lists, the Python encoders) expects;
-- the frontend translates option display labels client-side and submits canonical values.
ALTER TABLE question ADD COLUMN IF NOT EXISTS question_text_sw TEXT;
ALTER TABLE question ADD COLUMN IF NOT EXISTS description_sw TEXT;
ALTER TABLE question ADD COLUMN IF NOT EXISTS section_title_sw VARCHAR(255);

UPDATE question SET question_text_sw = v.text_sw, section_title_sw = v.section_sw
FROM (VALUES
    ('consent', 'Je, unakubali kufanya tathmini hii ya siri ya hatari?', 'Ridhaa'),
    ('age', 'Una umri gani?', 'Taarifa Binafsi'),
    ('gender', 'Jinsia yako ni ipi?', 'Taarifa Binafsi'),
    ('marital_status', 'Hali yako ya ndoa ni ipi?', 'Taarifa Binafsi'),
    ('education', 'Kiwango chako cha juu cha elimu ni kipi?', 'Taarifa Binafsi'),
    ('wealth_index', 'Ungeelezaje kiwango cha kipato cha kaya yako?', 'Taarifa Binafsi'),
    ('sexual_activity', 'Je, kwa sasa unajihusisha na ngono?', 'Historia ya Ngono'),
    ('pregnancy_status', 'Je, una ujauzito kwa sasa?', 'Afya ya Uzazi'),
    ('contraception_use', 'Unatumia njia gani ya kuzuia mimba kwa sasa, kama ipo?', 'Afya ya Uzazi'),
    ('recent_partners', 'Umekuwa na wapenzi wangapi wa ngono katika miezi mitatu iliyopita?', 'Historia ya Ngono'),
    ('condom_use', 'Unatumia kondomu mara ngapi?', 'Historia ya Ngono'),
    ('high_risk_partner', 'Je, unaamini mpenzi wako yeyote yuko katika hatari kubwa ya magonjwa ya zinaa au VVU?', 'Historia ya Ngono'),
    ('transactional_sex', 'Je, umewahi kubadilishana ngono kwa pesa, bidhaa, au upendeleo?', 'Historia ya Ngono'),
    ('sti_symptoms', 'Je, kwa sasa una dalili zozote za magonjwa ya zinaa (uchafu, maumivu wakati wa kukojoa, vidonda)?', 'Historia ya Matibabu'),
    ('previous_sti', 'Je, umewahi kugunduliwa na ugonjwa wa zinaa?', 'Historia ya Matibabu'),
    ('sti_treatment', 'Je, ulikamilisha matibabu ya ugonjwa huo wa zinaa?', 'Historia ya Matibabu'),
    ('hiv_test', 'Je, umewahi kupimwa VVU?', 'Upimaji wa VVU'),
    ('last_hiv_test', 'Kipimo chako cha VVU cha hivi karibuni kilikuwa lini?', 'Upimaji wa VVU'),
    ('other_sti_tests', 'Je, umepimwa magonjwa mengine ya zinaa katika mwaka uliopita?', 'Historia ya Matibabu'),
    ('sti_knowledge', 'Ungekadiriaje ujuzi wako wa kuzuia magonjwa ya zinaa?', 'Elimu'),
    ('substance_sex', 'Je, umetumia pombe au dawa za kulevya kabla ya ngono katika miezi mitatu iliyopita?', 'Mtindo wa Maisha'),
    ('drug_use', 'Je, umewahi kutumia dawa za kulevya za kujidunga?', 'Mtindo wa Maisha'),
    ('sexual_coercion', 'Je, umewahi kushinikizwa au kulazimishwa kufanya ngono?', 'Usalama'),
    ('partner_testing', 'Je, wapenzi wako wa hivi karibuni wamepimwa VVU au magonjwa ya zinaa?', 'Historia ya Ngono'),
    ('insurance_coverage', 'Je, una bima ya afya?', 'Upatikanaji wa Huduma'),
    ('cost_barrier', 'Je, gharama imewahi kukuzuia kutafuta huduma za afya ya uzazi?', 'Upatikanaji wa Huduma'),
    ('preferred_testing', 'Ungependa kupimwa wapi?', 'Upatikanaji wa Huduma')
) AS v(question_key, text_sw, section_sw)
WHERE question.question_key = v.question_key;
