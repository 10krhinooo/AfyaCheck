package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.repository.QuestionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Converts Question entities into the response shape the questionnaire
 * frontend (Thymeleaf and React) expects, and answers questions about the
 * active question set. Split out of DecisionService, which delegates here
 * so its callers (QuestionController) don't need to change.
 */
@Service
public class QuestionFormattingService {

    private static final Logger logger = LoggerFactory.getLogger(QuestionFormattingService.class);

    private static final List<String> CRITICAL_FALLBACK_QUESTIONS = List.of(
            "consent", "age", "gender", "sexual_activity", "hiv_tested", "previous_sti"
    );

    private final QuestionRepository questionRepository;

    public QuestionFormattingService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    public Map<String, Object> createQuestionResponse(String questionKey,
                                                        Map<String, String> currentAnswers,
                                                        String sessionId) {

        Optional<Question> questionOpt = questionRepository.findByQuestionKeyAndIsActiveTrue(questionKey);

        if (questionOpt.isEmpty()) {
            logger.error("Question not found in database: {}", questionKey);
            return createDefaultQuestionResponse(sessionId);
        }

        Question question = questionOpt.get();
        Map<String, Object> response = convertQuestionToMap(question);

        response.put("sessionId", sessionId);

        int totalQuestions = getTotalActiveQuestionsCount();
        int answeredCount = currentAnswers.size();
        int progress = (int) ((answeredCount / (double) totalQuestions) * 100);

        response.put("progress", progress);
        response.put("questionIndex", answeredCount + 1);
        response.put("totalQuestions", totalQuestions);

        logger.info("Generated question: {} (progress: {}%)", questionKey, progress);

        return response;
    }

    public Map<String, Object> convertQuestionToMap(Question question) {
        Map<String, Object> questionMap = new HashMap<>();

        questionMap.put("key", question.getQuestionKey());
        questionMap.put("text", question.getQuestionText());
        questionMap.put("description", question.getDescription());
        questionMap.put("type", mapQuestionType(question.getQuestionType()));
        questionMap.put("sectionTitle", question.getSectionTitle());
        questionMap.put("displayOrder", question.getDisplayOrder());

        String optionsString = parseOptionsToString(question.getOptions(), question.getQuestionType());
        questionMap.put("options", optionsString);

        logger.debug("Question: {}, Options String: {}", question.getQuestionKey(), optionsString);

        if ("number".equals(mapQuestionType(question.getQuestionType()))) {
            Map<String, Object> validation = new HashMap<>();
            validation.put("min", question.getMinValue() != null ? question.getMinValue() : 0);
            validation.put("max", question.getMaxValue() != null ? question.getMaxValue() : 100);
            questionMap.put("validation", validation);
            questionMap.put("min", validation.get("min"));
            questionMap.put("max", validation.get("max"));
        }

        return questionMap;
    }

    public String parseOptionsToString(List<String> optionsList, String questionType) {
        if (optionsList == null || optionsList.isEmpty()) {
            if ("yes_no".equalsIgnoreCase(questionType)) {
                return "Yes,No";
            }
            return "";
        }

        try {
            return String.join(",", optionsList);
        } catch (Exception e) {
            logger.warn("Error converting options list to string: {}. Using fallback.", optionsList);
            if ("yes_no".equalsIgnoreCase(questionType)) {
                return "Yes,No";
            }
            return "";
        }
    }

    public String mapQuestionType(String dbQuestionType) {
        if (dbQuestionType == null) return "radio";

        switch (dbQuestionType.toLowerCase()) {
            case "yes_no":
            case "multiple_choice":
                return "radio";
            case "number":
                return "number";
            case "text":
                return "text";
            default:
                return "radio";
        }
    }

    public int getTotalActiveQuestionsCount() {
        try {
            Long count = questionRepository.countByIsActiveTrue();
            return count != null ? count.intValue() : 40;
        } catch (Exception e) {
            logger.error("Error getting question count from database", e);
            return 40;
        }
    }

    public Map<String, Object> createDefaultQuestionResponse(String sessionId) {
        Map<String, Object> response = new HashMap<>();
        response.put("key", "default");
        response.put("text", "Please answer the following question:");
        response.put("description", "Please answer the following question:");
        response.put("type", "radio");
        response.put("sectionTitle", "Health Assessment");
        response.put("options", "Yes,No");
        response.put("sessionId", sessionId);
        response.put("progress", 0);
        response.put("questionIndex", 1);
        return response;
    }

    public List<Question> getActiveQuestionsInOrder() {
        return questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
    }

    /**
     * Fetches all active question keys from the database. Used by DecisionTreeClient.
     */
    public List<String> getAllQuestionKeys() {
        try {
            List<Question> activeQuestions = questionRepository.findByIsActiveTrue();
            return activeQuestions.stream()
                    .map(Question::getQuestionKey)
                    .collect(Collectors.toList());
        } catch (Exception e) {
            logger.error("Error getting question keys from database, using critical fallback list.", e);
            return CRITICAL_FALLBACK_QUESTIONS;
        }
    }

    public Map<String, Object> debugQuestionDatabase() {
        Map<String, Object> debugInfo = new HashMap<>();

        try {
            List<Question> allQuestions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            debugInfo.put("totalActiveQuestions", allQuestions.size());

            List<Map<String, Object>> questionsList = new ArrayList<>();
            for (Question q : allQuestions) {
                Map<String, Object> qInfo = new HashMap<>();
                qInfo.put("key", q.getQuestionKey());
                qInfo.put("text", q.getQuestionText());
                qInfo.put("type", q.getQuestionType());
                qInfo.put("section", q.getSectionTitle());
                qInfo.put("order", q.getDisplayOrder());
                qInfo.put("options", parseOptionsToString(q.getOptions(), q.getQuestionType()));
                questionsList.add(qInfo);
            }
            debugInfo.put("questions", questionsList);

        } catch (Exception e) {
            debugInfo.put("error", "Failed to fetch questions: " + e.getMessage());
        }

        return debugInfo;
    }
}
