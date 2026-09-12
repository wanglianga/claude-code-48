package com.community.chronic.service;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 规则引擎：
 * 1. 上传即评估——血压/血糖达标判断、连续漏服、低血糖、不良反应；
 * 2. 定时巡检——长期不上传 → 告警 → 失访 → 重点名单；
 * 3. 分层建议——按家庭支持程度、依从性、异常频率给出管理级别建议。
 */
@Service
public class RuleEngine {

    /** 连续漏服判定窗口（天）与次数 */
    static final int MISSED_MED_WINDOW_DAYS = 3;
    static final int MISSED_MED_THRESHOLD = 3;
    /** 连续血压超标次数 */
    static final int BP_OVER_THRESHOLD = 3;
    /** 严重高血压阈值 */
    static final int BP_CRISIS_SYS = 180;
    static final int BP_CRISIS_DIA = 110;
    /** 低血糖阈值 mmol/L */
    static final double GLUCOSE_LOW = 3.9;
    /** 不上传告警天数 / 失访天数 */
    static final int NO_UPLOAD_WARN_DAYS = 7;
    static final int LOST_DAYS = 14;

    private final HealthUploadRepo uploadRepo;
    private final AlertRepo alertRepo;
    private final ChronicRecordRepo recordRepo;
    private final MedicationRepo medicationRepo;
    private final FamilyContactRepo contactRepo;
    private final EventLogger eventLogger;

    public RuleEngine(HealthUploadRepo uploadRepo, AlertRepo alertRepo, ChronicRecordRepo recordRepo,
                      MedicationRepo medicationRepo, FamilyContactRepo contactRepo, EventLogger eventLogger) {
        this.uploadRepo = uploadRepo;
        this.alertRepo = alertRepo;
        this.recordRepo = recordRepo;
        this.medicationRepo = medicationRepo;
        this.contactRepo = contactRepo;
        this.eventLogger = eventLogger;
    }

    // ---------------- 上传即评估 ----------------

    @Transactional
    public void onUpload(HealthUpload u) {
        ChronicRecord r = u.record;
        switch (u.type) {
            case BP -> evaluateBp(r, u);
            case GLUCOSE -> evaluateGlucose(r, u);
            case MEDICATION -> evaluateMedication(r, u);
            default -> { /* 饮食/运动仅记录 */ }
        }
    }

    private void evaluateBp(ChronicRecord r, HealthUpload u) {
        if (u.sys == null || u.dia == null) return;
        if (u.sys >= BP_CRISIS_SYS || u.dia >= BP_CRISIS_DIA) {
            raiseAlert(r, Enums.AlertType.BP_HIGH, Enums.AlertLevel.CRITICAL,
                    String.format("血压严重升高 %d/%d mmHg（≥%d/%d 危急值），请立即处置",
                            u.sys, u.dia, BP_CRISIS_SYS, BP_CRISIS_DIA));
            return;
        }
        boolean over = u.sys > r.targetSys || u.dia > r.targetDia;
        if (over) {
            List<HealthUpload> recent = uploadRepo.findTop3ByRecordIdAndTypeOrderByMeasuredAtDesc(
                    r.id, Enums.UploadType.BP);
            long overCount = recent.stream()
                    .filter(x -> x.sys != null && x.dia != null && (x.sys > r.targetSys || x.dia > r.targetDia))
                    .count();
            if (recent.size() >= BP_OVER_THRESHOLD && overCount >= BP_OVER_THRESHOLD) {
                raiseAlert(r, Enums.AlertType.BP_HIGH, Enums.AlertLevel.WARN,
                        String.format("连续 %d 次家庭血压超过目标值（%d/%d，目标 <%d/%d mmHg）",
                                BP_OVER_THRESHOLD, u.sys, u.dia, r.targetSys, r.targetDia));
            }
        }
    }

    private void evaluateGlucose(ChronicRecord r, HealthUpload u) {
        if (u.glucose == null) return;
        if (u.glucose < GLUCOSE_LOW) {
            raiseAlert(r, Enums.AlertType.GLUCOSE_LOW, Enums.AlertLevel.CRITICAL,
                    String.format("低血糖 %.1f mmol/L（<%.1f），请立即确认居民状态", u.glucose, GLUCOSE_LOW));
        }
    }

    private void evaluateMedication(ChronicRecord r, HealthUpload u) {
        if (u.medStatus == Enums.MedLogStatus.ADVERSE) {
            raiseAlert(r, Enums.AlertType.ADVERSE_REACTION, Enums.AlertLevel.CRITICAL,
                    "上报药物不良反应：" + (u.medicationName != null ? u.medicationName : "未指明药物")
                            + (u.note != null ? "，" + u.note : ""));
            return;
        }
        if (u.medStatus == Enums.MedLogStatus.MISSED) {
            long missed = uploadRepo.countByRecordIdAndTypeAndMedStatusAndMeasuredAtAfter(
                    r.id, Enums.UploadType.MEDICATION, Enums.MedLogStatus.MISSED,
                    LocalDateTime.now().minusDays(MISSED_MED_WINDOW_DAYS));
            if (missed >= MISSED_MED_THRESHOLD) {
                raiseAlert(r, Enums.AlertType.MISSED_MED, Enums.AlertLevel.WARN,
                        String.format("近 %d 天漏服药物 %d 次，用药依从性差", MISSED_MED_WINDOW_DAYS, missed));
            }
        }
    }

