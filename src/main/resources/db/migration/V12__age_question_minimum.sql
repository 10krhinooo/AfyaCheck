-- The risk model and questionnaire are meant for adolescents/adults; enforce a minimum age
-- of 10 on the "age" question so the number input and server-side validation both reject
-- younger ages instead of silently accepting them via the unset-min-value fallback of 0
-- (see DecisionService.convertQuestionToMap).
UPDATE question SET min_value = 10 WHERE question_key = 'age';
