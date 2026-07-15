//package com.kimanga.afyacheck.repository;
//
//import com.kimanga.afyacheck.model.Questionnaire;
//import org.springframework.data.jpa.repository.JpaRepository;
//import org.springframework.data.jpa.repository.Query;
//import org.springframework.data.repository.query.Param;
//import org.springframework.stereotype.Repository;
//
//import java.util.List;
//
//@Repository
//public interface QuestionnaireRepository extends JpaRepository<Questionnaire, Long> {
//
//    // Count questionnaires by user ID
//    @Query("SELECT COUNT(q) FROM Questionnaire q WHERE q.user.id = :userId")
//    Long countByUserId(@Param("userId") Long userId);
//
//    // Count questionnaires by risk level
//    @Query("SELECT COUNT(q) FROM Questionnaire q WHERE q.riskLevel = :riskLevel")
//    Long countByRiskLevel(@Param("riskLevel") String riskLevel);
//
//    // Count questionnaires by user ID and risk level
//    @Query("SELECT COUNT(q) FROM Questionnaire q WHERE q.user.id = :userId AND q.riskLevel = :riskLevel")
//    Long countByUserIdAndRiskLevel(@Param("userId") Long userId, @Param("riskLevel") String riskLevel);
//
//    // Find questionnaires by user ID
//    List<Questionnaire> findByUserId(Long userId);
//
//    // Find recent questionnaires
//    List<Questionnaire> findTop10ByOrderByCreatedAtDesc();
//}