    /** 同类型同级别未处理告警不重复创建；危急值（CRITICAL）不受已有 WARN 影响。 */
    private void raiseAlert(ChronicRecord r, Enums.AlertType type, Enums.AlertLevel level, String message) {
        if (alertRepo.existsByRecordIdAndAlertTypeAndLevelAndStatus(r.id, type, level, Enums.AlertStatus.OPEN)) {
            return;
        }
        Alert a = new Alert();
        a.record = r;
        a.alertType = type;
        a.level = level;
        a.message = message;
        alertRepo.save(a);
        eventLogger.log(r, Enums.EventType.ALERT, "【" + levelLabel(level) + "】" + message, "系统");
    }

    static String levelLabel(Enums.AlertLevel l) {
        return switch (l) {
            case CRITICAL -> "紧急";
            case WARN -> "警告";
            default -> "提示";
        };
    }

    // ---------------- 定时巡检：长期不上传 → 失访 → 重点名单 ----------------

    @Scheduled(fixedDelay = 3600_000, initialDelay = 60_000)
    @Transactional
    public void checkNoUpload() {
        LocalDateTime now = LocalDateTime.now();
        for (ChronicRecord r : recordRepo.findByStatus(Enums.RecordStatus.ACTIVE)) {
            LocalDateTime last = uploadRepo.findFirstByRecordIdOrderByMeasuredAtDesc(r.id)
                    .map(u -> u.measuredAt)
                    .orElse(r.createdAt);
            if (last.isBefore(now.minusDays(LOST_DAYS))) {
                markLost(r, "超过 " + LOST_DAYS + " 天未上传家庭监测数据，判定失访");
            } else if (last.isBefore(now.minusDays(NO_UPLOAD_WARN_DAYS))) {
                raiseAlert(r, Enums.AlertType.NO_UPLOAD, Enums.AlertLevel.WARN,
                        "超过 " + NO_UPLOAD_WARN_DAYS + " 天未上传家庭监测数据，请家属/护士督促");
            }
        }
    }

    @Transactional
    public void markLost(ChronicRecord r, String reason) {
        if (r.status == Enums.RecordStatus.LOST) return;
        r.status = Enums.RecordStatus.LOST;
        r.manageLevel = Enums.ManageLevel.KEY_FOCUS; // 失访居民进入重点名单
        recordRepo.save(r);
        eventLogger.log(r, Enums.EventType.LOST, reason + "，已转入重点慢病名单", "系统");
        raiseAlert(r, Enums.AlertType.NO_UPLOAD, Enums.AlertLevel.CRITICAL, reason + "，居民已转入重点慢病名单");
    }

    // ---------------- 分层建议 ----------------

    /**
     * 依据家庭支持程度、用药依从性、达标情况、告警频率给出分层建议，
     * 帮助社区医生判断：家属代管 / 上门随访 / 重点慢病名单。
     */
    public Map<String, Object> suggestLevel(ChronicRecord r) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        long taken = uploadRepo.countByRecordIdAndTypeAndMedStatusAndMeasuredAtAfter(
                r.id, Enums.UploadType.MEDICATION, Enums.MedLogStatus.TAKEN, since);
        long missed = uploadRepo.countByRecordIdAndTypeAndMedStatusAndMeasuredAtAfter(
                r.id, Enums.UploadType.MEDICATION, Enums.MedLogStatus.MISSED, since);
        long uploads = uploadRepo.findByRecordIdAndMeasuredAtAfterOrderByMeasuredAtDesc(r.id, since).size();
        long alerts = alertRepo.countByRecordIdAndCreatedAtAfter(r.id, since);
        boolean hasProxy = contactRepo.findByRecordId(r.id).stream().anyMatch(c -> c.proxy);

        double adherence = (taken + missed) == 0 ? 1.0 : (double) taken / (taken + missed);

        int score = 0;
        if (r.familySupport == Enums.FamilySupport.WEAK) score += 2;
        else if (r.familySupport == Enums.FamilySupport.MODERATE) score += 1;
        if (adherence < 0.6) score += 2;
        else if (adherence < 0.8) score += 1;
        if (alerts >= 5) score += 2;
        else if (alerts >= 2) score += 1;
        if (!hasProxy && r.familySupport != Enums.FamilySupport.STRONG) score += 1;
        if (uploads < 8) score += 1; // 月均上传过少

        Enums.ManageLevel suggested;
        String reason;
        if (r.status == Enums.RecordStatus.LOST) {
            suggested = Enums.ManageLevel.KEY_FOCUS;
            reason = "居民处于失访状态，应列入重点慢病名单并尽快找回";
        } else if (score >= 5) {
            suggested = Enums.ManageLevel.KEY_FOCUS;
            reason = "家庭支持弱/依从性差/异常频繁，建议纳入重点慢病名单";
        } else if (score >= 3) {
            suggested = Enums.ManageLevel.HOME_VISIT;
            reason = "存在一定管理缺口，建议安排上门随访";
        } else if (hasProxy) {
            suggested = Enums.ManageLevel.FAMILY_PROXY;
            reason = "已有家属代管且风险可控，建议维持家属代管";
        } else {
            suggested = Enums.ManageLevel.NORMAL;
            reason = "指标平稳、依从性良好，常规管理即可";
        }

        Map<String, Object> m = new HashMap<>();
        m.put("suggestedLevel", suggested);
        m.put("reason", reason);
        m.put("score", score);
        m.put("adherence30d", Math.round(adherence * 100));
        m.put("alerts30d", alerts);
        m.put("uploads30d", uploads);
        m.put("hasProxy", hasProxy);
        return m;
    }
}
