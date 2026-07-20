package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.Answer;
import com.kimanga.afyacheck.model.Session;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AnswerRepository extends JpaRepository<Answer, Long> {

    List<Answer> findBySessionOrderByCreatedAtAsc(Session session);

    Optional<Answer> findTopBySessionOrderByCreatedAtDesc(Session session);

    // Find answer by session and question key
    @Query("SELECT a FROM Answer a WHERE a.session = :session AND a.questionKey = :questionKey")
    Optional<Answer> findBySessionAndQuestionKey(@Param("session") Session session, @Param("questionKey") String questionKey);

    // Find all answers for a session ID
    @Query("SELECT a FROM Answer a WHERE a.session.sessionId = :sessionId ORDER BY a.createdAt ASC")
    List<Answer> findBySessionIdOrderByCreatedAtAsc(@Param("sessionId") String sessionId);

    // Count answers for a session
    Long countBySession(Session session);

    // Delete all answers for a session
    void deleteBySession(Session session);

    // Bulk retention purge (DataRetentionService) — must run before the Session bulk delete.
    @org.springframework.data.jpa.repository.Modifying
    @Query("DELETE FROM Answer a WHERE a.session IN (SELECT s FROM Session s WHERE s.createdAt < :cutoff)")
    int deleteBySessionCreatedAtBefore(@Param("cutoff") java.util.Date cutoff);

    // Find answers by question key pattern
    @Query("SELECT a FROM Answer a WHERE a.questionKey LIKE %:pattern%")
    List<Answer> findByQuestionKeyContaining(@Param("pattern") String pattern);
}