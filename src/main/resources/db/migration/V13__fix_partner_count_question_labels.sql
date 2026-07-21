-- The live "recent_partners" and "sexual_partners" question text had their time windows
-- backwards relative to what the currently-deployed decision tree model
-- (python-service/decision_tree_model/kenphia_grounded_question_tree_model.joblib) was
-- actually trained on. Its own metadata sidecar (kenphia_question_mapping_metadata.json)
-- records the true KENPHIA source column for each key:
--   recent_partners  <- part12monum (partners in the past 12 months), but the seeded text
--                        asked about "the last 3 months"
--   sexual_partners  <- lifetimesex (lifetime partner count), but the seeded text asked
--                        about "the past 12 months"
-- so users were answering a different question than the one the model was scored against.
-- Retraining isn't possible here (the source KENPHIA CSV lives outside this repo), so this
-- corrects the question text to match the model's actual training grounding instead. This
-- also resolves the apparent redundancy of asking two overlapping-sounding windows
-- back-to-back: "partners in the past 12 months" and "partners in your lifetime" are two
-- standard, complementary STI-risk indicators (recent frequency vs. lifetime exposure), not
-- a repeat of the same question.
UPDATE question
SET question_text = 'How many sexual partners have you had in the past 12 months?',
    question_text_sw = 'Umekuwa na wapenzi wangapi wa ngono katika miezi 12 iliyopita?'
WHERE question_key = 'recent_partners';

UPDATE question
SET question_text = 'How many sexual partners have you had in your lifetime?',
    question_text_sw = 'Umekuwa na wapenzi wangapi wa ngono katika maisha yako yote?'
WHERE question_key = 'sexual_partners';
