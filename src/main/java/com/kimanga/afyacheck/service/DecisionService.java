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
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
public class DecisionService {

    private static final Logger logger = LoggerFactory.getLogger(DecisionService.class);

    private final QuestionRepository questionRepository;
    private final MLService mlService;

    private Map<String, Object> decisionTree = new HashMap<>();
    private Map<String, Question> questionMap = new LinkedHashMap<>();

    // Complete mapping between decision tree question IDs and database question keys
    private final Map<String, String> questionKeyMapping = createQuestionKeyMapping();

    // Mapping from current question keys to ML service expected keys
    private final Map<String, String> mlFieldMapping = createMLFieldMapping();

    public DecisionService(QuestionRepository questionRepository, MLService mlService) {
        this.questionRepository = questionRepository;
        this.mlService = mlService;
    }

    @PostConstruct
    public void initialize() {
        logger.info("🚀 Initializing DecisionService...");

        // Load decision tree from JSON (this is just the routing logic)
        ObjectMapper mapper = new ObjectMapper();
        this.decisionTree = loadJsonResource(mapper, "decision_tree.json");
        logger.info("📊 Decision tree loaded: {}", !decisionTree.isEmpty());

        // Load questions directly from database
        this.questionMap = loadQuestionsFromDatabase();
        logger.info("🗃️ Questions loaded directly from database: {}", questionMap.size());

        if (questionMap.isEmpty()) {
            logger.error("❌ CRITICAL: No questions found in database!");
        } else {
            logger.info("✅ Available question keys from database: {}", questionMap.keySet());
        }

        // Verify mapping
        verifyQuestionMapping();

        // Validate tree
        validateDecisionTreeAgainstQuestions();

        logger.info("🎯 DecisionService initialization completed. Questions: {}, Nodes: {}",
                questionMap.size(), countNodes(decisionTree));
    }

    private Map<String, String> createMLFieldMapping() {
        Map<String, String> mapping = new HashMap<>();

        // Map your current question keys to ML service expected keys
        mapping.put("age", "age");
        mapping.put("marital_status", "marital_status");
        mapping.put("education_level", "education");
        mapping.put("wealth_index", "wealth_index");
        mapping.put("hiv_test", "hiv_tested");
        mapping.put("recent_partners", "sexual_partners");
        mapping.put("condom_use", "condom_use");

        // Add any additional mappings needed
        mapping.put("multiple_partners", "sexual_partners"); // Fallback mapping

        return mapping;
    }

    private Map<String, String> mapToMLFields(Map<String, String> answers) {
        Map<String, String> mlAnswers = new HashMap<>();

        for (Map.Entry<String, String> entry : answers.entrySet()) {
            String mlField = mlFieldMapping.get(entry.getKey());
            if (mlField != null) {
                mlAnswers.put(mlField, entry.getValue());
            }
        }

        // Handle special mappings and transformations
        transformAnswersForML(mlAnswers, answers);

        logger.debug("Mapped answers for ML: {}", mlAnswers);
        return mlAnswers;
    }

    private void transformAnswersForML(Map<String, String> mlAnswers, Map<String, String> originalAnswers) {
        // Transform age if needed
        if (mlAnswers.containsKey("age") && mlAnswers.get("age") != null) {
            try {
                int age = Integer.parseInt(mlAnswers.get("age"));
                // Ensure age is within reasonable bounds
                if (age < 15 || age > 65) {
                    mlAnswers.put("age", "30"); // Default age
                }
            } catch (NumberFormatException e) {
                mlAnswers.put("age", "30"); // Default age
            }
        } else {
            mlAnswers.put("age", "30"); // Default age
        }

        // Transform sexual_partners if needed
        if (mlAnswers.containsKey("sexual_partners")) {
            String partners = mlAnswers.get("sexual_partners");
            if (partners != null) {
                try {
                    int partnerCount = Integer.parseInt(partners);
                    if (partnerCount >= 3) {
                        mlAnswers.put("sexual_partners", "3+");
                    } else if (partnerCount == 2) {
                        mlAnswers.put("sexual_partners", "2");
                    } else if (partnerCount == 1) {
                        mlAnswers.put("sexual_partners", "1");
                    } else {
                        mlAnswers.put("sexual_partners", "0");
                    }
                } catch (NumberFormatException e) {
                    // Keep original value if not numeric
                    if ("yes".equalsIgnoreCase(partners)) {
                        mlAnswers.put("sexual_partners", "2"); // Assume multiple
                    } else if ("no".equalsIgnoreCase(partners)) {
                        mlAnswers.put("sexual_partners", "1"); // Assume single
                    }
                }
            }
        }

        // Set default values for required ML fields if missing
        if (!mlAnswers.containsKey("marital_status")) {
            mlAnswers.put("marital_status", "married");
        }
        if (!mlAnswers.containsKey("education")) {
            mlAnswers.put("education", "secondary");
        }
        if (!mlAnswers.containsKey("wealth_index")) {
            mlAnswers.put("wealth_index", "middle");
        }
        if (!mlAnswers.containsKey("hiv_tested")) {
            mlAnswers.put("hiv_tested", "no");
        }
        if (!mlAnswers.containsKey("condom_use")) {
            mlAnswers.put("condom_use", "sometimes");
        }
    }

    private Map<String, Question> loadQuestionsFromDatabase() {
        Map<String, Question> map = new LinkedHashMap<>();
        try {
            List<Question> questions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            for (Question question : questions) {
                map.put(question.getQuestionKey(), question);
                logger.debug("Loaded question from database: {} - {}", question.getQuestionKey(), question.getQuestionText());
            }
            logger.info("✅ Successfully loaded {} active questions directly from database", questions.size());

        } catch (Exception e) {
            logger.error("❌ Error loading questions from database", e);
        }
        return map;
    }

    private void verifyQuestionMapping() {
        logger.info("🔗 Verifying question key mapping...");
        int foundCount = 0;
        int missingCount = 0;

        for (Map.Entry<String, String> entry : questionKeyMapping.entrySet()) {
            String decisionTreeId = entry.getKey();
            String databaseKey = entry.getValue();
            boolean exists = questionMap.containsKey(databaseKey);

            if (exists) {
                foundCount++;
                logger.debug("   ✅ {} -> {} [FOUND]", decisionTreeId, databaseKey);
            } else {
                missingCount++;
                logger.warn("   ❌ {} -> {} [MISSING]", decisionTreeId, databaseKey);
            }
        }

        logger.info("🔗 Mapping verification: {}/{} questions found in database", foundCount, questionKeyMapping.size());

        if (missingCount > 0) {
            logger.warn("⚠️  {} questions missing from database. Available keys: {}", missingCount, questionMap.keySet());
        }
    }

