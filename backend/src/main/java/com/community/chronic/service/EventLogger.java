package com.community.chronic.service;

import com.community.chronic.model.ChronicRecord;
import com.community.chronic.model.Enums;
import com.community.chronic.model.RecordEvent;
import com.community.chronic.repo.RecordEventRepo;
import org.springframework.stereotype.Service;

/** 事件流记录：所有关键变化回流到同一慢病档案。 */
@Service
public class EventLogger {

    private final RecordEventRepo eventRepo;

    public EventLogger(RecordEventRepo eventRepo) {
        this.eventRepo = eventRepo;
    }

    public void log(ChronicRecord record, Enums.EventType type, String detail, String createdBy) {
        RecordEvent e = new RecordEvent();
        e.record = record;
        e.eventType = type;
        e.detail = detail;
        e.createdBy = createdBy;
        eventRepo.save(e);
    }
}
