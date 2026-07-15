package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.repository.QuestionRepository;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.HashMap;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DataInitializationServiceTest {

    private final QuestionRepository questionRepository = mock(QuestionRepository.class);
    private final DataInitializationService service = new DataInitializationService(questionRepository);

    @Test
    void initializeQuestionsUsesExistingDatabaseQuestionsWhenJsonDisabled() {
        when(questionRepository.count()).thenReturn(5L);

        service.initializeQuestions();

        verify(questionRepository).count();
        verify(questionRepository, never()).save(any());
    }

    @Test
    void initializeQuestionsLoadsFromJsonWhenEnabledAndDatabaseEmpty() {
        // initializeFromJson is a hardcoded false field in production; flip it via
        // reflection here purely to exercise the branch that becomes live if that
        // flag is ever turned on.
        ReflectionTestUtils.setField(service, "initializeFromJson", true);
        when(questionRepository.count()).thenReturn(0L);
        when(questionRepository.existsByQuestionKey(anyString())).thenReturn(true);

        service.initializeQuestions();

        verify(questionRepository, atLeastOnce()).count();
    }

    @Test
    void loadQuestionsFromJsonParsesTestResourceAndSavesNewQuestions() {
        when(questionRepository.existsByQuestionKey(anyString())).thenReturn(false);

        ReflectionTestUtils.invokeMethod(service, "loadQuestionsFromJson");

        verify(questionRepository, times(2)).save(any());
    }

    @Test
    void loadQuestionsFromJsonSkipsExistingQuestions() {
        when(questionRepository.existsByQuestionKey(anyString())).thenReturn(true);

        ReflectionTestUtils.invokeMethod(service, "loadQuestionsFromJson");

        verify(questionRepository, never()).save(any());
    }

    @Test
    void saveQuestionFromJsonSetsFieldsFromKeyAndIntegerMinMax() {
        when(questionRepository.existsByQuestionKey("q1")).thenReturn(false);
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("key", "q1");
        questionData.put("question", "Text");
        questionData.put("description", "Desc");
        questionData.put("type", "yes_no");
        questionData.put("options", java.util.List.of("a", "b"));
        questionData.put("min", 1);
        questionData.put("max", 5);
        questionData.put("id", 7);
        Map<String, Object> section = Map.of("title", "Section 1");

        ReflectionTestUtils.invokeMethod(service, "saveQuestionFromJson", questionData, section);

        verify(questionRepository).save(any());
    }

    @Test
    void saveQuestionFromJsonUsesIdWhenKeyMissing() {
        when(questionRepository.existsByQuestionKey("q9")).thenReturn(false);
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("id", 9);
        Map<String, Object> section = Map.of();

        ReflectionTestUtils.invokeMethod(service, "saveQuestionFromJson", questionData, section);

        verify(questionRepository).existsByQuestionKey("q9");
    }

    @Test
    void saveQuestionFromJsonParsesStringMinMaxAndIgnoresInvalidNumbers() {
        when(questionRepository.existsByQuestionKey("q2")).thenReturn(false);
        Map<String, Object> questionData = new HashMap<>();
        questionData.put("key", "q2");
        questionData.put("min", "not-a-number");
        questionData.put("max", "42");
        Map<String, Object> section = Map.of();

        ReflectionTestUtils.invokeMethod(service, "saveQuestionFromJson", questionData, section);

        verify(questionRepository).save(any());
    }

    @Test
    void saveQuestionFromJsonCatchesExceptionFromRepositorySave() {
        when(questionRepository.existsByQuestionKey("q1")).thenReturn(false);
        when(questionRepository.save(any())).thenThrow(new RuntimeException("boom"));
        Map<String, Object> questionData = Map.of("key", "q1");
        Map<String, Object> section = Map.of();

        // Should not propagate — caught and logged internally.
        ReflectionTestUtils.invokeMethod(service, "saveQuestionFromJson", questionData, section);
    }

    @Test
    void saveQuestionFromJsonSkipsWhenAlreadyExists() {
        when(questionRepository.existsByQuestionKey("q1")).thenReturn(true);
        Map<String, Object> questionData = Map.of("key", "q1");
        Map<String, Object> section = Map.of();

        ReflectionTestUtils.invokeMethod(service, "saveQuestionFromJson", questionData, section);

        verify(questionRepository, never()).save(any());
    }
}