    private Map<String, String> createQuestionKeyMapping() {
        Map<String, String> mapping = new HashMap<>();

        // Complete mapping based on your database question keys
        mapping.put("q1", "consent");
        mapping.put("q2", "gender");
        mapping.put("q3", "age");
        mapping.put("q4", "sexual_activity");
        mapping.put("q5", "sexual_activity");
        mapping.put("q6", "recent_partners");
        mapping.put("q7", "condom_use");
        mapping.put("q8", "high_risk_partner");
        mapping.put("q9", "transactional_sex");
        mapping.put("q10", "multiple_partners");
        mapping.put("q11", "discharge_symptom");
        mapping.put("q12", "painful_urination");
        mapping.put("q13", "genital_sores");
        mapping.put("q14", "sti_symptoms");
        mapping.put("q15", "symptom_duration");
        mapping.put("q16", "previous_sti");
        mapping.put("q17", "sti_treatment");
        mapping.put("q18", "partner_symptoms");
        mapping.put("q19", "hiv_test");
        mapping.put("q20", "last_hiv_test");
        mapping.put("q21", "other_sti_tests");
        mapping.put("q22", "testing_barriers");
        mapping.put("q23", "willing_to_test");
        mapping.put("q24", "pregnancy_status");
        mapping.put("q25", "contraception_use");
        mapping.put("q26", "last_pap_smear");
        mapping.put("q27", "partner_concurrency");
        mapping.put("q28", "substance_sex");
        mapping.put("q29", "alcohol_frequency");
        mapping.put("q30", "drug_use");
        mapping.put("q31", "sexual_coercion");
        mapping.put("q32", "partner_communication");
        mapping.put("q33", "partner_testing");
        mapping.put("q34", "sti_knowledge");
        mapping.put("q35", "prevention_methods");
        mapping.put("q36", "hiv_prep");
        mapping.put("q37", "health_priorities");
        mapping.put("q38", "insurance_coverage");
        mapping.put("q39", "regular_provider");
        mapping.put("q40", "cost_barrier");
        mapping.put("q41", "preferred_testing");

        return mapping;
    }

    public Map<String, Object> getNextQuestion(Map<String, String> answers) {
        try {
            logger.info("getNextQuestion called with answers: {}", answers != null ? answers.keySet() : "null");

            if (questionMap.isEmpty()) {
                logger.error("No questions available in database!");
                return createErrorQuestion("No questions available in the system");
            }

            String consentAnswer = answers != null ? answers.get("consent") : null;
            if (consentAnswer != null && "no".equalsIgnoreCase(consentAnswer)) {
                logger.info("User declined consent, ending survey");
                return Map.of(
                        "text", "Thank you. You have declined to participate.",
                        "end", true,
                        "type", "end"
                );
            }

            // Determine next node by traversing the tree using current answers
            String nextNodeId = traverseFromRootForNextNode(answers);
            logger.info("Next node determined by traversal: {}", nextNodeId);

            if (nextNodeId == null) {
                // Nothing left in the tree - calculate final risk assessment
                logger.info("No next node found in tree, calculating final risk assessment");
                return calculateFinalResults(answers);
            }

            Map<String, Object> currentNode = findNodeInTree(nextNodeId, decisionTree);
            if (currentNode == null) {
                // Try to map node id to question mapping fallback
                logger.warn("Next node '{}' not found in decision tree object, falling back to linear progression", nextNodeId);
                return getNextLinearQuestion(nextNodeId);
            }

            // If node has direct question_key, return that question
            Object qKeyObj = currentNode.get("question_key");
            if (qKeyObj != null) {
                String questionKey = qKeyObj.toString();
                if (answers == null || !answers.containsKey(questionKey)) {
                    // question not yet answered -> ask it
                    logger.info("Asking question for node {} -> question_key {}", nextNodeId, questionKey);
                    Map<String, Object> question = getQuestionByKeyOrFallback(questionKey, nextNodeId);
                    return enhanceQuestionWithContext(question, mapQuestionKeyToNodeId(questionKey));
                } else {
                    // If it is already answered (race case), continue traversal
                    logger.info("Question '{}' already answered, traversing again", questionKey);
                    String next = traverseFromRootForNextNode(answers);
                    if (next == null) {
                        return calculateFinalResults(answers);
                    }
                    return getNextQuestion(answers); // re-evaluate
                }
            }

            // If node doesn't have question_key (internal node), evaluate it as usual
            return evaluateDecisionTree(nextNodeId, answers);

        } catch (Exception e) {
            logger.error("Error in getNextQuestion", e);
            return createErrorQuestion("System error: " + e.getMessage());
        }
    }

    /**
     * Calculate final results when survey is complete - USING ML SERVICE
     */
    private Map<String, Object> calculateFinalResults(Map<String, String> answers) {
        try {
            if (answers == null || answers.isEmpty()) {
                return Map.of(
                        "text", "Thank you for your interest in the assessment.",
                        "end", true,
                        "type", "completion"
                );
            }

            // Use ML service for risk assessment
            Map<String, Object> riskAssessment = calculateRiskScoreWithML(answers);

            logger.info("Final risk assessment calculated using ML - Score: {}, Level: {}, Model Used: {}",
                    riskAssessment.get("riskScore"), riskAssessment.get("riskLevel"),
                    riskAssessment.get("modelUsed"));

            Map<String, Object> finalResults = new HashMap<>();
            finalResults.put("text", "Thank you for completing the STI risk assessment!");
            finalResults.put("end", true);
            finalResults.put("type", "completion");
            finalResults.put("riskScore", riskAssessment.get("riskScore"));
            finalResults.put("riskLevel", riskAssessment.get("riskLevel"));
            finalResults.put("recommendations", riskAssessment.get("recommendations"));
            finalResults.put("hivProbability", riskAssessment.get("hivProbability"));
            finalResults.put("confidence", riskAssessment.get("confidence"));
            finalResults.put("modelUsed", riskAssessment.get("modelUsed"));

            return finalResults;

        } catch (Exception e) {
            logger.error("Error calculating final results", e);
            return Map.of(
                    "text", "Thank you for completing the assessment!",
                    "end", true,
                    "type", "completion"
            );
        }
    }

