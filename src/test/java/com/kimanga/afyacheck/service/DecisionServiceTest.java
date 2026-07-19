package com.kimanga.afyacheck.service;

import com.kimanga.afyacheck.model.Question;
import com.kimanga.afyacheck.repository.QuestionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class DecisionServiceTest {

    private DecisionTreeClient decisionTreeClient;
    private SessionService sessionService;
    private MLService mlService;
    private QuestionRepository questionRepository;
    private DecisionService decisionService;

    @BeforeEach
    void setUp() {
        decisionTreeClient = mock(DecisionTreeClient.class);
        sessionService = mock(SessionService.class);
        mlService = mock(MLService.class);
        questionRepository = mock(QuestionRepository.class);
        decisionService = new DecisionService(decisionTreeClient, sessionService, mlService, questionRepository);
    }

    private Question question(String key, String type) {
        Question q = new Question();
        q.setQuestionKey(key);
        q.setQuestionText("Text for " + key);
        q.setDescription("Desc");
        q.setQuestionType(type);
        q.setSectionTitle("Section");
        q.setDisplayOrder(1);
        return q;
    }

    @Test
    void getNextQuestionUsesFallbackWhenSessionIdMissing() {
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());

        Map<String, Object> result = decisionService.getNextQuestion(Map.of());

        assertThat(result.get("end")).isEqualTo(true);
    }

    @Test
    void getNextQuestionForcesConsentAsFirstQuestion() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(question("consent", "yes_no")));
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("consent")).thenReturn(Optional.of(question("consent", "yes_no")));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, Object> result = decisionService.getNextQuestion(Map.of("_sessionId", "sid-1"));

        assertThat(result.get("key")).isEqualTo("consent");
    }

    @Test
    void getNextQuestionReturnsConsentDeniedResponse() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of());

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-2");
        answers.put("consent", "No");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("consentDenied")).isEqualTo(true);
    }

    @Test
    void getNextQuestionReturnsConsentDeniedForLongFormAnswer() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of());

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-3");
        answers.put("consent", "No, I do not consent");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("consentDenied")).isEqualTo(true);
    }

    @Test
    void getNextQuestionReturnsQuestionFromDecisionTree() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(question("age", "number")));
        when(decisionTreeClient.getNextQuestion(eq("sid-4"), any(), any())).thenReturn("age");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("age")).thenReturn(Optional.of(question("age", "number")));
        when(questionRepository.countByIsActiveTrue()).thenReturn(5L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-4");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("key")).isEqualTo("age");
        assertThat(result.get("type")).isEqualTo("number");
    }

    @Test
    void getNextQuestionReturnsSwahiliTextWhenLocaleIsSwahiliAndFallsBackWhenUntranslated() {
        Question translated = question("age", "number");
        translated.setQuestionTextSw("Una umri gani?");
        translated.setSectionTitleSw("Taarifa Binafsi");
        // descriptionSw left null -> English fallback per-field
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(translated));
        when(decisionTreeClient.getNextQuestion(eq("sid-sw"), any(), any())).thenReturn("age");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("age")).thenReturn(Optional.of(translated));
        when(questionRepository.countByIsActiveTrue()).thenReturn(5L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-sw");
        answers.put("consent", "Yes");

        org.springframework.context.i18n.LocaleContextHolder.setLocale(java.util.Locale.of("sw"));
        try {
            Map<String, Object> result = decisionService.getNextQuestion(answers);
            assertThat(result.get("text")).isEqualTo("Una umri gani?");
            assertThat(result.get("sectionTitle")).isEqualTo("Taarifa Binafsi");
            assertThat(result.get("description")).isEqualTo("Desc");
        } finally {
            org.springframework.context.i18n.LocaleContextHolder.resetLocaleContext();
        }
    }

    @Test
    void getNextQuestionEndsSurveyWhenDecisionTreeReturnsNull() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(decisionTreeClient.getNextQuestion(eq("sid-5"), any(), any())).thenReturn(null);
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-5");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("end")).isEqualTo(true);
    }

    @Test
    void getNextQuestionReturnsDefaultResponseWhenQuestionNotInDatabase() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("missing_key");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("missing_key")).thenReturn(Optional.empty());

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-6");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("key")).isEqualTo("default");
    }

    @Test
    void getNextQuestionFallsBackOnException() {
        // getAllQuestionKeys() has its own internal try/catch and falls back to a
        // static list on failure, so an exception there never reaches this method's
        // own catch block. Throw from decisionTreeClient instead, which isn't
        // wrapped internally, to actually exercise getNextQuestion's outer catch.
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of());
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenThrow(new RuntimeException("service down"));
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of());

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-7");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result).isNotNull();
    }

    @Test
    void fallbackLogicReturnsNextUnansweredQuestion() {
        Question q1 = question("q1", "yes_no");
        Question q2 = question("q2", "text");
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(q1, q2));

        Map<String, String> answers = new HashMap<>();
        answers.put("q1", "yes");

        // Trigger fallback via missing session id
        answers.remove("_sessionId");
        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("key")).isEqualTo("q2");
    }

    @Test
    void fallbackLogicEndsSurveyWhenAllQuestionsAnswered() {
        Question q1 = question("q1", "yes_no");
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(q1));
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));

        Map<String, String> answers = new HashMap<>();
        answers.put("q1", "yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("end")).isEqualTo(true);
    }

    @Test
    void fallbackLogicEndsEarlyWhenEnoughInfoGathered() {
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(question("extra", "text")));
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));

        Map<String, String> answers = new HashMap<>();
        answers.put("consent", "Yes");
        answers.put("age", "30");
        answers.put("gender", "Male");
        answers.put("sexual_activity", "Yes");
        answers.put("recent_partners", "2");
        answers.put("condom_use", "Never");
        answers.put("high_risk_partner", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("end")).isEqualTo(true);
    }

    @Test
    void fallbackLogicEndsEarlyAfterFifteenAnswersWithoutDemographics() {
        when(questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc()).thenReturn(List.of(question("extra", "text")));
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));

        Map<String, String> answers = new HashMap<>();
        answers.put("consent", "Yes");
        for (int i = 0; i < 14; i++) {
            answers.put("q" + i, "answer");
        }
        // 15 total answers (consent + 14 q's), no age/gender/sexual_activity present,
        // so hasEnoughInformationFallback falls through to the answered >= 15 branch.

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("end")).isEqualTo(true);
    }

    @Test
    void getDefaultValueForFieldReturnsNoForUnrecognizedField() {
        String value = org.springframework.test.util.ReflectionTestUtils.invokeMethod(
                decisionService, "getDefaultValueForField", "some_unmapped_field");
        assertThat(value).isEqualTo("No");
    }

    @Test
    void numberQuestionIncludesValidationBounds() {
        Question q = question("age", "number");
        q.setMinValue(18);
        q.setMaxValue(99);
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("age");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("age")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-8");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        @SuppressWarnings("unchecked")
        Map<String, Object> validation = (Map<String, Object>) result.get("validation");
        assertThat(validation.get("min")).isEqualTo(18);
        assertThat(validation.get("max")).isEqualTo(99);
    }

    @Test
    void numberQuestionDefaultsValidationBoundsWhenMissing() {
        Question q = question("age", "number");
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("age");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("age")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-9");
        answers.put("consent", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        @SuppressWarnings("unchecked")
        Map<String, Object> validation = (Map<String, Object>) result.get("validation");
        assertThat(validation.get("min")).isEqualTo(0);
        assertThat(validation.get("max")).isEqualTo(100);
    }

    @Test
    void yesNoQuestionWithoutOptionsDefaultsToYesNo() {
        Question q = question("consent", "yes_no");
        q.setOptions(null);
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("consent");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("consent")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-10");
        answers.put("someprior", "Yes"); // avoid forced-first-consent branch

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("options")).isEqualTo("Yes,No");
    }

    // Note: parseOptionsToString's catch block (String.join(",", optionsList)
    // failing) is effectively unreachable through normal usage — String.join
    // tolerates null elements by rendering them as the literal text "null"
    // rather than throwing, so there's no realistic input that hits it without
    // deeper reflection tricks on the List implementation itself.

    @Test
    void unrecognizedQuestionTypeMapsToRadioByDefault() {
        Question q = question("misc", "essay");
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("misc");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("misc")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-10c");
        answers.put("someprior", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("type")).isEqualTo("radio");
    }

    @Test
    void questionWithOptionsJoinsThemWithComma() {
        Question q = question("marital_status", "multiple_choice");
        q.setOptions(List.of("Single", "Married"));
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("marital_status");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("marital_status")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenReturn(1L);

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-11");
        answers.put("someprior", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("options")).isEqualTo("Single,Married");
    }

    @Test
    void getAllQuestionKeysReturnsKeysFromRepository() {
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(question("q1", "text"), question("q2", "text")));
        List<String> keys = decisionService.getAllQuestionKeys();
        assertThat(keys).containsExactly("q1", "q2");
    }

    @Test
    void getAllQuestionKeysFallsBackToCriticalListOnException() {
        when(questionRepository.findByIsActiveTrue()).thenThrow(new RuntimeException("boom"));
        List<String> keys = decisionService.getAllQuestionKeys();
        assertThat(keys).contains("consent", "age", "gender");
    }

    @Test
    void calculateRiskScoreWithMLUsesMlResultOnSuccess() {
        when(mlService.predictRisk(any())).thenReturn(Map.of(
                "success", true,
                "confidence", 0.876,
                "hivProbability", 0.5,
                "riskScore", 60,
                "riskLevel", "High",
                "modelUsed", true,
                "recommendations", List.of("Get tested")
        ));

        Map<String, Object> result = decisionService.calculateRiskScoreWithML(Map.of("condom_use", "Never"));

        assertThat(result.get("riskScore")).isEqualTo(60);
        assertThat(result.get("riskLevel")).isEqualTo("High");
        assertThat(result.get("confidence")).isEqualTo(0.88);
        assertThat((String) result.get("recommendations")).contains("Get tested");
    }

    @Test
    void calculateRiskScoreWithMLFallsBackWhenUnsuccessful() {
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));

        Map<String, Object> result = decisionService.calculateRiskScoreWithML(Map.of());

        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void calculateRiskScoreWithMLFallsBackOnException() {
        when(mlService.predictRisk(any())).thenThrow(new RuntimeException("boom"));

        Map<String, Object> result = decisionService.calculateRiskScoreWithML(Map.of());

        assertThat(result.get("modelUsed")).isEqualTo(false);
    }

    @Test
    void fallbackRecommendationsIncludeAllEnhancedHints() {
        when(mlService.predictRisk(any())).thenThrow(new RuntimeException("boom"));

        Map<String, String> answers = new HashMap<>();
        answers.put("condom_use", "Never");
        answers.put("sti_symptoms", "Yes");
        answers.put("high_risk_partner", "Yes");

        Map<String, Object> result = decisionService.calculateRiskScoreWithML(answers);

        String recs = (String) result.get("recommendations");
        assertThat(recs).contains("Consistent condom use can significantly reduce STI transmission risk");
        assertThat(recs).contains("Consult a healthcare provider about your symptoms as soon as possible");
        assertThat(recs).contains("Consider discussing mutual testing with your partner(s)");
    }

    @Test
    void calculateRiskScoreDelegatesToCalculateRiskScoreWithML() {
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", false));
        Map<String, Object> result = decisionService.calculateRiskScore(Map.of());
        assertThat(result).containsKey("riskScore");
    }

    @Test
    void extractMLResultsUsesFallbackScoreWhenRiskScoreMissing() {
        when(mlService.predictRisk(any())).thenReturn(Map.of("success", true));

        Map<String, String> answers = Map.of("high_risk_partner", "Yes");
        Map<String, Object> result = decisionService.calculateRiskScoreWithML(answers);

        assertThat(result.get("riskScore")).isEqualTo(20);
        assertThat(result.get("riskLevel")).isEqualTo("Low");
    }

    @Test
    void getDecisionTreeStatusHealthy() {
        when(decisionTreeClient.isServiceHealthy()).thenReturn(true);
        Map<String, Object> status = decisionService.getDecisionTreeStatus();
        assertThat(status.get("status")).isEqualTo("HEALTHY");
    }

    @Test
    void getDecisionTreeStatusDegraded() {
        when(decisionTreeClient.isServiceHealthy()).thenReturn(false);
        Map<String, Object> status = decisionService.getDecisionTreeStatus();
        assertThat(status.get("status")).isEqualTo("DEGRADED");
    }

    @Test
    void getTotalActiveQuestionsCountFallsBackTo40OnException() {
        Question q = question("consent", "yes_no");
        when(questionRepository.findByIsActiveTrue()).thenReturn(List.of(q));
        when(decisionTreeClient.getNextQuestion(anyString(), any(), any())).thenReturn("consent");
        when(questionRepository.findByQuestionKeyAndIsActiveTrue("consent")).thenReturn(Optional.of(q));
        when(questionRepository.countByIsActiveTrue()).thenThrow(new RuntimeException("boom"));

        Map<String, String> answers = new HashMap<>();
        answers.put("_sessionId", "sid-11");
        answers.put("someprior", "Yes");

        Map<String, Object> result = decisionService.getNextQuestion(answers);

        assertThat(result.get("totalQuestions")).isEqualTo(40);
    }
}
