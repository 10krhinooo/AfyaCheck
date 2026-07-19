package com.kimanga.afyacheck.repository;

import com.kimanga.afyacheck.model.RetestReminder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.List;

@Repository
public interface RetestReminderRepository extends JpaRepository<RetestReminder, Long> {

    List<RetestReminder> findByDueAtBefore(Date cutoff);
}