    /**
     * Calculate risk score using ML service with fallback to rule-based
     */
    public Map<String, Object> calculateRiskScoreWithML(Map<String, String> answers) {
        try {
            // Map answers to ML service format
            Map<String, String> mlAnswers = mapToMLFields(answers);

            logger.info("Calling ML service with mapped answers: {}", mlAnswers.keySet());

            // Call ML service
            Map<String, Object> mlResult = mlService.predictRisk(mlAnswers);

            if (mlResult != null && Boolean.TRUE.equals(mlResult.get("success"))) {
                logger.info("ML service returned successful prediction");

                // Extract ML results
                Double hivProbability = (Double) mlResult.get("hivProbability");
                Integer riskScore = (Integer) mlResult.get("riskScore");
                String riskLevel = (String) mlResult.get("riskLevel");
                Double confidence = (Double) mlResult.get("confidence");
                Boolean modelUsed = (Boolean) mlResult.get("modelUsed");

                @SuppressWarnings("unchecked")
                List<String> mlRecommendations = (List<String>) mlResult.get("recommendations");

                // Combine ML recommendations with additional context-aware recommendations
                List<String> enhancedRecommendations = enhanceRecommendations(
                        mlRecommendations != null ? mlRecommendations : new ArrayList<>(),
                        answers
                );

                return Map.of(
                        "riskScore", riskScore != null ? riskScore : calculateFallbackRiskScore(answers),
                        "riskLevel", riskLevel != null ? riskLevel : determineRiskLevel(riskScore != null ? riskScore : calculateFallbackRiskScore(answers)),
                        "recommendations", enhancedRecommendations,
                        "hivProbability", hivProbability != null ? hivProbability : 0.0,
                        "confidence", confidence != null ? confidence : 0.75,
                        "modelUsed", modelUsed != null ? modelUsed : false
                );

            } else {
                logger.warn("ML service returned error or unsuccessful, using fallback");
                return calculateFallbackRiskAssessment(answers);
            }

        } catch (Exception e) {
            logger.error("Error calling ML service, using fallback assessment", e);
            return calculateFallbackRiskAssessment(answers);
        }
    }

    /**
     * Fallback risk assessment when ML service is unavailable
     */
    private Map<String, Object> calculateFallbackRiskAssessment(Map<String, String> answers) {
        logger.info("Using fallback rule-based risk assessment");

        int riskScore = calculateFallbackRiskScore(answers);
        String riskLevel = determineRiskLevel(riskScore);
        List<String> recommendations = generateFallbackRecommendations(riskScore, answers);

        return Map.of(
                "riskScore", riskScore,
                "riskLevel", riskLevel,
                "recommendations", recommendations,
                "hivProbability", riskScore / 100.0,
                "confidence", 0.75,
                "modelUsed", false
        );
    }

    /**
     * Enhanced recommendations combining ML output with context-aware suggestions
     */
    private List<String> enhanceRecommendations(List<String> mlRecommendations, Map<String, String> answers) {
        List<String> enhanced = new ArrayList<>(mlRecommendations);

        // Add context-aware recommendations based on specific answers
        if ("yes".equalsIgnoreCase(answers.get("sti_symptoms"))) {
            addIfNotContains(enhanced, "Please consult a healthcare provider for symptom evaluation");
        }

        if ("yes".equalsIgnoreCase(answers.get("previous_sti"))) {
            addIfNotContains(enhanced, "Prior STI history increases risk; ensure post-treatment follow-up");
        }

        if ("yes".equalsIgnoreCase(answers.get("sexual_coercion"))) {
            addIfNotContains(enhanced, "If you've experienced sexual coercion, consider seeking support services");
        }

        // Always add these general recommendations if not already present
        addIfNotContains(enhanced, "Regular STI testing is recommended for sexually active individuals");
        addIfNotContains(enhanced, "Open communication with partners about sexual health is important");

        return enhanced;
    }

    private void addIfNotContains(List<String> list, String item) {
        if (!list.contains(item)) {
            list.add(item);
        }
    }

    /**
     * Original rule-based scoring as fallback
     */
    private int calculateFallbackRiskScore(Map<String, String> answers) {
        int riskScore = 0;

        // sexual activity
        if ("yes".equalsIgnoreCase(answers.get("sexual_activity"))) {
            riskScore += 20;
        }

        // recent partners count - prefer numeric field 'recent_partners', if absent fall back to multiple_partners
        String recentStr = answers.get("recent_partners");
        int recentCount = 0;
        if (recentStr != null && isNumeric(recentStr)) {
            recentCount = (int) Double.parseDouble(recentStr);
        } else if ("yes".equalsIgnoreCase(answers.get("multiple_partners"))) {
            recentCount = 2; // assume >1
        }
        if (recentCount > 1) {
            riskScore += 25;
        }

        // condom use normalization
        String condom = answers.getOrDefault("condom_use", "").trim();
        String condomLower = condom == null ? "" : condom.toLowerCase();
        if (condomLower.equals("sometimes") || condomLower.equals("never") || condomLower.equals("inconsistent")) {
            riskScore += 30;
        } else if (condomLower.equals("always") || condomLower.equals("consistent") || condomLower.equals("always use")) {
            // protective -> subtract some points if sexual activity true
            if ("yes".equalsIgnoreCase(answers.get("sexual_activity"))) {
                riskScore -= 5; // small reduction for consistent protection
                if (riskScore < 0) riskScore = 0;
            }
        }

        // STI symptoms
        if ("yes".equalsIgnoreCase(answers.get("sti_symptoms"))
                || "yes".equalsIgnoreCase(answers.get("discharge_symptom"))
                || "yes".equalsIgnoreCase(answers.get("painful_urination"))
                || "yes".equalsIgnoreCase(answers.get("genital_sores"))) {
            riskScore += 35;
        }

        // previous STI history
        if ("yes".equalsIgnoreCase(answers.get("previous_sti"))) {
            riskScore += 15;
        }

        // HIV testing status
        if ("no".equalsIgnoreCase(answers.get("hiv_test")) || answers.get("hiv_test") == null) {
            riskScore += 10;
        }

        // Substance use before sex increases risk
        if ("yes".equalsIgnoreCase(answers.get("substance_sex"))) {
            riskScore += 10;
        }

        // sexual coercion flag increases vulnerability
        if ("yes".equalsIgnoreCase(answers.get("sexual_coercion"))) {
            riskScore += 15;
        }

        // age-based tweak: younger age with multiple partners increases risk slightly
        String ageStr = answers.get("age");
        if (ageStr != null && isNumeric(ageStr)) {
            int age = (int) Double.parseDouble(ageStr);
            if (age >= 13 && age <= 24 && recentCount > 1) {
                riskScore += 5;
            }
        }

        // Bound the score to [0, 100]
        if (riskScore < 0) riskScore = 0;
        if (riskScore > 100) riskScore = 100;

        return riskScore;
    }

