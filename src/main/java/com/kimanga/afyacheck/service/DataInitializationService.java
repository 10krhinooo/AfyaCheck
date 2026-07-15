package com.kimanga.afyacheck.service;


import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.repository.QuestionRepository;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

@Service
public class DataInitializationService {

    private static final Logger logger = LoggerFactory.getLogger(DataInitializationService.class);
    private final QuestionRepository questionRepository;

    // Add a property to enable/disable JSON initialization
    private final boolean initializeFromJson = false;

    public DataInitializationService(QuestionRepository questionRepository) {
        this.questionRepository = questionRepository;
    }

    @PostConstruct
    public void initializeQuestions() {
        if (initializeFromJson && questionRepository.count() == 0) {
            logger.info("JSON initialization enabled - loading questions from JSON file...");
            loadQuestionsFromJson();
        } else {
            logger.info("Using existing database questions. Count: {}", questionRepository.count());
        }
    }

    private void loadQuestionsFromJson() {
        ObjectMapper mapper = new ObjectMapper();

        try {
            ClassPathResource resource = new ClassPathResource("questions.json");
            if (resource.exists()) {
                try (InputStream inputStream = resource.getInputStream()) {
                    Map<String, Object> questionsData = mapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
                    Object sectionsObj = questionsData.get("sections");
                    if (sectionsObj instanceof List) {
                        List<Map<String, Object>> sections = (List<Map<String, Object>>) sectionsObj;
                        for (Map<String, Object> section : sections) {
                            Object questionsObj = section.get("questions");
                            if (questionsObj instanceof List) {
                                List<Map<String, Object>> questions = (List<Map<String, Object>>) questionsObj;
                                for (Map<String, Object> questionData : questions) {
                                    saveQuestionFromJson(questionData, section);
                                }
                            }
                        }
                    }
                }
                logger.info("Successfully loaded questions from JSON file");
            }
        } catch (Exception e) {
            logger.error("Error loading questions from JSON file", e);
        }
    }

    private void saveQuestionFromJson(Map<String, Object> questionData, Map<String, Object> section) {
        try {
            Question question = new Question();

            Object idObj = questionData.get("id");
            Object keyObj = questionData.get("key");
            Object textObj = questionData.get("question");
            Object descObj = questionData.get("description");
            Object typeObj = questionData.get("type");
            Object optionsObj = questionData.get("options");
            Object minObj = questionData.get("min");
            Object maxObj = questionData.get("max");
            Object sectionTitleObj = section.get("title");

            // Set question key
            if (keyObj != null) {
                question.setQuestionKey(keyObj.toString());
            } else if (idObj != null) {
                question.setQuestionKey("q" + idObj);
            }

            // Check if question already exists
            if (questionRepository.existsByQuestionKey(question.getQuestionKey())) {
                logger.info("Question {} already exists, skipping", question.getQuestionKey());
                return;
            }

            // Set other fields
            if (textObj != null) question.setQuestionText(textObj.toString());
            if (descObj != null) question.setDescription(descObj.toString());
            if (typeObj != null) question.setQuestionType(typeObj.toString());
            if (optionsObj instanceof List) question.setOptions((List<String>) optionsObj);
            if (sectionTitleObj != null) question.setSectionTitle(sectionTitleObj.toString());

            // Set numeric values
            if (minObj instanceof Integer) {
                question.setMinValue((Integer) minObj);
            } else if (minObj != null) {
                try {
                    question.setMinValue(Integer.parseInt(minObj.toString()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            if (maxObj instanceof Integer) {
                question.setMaxValue((Integer) maxObj);
            } else if (maxObj != null) {
                try {
                    question.setMaxValue(Integer.parseInt(maxObj.toString()));
                } catch (NumberFormatException e) {
                    // ignore
                }
            }

            if (idObj instanceof Integer) {
                question.setDisplayOrder((Integer) idObj);
            }

            questionRepository.save(question);
            logger.info("Saved question: {}", question.getQuestionKey());

        } catch (Exception e) {
            logger.error("Error saving question from JSON data: {}", questionData, e);
        }
    }
}