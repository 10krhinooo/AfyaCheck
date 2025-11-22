package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface QuestionRepository extends JpaRepository<Question, Long> {

    // Basic question key queries
    Optional<Question> findByQuestionKey(String questionKey);

    // Add this missing method
    Optional<Question> findByQuestionKeyAndIsActiveTrue(String questionKey);

    boolean existsByQuestionKey(String questionKey);

    // Active questions
    List<Question> findByIsActiveTrueOrderByDisplayOrderAsc();
    List<Question> findByIsActiveTrue();
    Long countByIsActiveTrue();

    // Section-based queries
    List<Question> findBySectionTitleAndIsActiveTrueOrderByDisplayOrderAsc(String sectionTitle);
    List<Question> findBySectionTitleOrderByDisplayOrderAsc(String sectionTitle);

    // Custom query for active questions by section
    @Query("SELECT q FROM Question q WHERE q.sectionTitle = :sectionTitle AND q.isActive = true ORDER BY q.displayOrder")
    List<Question> findActiveQuestionsBySection(@Param("sectionTitle") String sectionTitle);

    // Distinct sections
    @Query("SELECT DISTINCT q.sectionTitle FROM Question q WHERE q.isActive = true ORDER BY q.sectionTitle")
    List<String> findDistinctSectionTitles();

    // Type-based queries
    List<Question> findByQuestionTypeAndIsActiveTrue(String questionType);
    List<Question> findByQuestionTypeOrderByDisplayOrderAsc(String questionType);

    // Multiple keys query
    @Query("SELECT q FROM Question q WHERE q.questionKey IN :keys AND q.isActive = true ORDER BY q.displayOrder")
    List<Question> findByQuestionKeyInAndIsActiveTrue(@Param("keys") List<String> keys);

    // Find by display order range
    List<Question> findByDisplayOrderBetweenAndIsActiveTrue(Integer start, Integer end);

    // Find questions with specific types in a section
    @Query("SELECT q FROM Question q WHERE q.sectionTitle = :sectionTitle AND q.questionType = :questionType AND q.isActive = true ORDER BY q.displayOrder")
    List<Question> findBySectionTitleAndQuestionTypeAndIsActiveTrue(@Param("sectionTitle") String sectionTitle,
                                                                    @Param("questionType") String questionType);
}