    private String determineRiskLevel(int riskScore) {
        if (riskScore >= 70) {
            return "High";
        } else if (riskScore >= 40) {
            return "Medium";
        } else {
            return "Low";
        }
    }

    private List<String> generateFallbackRecommendations(int riskScore, Map<String, String> answers) {
        List<String> recommendations = new ArrayList<>();

        if (riskScore >= 70) {
            recommendations.add("High risk detected - please consult a healthcare provider for testing and counseling");
        } else if (riskScore >= 40) {
            recommendations.add("Moderate risk - consider regular STI testing and risk reduction strategies");
        } else {
            recommendations.add("Low risk - maintain safe sexual practices and consider regular check-ups");
        }

        // Context-specific recommendations
        String condom = answers.getOrDefault("condom_use", "").toLowerCase();
        if (condom.equals("sometimes") || condom.equals("never")) {
            recommendations.add("Consistent condom use can significantly reduce STI transmission risk");
        }

        String recentStr = answers.get("recent_partners");
        if (recentStr != null && isNumeric(recentStr)) {
            int recentCount = (int) Double.parseDouble(recentStr);
            if (recentCount > 1) {
                recommendations.add("Consider reducing the number of sexual partners to lower STI risk");
            }
        }

        if ("yes".equalsIgnoreCase(answers.get("sti_symptoms"))) {
            recommendations.add("Please consult a healthcare provider for symptom evaluation");
        }

        if ("yes".equalsIgnoreCase(answers.get("previous_sti"))) {
            recommendations.add("Prior STI history increases risk; ensure post-treatment follow-up and partner notification");
        }

        if ("no".equalsIgnoreCase(answers.get("hiv_test"))) {
            recommendations.add("Consider getting tested for HIV, especially if sexually active");
        }

        if ("yes".equalsIgnoreCase(answers.get("substance_sex"))) {
            recommendations.add("Avoid alcohol/drug use before sex as it can impair safer-sex decisions");
        }

        if ("yes".equalsIgnoreCase(answers.get("sexual_coercion"))) {
            recommendations.add("If you've experienced sexual coercion, consider seeking support services");
        }

        // Always-good recommendations
        recommendations.add("Regular STI testing is recommended for sexually active individuals");
        recommendations.add("Open communication with partners about sexual health is important");

        return recommendations;
    }

    /**
     * Traverses the decision tree from root following the conditions and returns the next node_id
     * whose question_key is not yet answered (i.e., should be asked next). If tree finished, returns null.
     */
    private String traverseFromRootForNextNode(Map<String, String> answers) {
        try {
            if (decisionTree == null || decisionTree.isEmpty()) {
                return null;
            }
            return traverseNodeForNext((Map<String, Object>) decisionTree, answers, new HashSet<>());
        } catch (Exception e) {
            logger.error("Error during tree traversal", e);
            return null;
        }
    }

    @SuppressWarnings("unchecked")
    private String traverseNodeForNext(Map<String, Object> node, Map<String, String> answers, Set<String> visited) {
        if (node == null) return null;

        Object nodeIdObj = node.get("node_id");
        String nodeId = nodeIdObj != null ? nodeIdObj.toString() : null;
        if (nodeId != null && visited.contains(nodeId)) {
            // avoid loops
            return null;
        }
        if (nodeId != null) visited.add(nodeId);

        // If this node has a question_key and it's unanswered -> this is the next question
        Object questionKeyObj = node.get("question_key");
        if (questionKeyObj != null) {
            String questionKey = questionKeyObj.toString();
            if (questionKey != null && !answers.containsKey(questionKey)) {
                return nodeId;
            }
        }

        // Otherwise, evaluate children in order to find matching path
        Object childrenObj = node.get("children");
        if (childrenObj instanceof List) {
            List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;
            for (Map<String, Object> child : children) {
                String cond = child.get("condition") != null ? child.get("condition").toString() : null;
                boolean matches = cond == null || evaluateCondition(cond, answers);
                logger.debug("Traverse: nodeId={}, checking child condition='{}' -> {}", nodeId, cond, matches);
                if (matches) {
                    String action = child.get("action") != null ? child.get("action").toString() : null;
                    if ("goto".equals(action)) {
                        Object nextObj = child.get("next");
                        if (nextObj != null) {
                            String nextNodeId = nextObj.toString();
                            Map<String, Object> nextNode = findNodeInTree(nextNodeId, decisionTree);
                            // recurse into the referenced subtree
                            String found = traverseNodeForNext(nextNode, answers, visited);
                            if (found != null) return found;
                        }
                    } else if ("end".equals(action)) {
                        // end -> no next question
                        return null;
                    } else if ("ask".equals(action)) {
                        Object q = child.get("question");
                        if (q != null) {
                            String qId = q.toString();
                            // try to map this question id to a question_key (via mapping)
                            String mappedKey = questionKeyMapping.getOrDefault(qId, qId);
                            if (!answers.containsKey(mappedKey)) {
                                return mapQuestionKeyToNodeId(mappedKey);
                            }
                        }
                    }
                }
            }
        }

        // Check subtrees map (descend all subtrees if needed)
        Object subtreesObj = node.get("subtrees");
        if (subtreesObj instanceof Map) {
            Map<String, Object> subtrees = (Map<String, Object>) subtreesObj;
            for (Object subtreeObj : subtrees.values()) {
                if (subtreeObj instanceof Map) {
                    String found = traverseNodeForNext((Map<String, Object>) subtreeObj, answers, visited);
                    if (found != null) return found;
                }
            }
        }

        return null;
    }

    private Map<String, Object> getQuestionByKeyOrFallback(String questionKey, String preferredNodeId) {
        // Try direct DB lookup by question key
        Question q = questionMap.get(questionKey);
        if (q != null) {
            return convertQuestionToMap(q, preferredNodeId != null ? preferredNodeId : questionKey);
        }
        // fallback to createFallbackQuestions or getQuestionById mapping
        String mappedNodeId = mapQuestionKeyToNodeId(questionKey);
        return getFallbackQuestion(mappedNodeId != null ? mappedNodeId : "q1");
    }

    private String mapQuestionKeyToNodeId(String questionKey) {
        // find a node id in mapping that points to this key
        for (Map.Entry<String, String> e : questionKeyMapping.entrySet()) {
            if (e.getValue().equalsIgnoreCase(questionKey)) return e.getKey();
        }
        // fallback: return questionKey as nodeId (some nodes use question_key values directly)
        return questionKey;
    }

    private Map<String, Object> getQuestionById(String questionId) {
        logger.debug("Looking for question: {}", questionId);

        // First, check if we have a mapping for this question ID
        String mappedKey = questionKeyMapping.get(questionId);
        if (mappedKey != null) {
            Question question = questionMap.get(mappedKey);
            if (question != null) {
                logger.debug("Found question via mapping: {} -> {}", questionId, mappedKey);
                return convertQuestionToMap(question, questionId);
            } else {
                logger.warn("Mapped key '{}' not found in database for questionId: {}", mappedKey, questionId);
            }
        }

        // Fallback: try to find the question directly in database by questionId interpreted as question_key
        logger.info("Falling back to direct database lookup for: {}", questionId);
        Optional<Question> dbQuestion = findQuestionInDatabase(questionId);

        if (dbQuestion.isPresent()) {
            logger.info("Found question in database: {}", questionId);
            // Cache it for future use
            questionMap.put(dbQuestion.get().getQuestionKey(), dbQuestion.get());
            return convertQuestionToMap(dbQuestion.get(), questionId);
        }

        // Last attempt: if questionId looks like a question key, try that
        if (questionMap.containsKey(questionId)) {
            Question q = questionMap.get(questionId);
            return convertQuestionToMap(q, questionId);
        }

        logger.error("Question not found: {}. Available in database: {}", questionId, questionMap.keySet());
        return createErrorQuestion("Question not found: " + questionId);
    }

    private Optional<Question> findQuestionInDatabase(String searchKey) {
        // Try exact match first
        Optional<Question> question = questionRepository.findByQuestionKeyAndIsActiveTrue(searchKey);
        if (question.isPresent()) {
            return question;
        }

        // Try case-insensitive search
        List<Question> allQuestions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
        return allQuestions.stream()
                .filter(q -> q.getQuestionKey().equalsIgnoreCase(searchKey))
                .findFirst();
    }

    private Map<String, Object> convertQuestionToMap(Question question, String questionId) {
        Map<String, Object> questionData = new HashMap<>();

        questionData.put("id", questionId);
        questionData.put("key", question.getQuestionKey());
        questionData.put("question", question.getQuestionText());
        questionData.put("text", question.getQuestionText());
        questionData.put("description", question.getDescription());
        questionData.put("type", question.getQuestionType());
        questionData.put("options", question.getOptions());
        questionData.put("min", question.getMinValue());
        questionData.put("max", question.getMaxValue());
        questionData.put("sectionTitle", question.getSectionTitle());

        return questionData;
    }

    private Map<String, Object> evaluateDecisionTree(String currentNodeId, Map<String, String> answers) {
        try {
            if (currentNodeId == null) {
                return getFallbackQuestion("q1");
            }

            if ("root".equals(currentNodeId) || currentNodeId.startsWith("q")) {
                Map<String, Object> firstQuestion = getQuestionById(currentNodeId);
                if (firstQuestion.containsKey("error")) {
                    logger.warn("{} not found, using fallback", currentNodeId);
                    firstQuestion = getFallbackQuestion("q1");
                }
                return firstQuestion;
            }

            Map<String, Object> currentNode = findNodeInTree(currentNodeId, decisionTree);
            if (currentNode == null) {
                logger.warn("Node not found in decision tree: {}, using linear progression", currentNodeId);
                return getNextLinearQuestion(currentNodeId);
            }

            Object childrenObj = currentNode.get("children");
            if (childrenObj instanceof List) {
                List<Map<String, Object>> children = (List<Map<String, Object>>) childrenObj;

                for (Map<String, Object> child : children) {
                    Object conditionObj = child.get("condition");
                    Object actionObj = child.get("action");

                    String condition = conditionObj != null ? conditionObj.toString() : null;
                    String action = actionObj != null ? actionObj.toString() : null;

                    boolean matches = condition == null || evaluateCondition(condition, answers);
                    logger.debug("evaluateDecisionTree: node={}, child condition='{}' matches={} action={}", currentNodeId, condition, matches, action);

                    if (matches) {
                        Map<String, Object> result = handleAction(action, child, answers);
                        if (result != null && !result.containsKey("error")) {
                            logger.info("evaluateDecisionTree: node={}, taking action={}, returning result", currentNodeId, action);
                            return result;
                        }
                    }
                }
            }

            logger.info("No valid path found for node {}, using linear progression", currentNodeId);
            return getNextLinearQuestion(currentNodeId);

        } catch (Exception e) {
            logger.error("Error evaluating decision tree for node: {}", currentNodeId, e);
            return getNextLinearQuestion(currentNodeId);
        }
    }

    private Map<String, Object> findNodeInTree(String nodeId, Map<String, Object> tree) {
        if (nodeId == null || tree == null) return null;

        Object treeNodeId = tree.get("node_id");
        if (treeNodeId != null && nodeId.equals(treeNodeId.toString())) {
            return tree;
        }

        Object subtreesObj = tree.get("subtrees");
        if (subtreesObj instanceof Map) {
            Map<String, Object> subtrees = (Map<String, Object>) subtreesObj;
            if (subtrees.containsKey(nodeId)) {
                Object subtree = subtrees.get(nodeId);
                if (subtree instanceof Map) {
                    return (Map<String, Object>) subtree;
                }
            }

            for (Object subtreeObj : subtrees.values()) {
                if (subtreeObj instanceof Map) {
                    Map<String, Object> result = findNodeInTree(nodeId, (Map<String, Object>) subtreeObj);
                    if (result != null) {
                        return result;
                    }
                }
            }
        }

        return null;
    }

    private boolean evaluateCondition(String condition, Map<String, String> answers) {
        if (condition == null || condition.trim().isEmpty()) {
            return true;
        }

        try {
            return evaluateSimpleCondition(condition, answers);
        } catch (Exception e) {
            logger.error("Error evaluating condition: {} with answers {}", condition, answers, e);
            return false;
        }
    }

    /**
     * Improved simple condition evaluator:
     * - supports ==, !=, >, >=, <, <=
     * - supports boolean/null checks like 'age == null' or 'gender != null'
     * - supports && and || grouping (evaluated left-to-right)
     * - comparisons are case-insensitive for strings
     */
    private boolean evaluateSimpleCondition(String condition, Map<String, String> answers) {
        // normalize spacing to preserve string tokens e.g. 'condom_use == 'Always''
        String trimmed = condition.trim();

        // Handle parentheses by recursion (basic)
        if (trimmed.startsWith("(") && trimmed.endsWith(")")) {
            return evaluateSimpleCondition(trimmed.substring(1, trimmed.length() - 1), answers);
        }

        // handle top-level OR
        if (trimmed.contains("||")) {
            String[] parts = trimmed.split("\\|\\|");
            for (String p : parts) if (evaluateSimpleCondition(p.trim(), answers)) return true;
            return false;
        }

        // handle top-level AND
        if (trimmed.contains("&&")) {
            String[] parts = trimmed.split("&&");
            for (String p : parts) if (!evaluateSimpleCondition(p.trim(), answers)) return false;
            return true;
        }

        // equality / inequality
        if (trimmed.contains("==")) {
            String[] parts = trimmed.split("==", 2);
            String variable = parts[0].trim();
            String expectedRaw = parts[1].trim();
            String expected = stripQuotes(expectedRaw);
            if ("null".equalsIgnoreCase(expected)) {
                return answers.get(variable) == null || answers.get(variable).isBlank();
            }
            String actual = answers.get(variable);
            if (actual == null) return false;
            return expected.equalsIgnoreCase(actual.trim());
        }

        if (trimmed.contains("!=")) {
            String[] parts = trimmed.split("!=", 2);
            String variable = parts[0].trim();
            String expectedRaw = parts[1].trim();
            String expected = stripQuotes(expectedRaw);
            if ("null".equalsIgnoreCase(expected)) {
                return !(answers.get(variable) == null || answers.get(variable).isBlank());
            }
            String actual = answers.get(variable);
            if (actual == null) return true;
            return !expected.equalsIgnoreCase(actual.trim());
        }

        // numeric comparisons: variable <op> number
        Pattern numericPattern = Pattern.compile("([a-zA-Z_]+)\\s*([<>]=?)\\s*([0-9\\.]+)");
        var m = numericPattern.matcher(trimmed);
        if (m.matches()) {
            String variable = m.group(1);
            String operator = m.group(2);
            String numRaw = m.group(3);
            String actual = answers.get(variable);
            if (actual == null || !isNumeric(actual) || !isNumeric(numRaw)) return false;
            double actualNum = Double.parseDouble(actual);
            double expectedNum = Double.parseDouble(numRaw);
            return switch (operator) {
                case "<" -> actualNum < expectedNum;
                case "<=" -> actualNum <= expectedNum;
                case ">" -> actualNum > expectedNum;
                case ">=" -> actualNum >= expectedNum;
                default -> false;
            };
        }

        // handle existence check 'key' meaning truthy
        String actualVal = answers.get(trimmed);
        if (actualVal != null) {
            return "yes".equalsIgnoreCase(actualVal) || "true".equalsIgnoreCase(actualVal) || !actualVal.isBlank();
        }

        // no match: return false
        return false;
    }

    private String stripQuotes(String s) {
        return s.replaceAll("^['\"]|['\"]$", "").trim();
    }

    private boolean isNumeric(String str) {
        try {
            Double.parseDouble(str);
            return true;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    private Map<String, Object> handleAction(String action, Map<String, Object> actionNode, Map<String, String> answers) {
        if (action == null) {
            logger.warn("Action is null, using linear progression");
            return getNextLinearQuestion("q1");
        }

        switch (action) {
            case "end":
                Map<String, Object> riskAssessment = calculateRiskScoreWithML(answers);
                return Map.of(
                        "text", getEndMessage(actionNode),
                        "end", true,
                        "type", "end",
                        "riskScore", riskAssessment.get("riskScore"),
                        "riskLevel", riskAssessment.get("riskLevel"),
                        "recommendations", riskAssessment.get("recommendations"),
                        "hivProbability", riskAssessment.get("hivProbability"),
                        "confidence", riskAssessment.get("confidence"),
                        "modelUsed", riskAssessment.get("modelUsed")
                );

            case "goto":
                Object nextObj = actionNode.get("next");
                if (nextObj != null) {
                    String nextNodeId = nextObj.toString();
                    Map<String, Object> nextQuestion = getQuestionById(nextNodeId);
                    if (nextQuestion != null && !nextQuestion.containsKey("error")) {
                        return enhanceQuestionWithContext(nextQuestion, nextNodeId);
                    } else {
                        Map<String, Object> nextNode = findNodeInTree(nextNodeId, decisionTree);
                        if (nextNode != null) {
                            // continue evaluating down the tree
                            String nextForTraversal = traverseFromRootForNextNode(answers);
                            if (nextForTraversal != null) {
                                Map<String, Object> q = findNodeInTree(nextForTraversal, decisionTree);
                                if (q != null) {
                                    Object qKeyObj = q.get("question_key");
                                    if (qKeyObj != null) {
                                        String questionKey = qKeyObj.toString();
                                        if (!answers.containsKey(questionKey)) {
                                            return getQuestionByKeyOrFallback(questionKey, nextForTraversal);
                                        }
                                    }
                                }
                            }
                            return evaluateDecisionTree(nextNodeId, answers);
                        }
                    }
                }
                logger.warn("Goto target not found, falling back to linear progression");
                return getNextLinearQuestion("q1");

            case "ask":
                Object questionObj = actionNode.get("question");
                if (questionObj != null) {
                    String questionId = questionObj.toString();
                    Map<String, Object> question = getQuestionById(questionId);
                    if (question != null && !question.containsKey("error")) {
                        return enhanceQuestionWithContext(question, questionId);
                    }
                }
                return Map.of("text", "Question not found", "error", true);

            default:
                logger.warn("Unknown action: {}, using linear progression", action);
                return getNextLinearQuestion("q1");
        }
    }

    private String getEndMessage(Map<String, Object> actionNode) {
        Object messageObj = actionNode.get("message");
        if (messageObj != null) {
            return messageObj.toString();
        }

        Object conditionObj = actionNode.get("condition");
        if (conditionObj != null) {
            String condition = conditionObj.toString();
            if (condition.contains("consent == 'no'")) {
                return "Thank you. You have declined to participate.";
            }
        }

        return "Thank you for completing the STI risk assessment!";
    }

    private Map<String, Object> getNextLinearQuestion(String currentNodeId) {
        try {
            if ("root".equals(currentNodeId) || currentNodeId == null) {
                return getQuestionById("q1");
            }

            // For now, use simple linear progression based on available questions order in questionMap
            List<String> availableKeys = new ArrayList<>(questionMap.keySet());
            if (availableKeys.isEmpty()) {
                return getFallbackQuestion("q1");
            }

            // Find current position (map currentNodeId to a question key)
            String currentKey = null;
            for (Map.Entry<String, String> entry : questionKeyMapping.entrySet()) {
                if (entry.getKey().equals(currentNodeId)) {
                    currentKey = entry.getValue();
                    break;
                }
            }

            if (currentKey == null) {
                // if given a question key directly, use it
                if (availableKeys.contains(currentNodeId)) currentKey = currentNodeId;
                else currentKey = availableKeys.get(0);
            }

            int currentIndex = availableKeys.indexOf(currentKey);
            if (currentIndex >= 0 && currentIndex < availableKeys.size() - 1) {
                String nextKey = availableKeys.get(currentIndex + 1);
                // Map back to decision tree ID if possible
                for (Map.Entry<String, String> entry : questionKeyMapping.entrySet()) {
                    if (entry.getValue().equals(nextKey)) {
                        return getQuestionById(entry.getKey());
                    }
                }
                // If no mapping found, use the database key directly
                Question nextQuestion = questionMap.get(nextKey);
                if (nextQuestion != null) {
                    return convertQuestionToMap(nextQuestion, nextKey);
                }
            }

            // If we've reached the end
            return Map.of(
                    "text", "Thank you for completing the survey!",
                    "end", true,
                    "type", "completion"
            );

        } catch (Exception e) {
            logger.error("Error in linear progression from: {}", currentNodeId, e);
            return getQuestionById("q1");
        }
    }

    private Map<String, Object> enhanceQuestionWithContext(Map<String, Object> question, String questionId) {
        Map<String, Object> enhanced = new HashMap<>(question);

        enhanced.put("id", questionId);

        // Calculate progress based on question order
        int questionIndex = calculateQuestionIndex(questionId);
        int totalQuestions = getTotalQuestionCount();
        int progress = totalQuestions > 0 ? (int) ((questionIndex / (double) totalQuestions) * 100) : 0;

        enhanced.put("questionIndex", questionIndex);
        enhanced.put("totalQuestions", totalQuestions);
        enhanced.put("progress", progress);

        // Ensure proper type handling
        Object typeObj = question.get("type");
        if (typeObj != null) {
            String questionType = typeObj.toString();
            if ("yes_no".equals(questionType) || "yes-no".equals(questionType)) {
                enhanced.put("options", Arrays.asList("Yes", "No"));
            }
        }

        return enhanced;
    }

    private int calculateQuestionIndex(String questionId) {
        // Use the mapping to determine order - try mapping then direct key
        String mappedKey = questionKeyMapping.get(questionId);
        List<String> orderedKeys = new ArrayList<>(questionMap.keySet());
        if (mappedKey != null) {
            int index = orderedKeys.indexOf(mappedKey);
            return index >= 0 ? index + 1 : 1;
        } else {
            int index = orderedKeys.indexOf(questionId);
            return index >= 0 ? index + 1 : 1;
        }
    }

    /**
     * OLD METHOD: Keep for backward compatibility but now uses ML-enhanced version
     */
    public Map<String, Object> calculateRiskScore(Map<String, String> answers) {
        return calculateRiskScoreWithML(answers);
    }

    // Helper methods
    public int getTotalQuestionCount() {
        return questionMap.size();
    }

    public List<String> getAllSectionTitles() {
        try {
            return questionRepository.findDistinctSectionTitles();
        } catch (Exception e) {
            logger.error("Error getting section titles", e);
            return new ArrayList<>();
        }
    }

    public List<Question> getQuestionsBySection(String sectionTitle) {
        try {
            return questionRepository.findBySectionTitleAndIsActiveTrueOrderByDisplayOrderAsc(sectionTitle);
        } catch (Exception e) {
            logger.error("Error getting questions by section: {}", sectionTitle, e);
            return new ArrayList<>();
        }
    }

    public boolean questionExists(String questionKey) {
        return questionMap.containsKey(questionKey);
    }

    public int getSectionCount() {
        try {
            return questionRepository.findDistinctSectionTitles().size();
        } catch (Exception e) {
            logger.error("Error getting section count", e);
            return 0;
        }
    }

    public boolean isInitialized() {
        return !questionMap.isEmpty() && !decisionTree.isEmpty();
    }

    // Fallback and error handling
    private Map<String, Object> getFallbackQuestion(String questionId) {
        Map<String, Object> fallbackQuestions = createFallbackQuestions();
        Map<String, Object> question = (Map<String, Object>) fallbackQuestions.get(questionId);
        if (question != null) {
            return enhanceQuestionWithContext(question, questionId);
        }
        return createErrorQuestion("Fallback question not found: " + questionId);
    }

    private Map<String, Object> createFallbackQuestions() {
        Map<String, Object> questions = new HashMap<>();

        // Consent question (q1)
        questions.put("q1", Map.of(
                "id", "q1",
                "key", "consent",
                "text", "Welcome to the STI Risk Assessment",
                "description", "This assessment will help evaluate your risk for sexually transmitted infections. Your responses are confidential. Do you consent to participate?",
                "type", "yes_no",
                "sectionTitle", "Consent",
                "options", Arrays.asList("Yes", "No")
        ));

        return questions;
    }

    private Map<String, Object> createErrorQuestion(String errorMessage) {
        return Map.of(
                "error", errorMessage,
                "text", "System temporarily unavailable",
                "description", "Please try again later.",
                "type", "multiple_choice",
                "key", "error",
                "options", Arrays.asList("Retry", "Contact Support"),
                "sectionTitle", "System Error"
        );
    }

    private Map<String, Object> loadJsonResource(ObjectMapper mapper, String filename) {
        Map<String, Object> result = new HashMap<>();
        String resourcePath = filename.startsWith("/") ? filename : "/" + filename;

        try {
            ClassPathResource resource = new ClassPathResource(resourcePath);
            if (!resource.exists()) {
                logger.warn("Resource file not found: {}", resourcePath);
                return result;
            }

            try (InputStream inputStream = resource.getInputStream()) {
                result = mapper.readValue(inputStream, new TypeReference<Map<String, Object>>() {});
                logger.info("Successfully loaded: {}", filename);
            }
        } catch (Exception e) {
            logger.error("Error loading resource: {}", filename, e);
        }

        return result;
    }

    // Decision tree validation: ensure referenced question_keys and next nodes exist
    private void validateDecisionTreeAgainstQuestions() {
        try {
            if (decisionTree == null || decisionTree.isEmpty()) return;

            List<String> missingQuestionKeys = new ArrayList<>();
            List<String> missingNextNodes = new ArrayList<>();

            validateNodeRecursively(decisionTree, missingQuestionKeys, missingNextNodes);

            if (!missingQuestionKeys.isEmpty()) {
                logger.warn("Decision tree validation found nodes referencing unknown question_keys: {}", missingQuestionKeys);
            }
            if (!missingNextNodes.isEmpty()) {
                logger.warn("Decision tree validation found children referencing unknown 'next' node_ids: {}", missingNextNodes);
            }
            if (missingQuestionKeys.isEmpty() && missingNextNodes.isEmpty()) {
                logger.info("Decision tree validation passed: All referenced question_keys and next nodes exist (or will fallback).");
            }
        } catch (Exception e) {
            logger.error("Error validating decision tree", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void validateNodeRecursively(Map<String, Object> node, List<String> missingQuestionKeys, List<String> missingNextNodes) {
        if (node == null) return;
        Object qKey = node.get("question_key");
        if (qKey != null) {
            String key = qKey.toString();
            if (!questionMap.containsKey(key) && !key.trim().isEmpty()) {
                missingQuestionKeys.add(key);
            }
        }

        Object childrenObj = node.get("children");
        if (childrenObj instanceof List) {
            for (Map<String, Object> child : (List<Map<String, Object>>) childrenObj) {
                Object next = child.get("next");
                if (next != null) {
                    String nextNodeId = next.toString();
                    Map<String, Object> nextNode = findNodeInTree(nextNodeId, decisionTree);
                    if (nextNode == null) missingNextNodes.add(nextNodeId);
                }
            }
        }

        Object subtreesObj = node.get("subtrees");
        if (subtreesObj instanceof Map) {
            for (Object subtree : ((Map<String, Object>) subtreesObj).values()) {
                if (subtree instanceof Map) {
                    validateNodeRecursively((Map<String, Object>) subtree, missingQuestionKeys, missingNextNodes);
                }
            }
        }
    }

    // Debug methods
    public Map<String, Object> debugDatabaseStatus() {
        Map<String, Object> status = new HashMap<>();

        try {
            long dbCount = questionRepository.count();
            List<Question> allQuestions = questionRepository.findByIsActiveTrueOrderByDisplayOrderAsc();
            List<String> dbKeys = allQuestions.stream()
                    .map(Question::getQuestionKey)
                    .collect(Collectors.toList());

            status.put("databaseTotalCount", dbCount);
            status.put("databaseActiveKeys", dbKeys);
            status.put("questionMapSize", questionMap.size());
            status.put("questionMapKeys", new ArrayList<>(questionMap.keySet()));
            status.put("mappingSize", questionKeyMapping.size());
            status.put("decisionTreeLoaded", !decisionTree.isEmpty());
            status.put("mlServiceAvailable", mlService.isServiceHealthy());

            // Test q1 lookup
            Map<String, Object> q1Test = getQuestionById("q1");
            status.put("q1LookupSuccess", !q1Test.containsKey("error"));
            status.put("q1ResultKeys", q1Test.keySet());

        } catch (Exception e) {
            status.put("error", e.getMessage());
        }

        return status;
    }

    public Map<String, Object> debugQuestionLookup(String questionId) {
        logger.info("=== DEBUG QUESTION LOOKUP: {} ===", questionId);

        // Check mapping
        String mappedKey = questionKeyMapping.get(questionId);
        logger.info("Mapped key for {}: {}", questionId, mappedKey);

        // Check if mapped key exists in questionMap
        if (mappedKey != null) {
            Question question = questionMap.get(mappedKey);
            logger.info("Question found for mapped key '{}': {}", mappedKey, question != null);
            if (question != null) {
                logger.info("Question details - Text: {}, Type: {}", question.getQuestionText(), question.getQuestionType());
            }
        }

        // Try direct lookup
        Question directQuestion = questionMap.get(questionId);
        logger.info("Direct lookup for '{}': {}", questionId, directQuestion != null);

        // Show all available keys for debugging
        logger.info("All available question keys: {}", questionMap.keySet());

        Map<String, Object> result = getQuestionById(questionId);
        logger.info("Final result for {}: {}", questionId, result.keySet());
        logger.info("Has error: {}", result.containsKey("error"));

        logger.info("=== END DEBUG ===");

        return Map.of(
                "questionId", questionId,
                "mappedKey", mappedKey,
                "mappedKeyExists", mappedKey != null && questionMap.containsKey(mappedKey),
                "directLookup", directQuestion != null,
                "resultKeys", result.keySet(),
                "hasError", result.containsKey("error"),
                "availableKeys", new ArrayList<>(questionMap.keySet())
        );
    }

    public Map<String, Object> debugConsentFlow(Map<String, String> answers) {
        Map<String, Object> debugInfo = new HashMap<>();
        String consentAnswer = answers.get("consent");
        debugInfo.put("consentAnswer", consentAnswer);
        debugInfo.put("answers", answers);
        boolean consentNoCondition = evaluateCondition("consent == 'no'", answers);
        boolean consentYesCondition = evaluateCondition("consent == 'yes'", answers);
        debugInfo.put("consentNoCondition", consentNoCondition);
        debugInfo.put("consentYesCondition", consentYesCondition);
        String currentNodeId = traverseFromRootForNextNode(answers);
        Map<String, Object> currentNode = findNodeInTree(currentNodeId, decisionTree);
        debugInfo.put("currentNodeId", currentNodeId);
        debugInfo.put("currentNode", currentNode);
        return debugInfo;
    }

    public Map<String, Object> getDecisionTreeStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("totalNodes", countNodes(decisionTree));
        status.put("totalQuestions", questionMap.size());
        status.put("questionKeyMappingSize", questionKeyMapping.size());
        status.put("decisionTreeLoaded", !decisionTree.isEmpty());
        status.put("questionsLoaded", !questionMap.isEmpty());
        status.put("mlServiceHealthy", mlService.isServiceHealthy());
        status.put("questionKeyMapping", questionKeyMapping);
        status.put("mlFieldMapping", mlFieldMapping);
        return status;
    }

    private int countNodes(Map<String, Object> tree) {
        int count = 1;
        Object subtreesObj = tree.get("subtrees");
        if (subtreesObj instanceof Map) {
            Map<String, Object> subtrees = (Map<String, Object>) subtreesObj;
            for (Object subtree : subtrees.values()) {
                if (subtree instanceof Map) {
                    count += countNodes((Map<String, Object>) subtree);
                }
            }
        }
        return count;
    }
}

