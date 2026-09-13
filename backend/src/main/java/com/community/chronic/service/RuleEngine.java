package com.community.chronic.service;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    private final BpWarningRepo warningRepo;
    private final FollowUpPlanRepo planRepo;
    private final EventLogger eventLogger;

    public RuleEngine(HealthUploadRepo uploadRepo, AlertRepo alertRepo, ChronicRecordRepo recordRepo,
                      MedicationRepo medicationRepo, FamilyContactRepo contactRepo, EventLogger eventLogger,
                      BpWarningRepo warningRepo, FollowUpPlanRepo planRepo) {
        this.uploadRepo = uploadRepo;
        this.alertRepo = alertRepo;
        this.recordRepo = recordRepo;
        this.medicationRepo = medicationRepo;
        this.contactRepo = contactRepo;
        this.eventLogger = eventLogger;
        this.warningRepo = warningRepo;
        this.planRepo = planRepo;
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
            raiseBpWarning(r, Enums.AlertLevel.CRITICAL, 1, u.sys, u.dia,
                    String.format("血压达危急值 %d/%d mmHg，已生成连续高血压预警", u.sys, u.dia));
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
                int maxSys = recent.stream().mapToInt(x -> x.sys).max().orElse(u.sys);
                int maxDia = recent.stream().mapToInt(x -> x.dia).max().orElse(u.dia);
                raiseBpWarning(r, Enums.AlertLevel.WARN, BP_OVER_THRESHOLD, maxSys, maxDia,
                        String.format("连续 %d 次家庭血压超标（最高 %d/%d mmHg，目标 <%d/%d），"
                                + "请居民/家属补充测量时间、服药、症状与就医情况",
                                BP_OVER_THRESHOLD, maxSys, maxDia, r.targetSys, r.targetDia));
            }
        }
    }

    /**
     * 连续高血压预警：存在未办结预警时不重复创建；
     * 但已有 WARN 预警期间出现危急值时，将现有预警升级为 CRITICAL。
     * 生成预警的同时自动生成电话随访任务（提前到明天）。
     */
    private void raiseBpWarning(ChronicRecord r, Enums.AlertLevel level, int triggerCount,
                                int maxSys, int maxDia, String message) {
        BpWarning existing = warningRepo.findFirstByRecordIdAndStatusInOrderByCreatedAtDesc(r.id,
                List.of(Enums.WarningStatus.OPEN, Enums.WarningStatus.NURSE_CONFIRMED)).orElse(null);
        if (existing != null) {
            if (level == Enums.AlertLevel.CRITICAL && existing.level != Enums.AlertLevel.CRITICAL) {
                existing.level = Enums.AlertLevel.CRITICAL;
                existing.message = message;
                existing.maxSys = maxSys;
                existing.maxDia = maxDia;
                warningRepo.save(existing);
                eventLogger.log(r, Enums.EventType.ALERT, "【连续高血压预警·升级】" + message, "系统");
            }
            return;
        }
        BpWarning w = new BpWarning();
        w.record = r;
        w.level = level;
        w.triggerCount = triggerCount;
        w.maxSys = maxSys;
        w.maxDia = maxDia;
        w.message = message;
        warningRepo.save(w);
        eventLogger.log(r, Enums.EventType.ALERT, "【连续高血压预警】" + message + "，已生成电话随访任务", "系统");
        createPhoneFollowUpTask(r);
    }

    /** 高血压预警生成电话随访任务：已有计划则提前到明天，没有则新建电话随访计划。 */
    private void createPhoneFollowUpTask(ChronicRecord r) {
        java.time.LocalDate tomorrow = java.time.LocalDate.now().plusDays(1);
        FollowUpPlan p = planRepo.findFirstByRecordIdAndActiveTrue(r.id).orElse(null);
        if (p == null) {
            p = new FollowUpPlan();
            p.record = r;
            p.planType = Enums.PlanType.PHONE;
            p.intervalDays = 7;
            p.nextDueDate = tomorrow;
            planRepo.save(p);
        } else if (p.nextDueDate == null || p.nextDueDate.isAfter(tomorrow)) {
            p.planType = Enums.PlanType.PHONE;
            p.nextDueDate = tomorrow;
            planRepo.save(p);
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

    // ---------------- 异常归因：疾病变化 / 用药依从性 / 家属照护缺口 ----------------

    /**
     * 对近 30 天数据做归因判断，三类结论均附可核验依据（具体次数/比率/告警条数），
     * 帮助社区医生分辨异常来自疾病变化、用药依从性还是家属照护缺口。
     */
    public List<Map<String, Object>> analyzeCauses(ChronicRecord r) {
        LocalDateTime since = LocalDateTime.now().minusDays(30);
        List<HealthUpload> recent = uploadRepo.findByRecordIdAndMeasuredAtAfterOrderByMeasuredAtDesc(r.id, since);

        long taken = recent.stream().filter(u -> u.type == Enums.UploadType.MEDICATION
                && u.medStatus == Enums.MedLogStatus.TAKEN).count();
        long missed = recent.stream().filter(u -> u.type == Enums.UploadType.MEDICATION
                && u.medStatus == Enums.MedLogStatus.MISSED).count();
        long medLogs = taken + missed;
        double adherence = medLogs == 0 ? 1.0 : (double) taken / medLogs;
        int adherencePct = (int) Math.round(adherence * 100);

        List<HealthUpload> bps = recent.stream()
                .filter(u -> u.type == Enums.UploadType.BP && u.sys != null && u.dia != null).toList();
        long bpOver = bps.stream().filter(u -> u.sys > r.targetSys || u.dia > r.targetDia).count();
        long crisis = bps.stream().filter(u -> u.sys >= BP_CRISIS_SYS || u.dia >= BP_CRISIS_DIA).count();
        long lowGlu = recent.stream().filter(u -> u.type == Enums.UploadType.GLUCOSE
                && u.glucose != null && u.glucose < GLUCOSE_LOW).count();
        long familyUploads = recent.stream().filter(u -> u.uploaderType == Enums.UploaderType.FAMILY).count();

        boolean hasProxy = contactRepo.findByRecordId(r.id).stream().anyMatch(c -> c.proxy);
        long missedMedAlerts = alertRepo.countByRecordIdAndAlertTypeAndCreatedAtAfter(
                r.id, Enums.AlertType.MISSED_MED, since);
        long noUploadAlerts = alertRepo.countByRecordIdAndAlertTypeAndCreatedAtAfter(
                r.id, Enums.AlertType.NO_UPLOAD, since);

        List<Map<String, Object>> causes = new ArrayList<>();

        // 1) 疾病变化：依从性良好、监测规律的前提下指标仍失控，才归因于疾病本身
        List<String> diseaseEv = new ArrayList<>();
        boolean adherencePoor = medLogs >= 3 && adherence < 0.8;
        boolean disease = !adherencePoor && (bpOver >= 3 || crisis > 0 || lowGlu > 0);
        if (adherencePoor) {
            diseaseEv.add(String.format("用药依从性仅 %d%%，指标异常更可能源于漏服药物，需先纠正依从性再评估疾病变化", adherencePct));
        }
        diseaseEv.add(bpOver > 0
                ? String.format("近30天家庭血压超标 %d/%d 次（目标 <%d/%d mmHg）", bpOver, bps.size(), r.targetSys, r.targetDia)
                : String.format("近30天家庭血压超标 0/%d 次（目标 <%d/%d mmHg）", bps.size(), r.targetSys, r.targetDia));
        if (crisis > 0) diseaseEv.add(String.format("出现危急值 %d 次（≥%d/%d mmHg）", crisis, BP_CRISIS_SYS, BP_CRISIS_DIA));
        if (lowGlu > 0) diseaseEv.add(String.format("低血糖 %d 次（<%.1f mmol/L）", lowGlu, GLUCOSE_LOW));
        if (medLogs >= 3) {
            diseaseEv.add(String.format("同期按时服药率 %d%%，%s", adherencePct,
                    adherence >= 0.8 ? "可排除漏服因素" : "无法排除漏服因素"));
        }
        if (bps.isEmpty() && lowGlu == 0) diseaseEv.add("近30天无血压/血糖记录，无法评估疾病变化");
        causes.add(cause("DISEASE", "疾病变化", disease, diseaseEv));

        // 2) 用药依从性
        List<String> adEv = new ArrayList<>();
        boolean adIssue = medLogs >= 3 && adherence < 0.8;
        if (medLogs == 0) {
            adEv.add("近30天无用药打卡记录，无法评估（本身即监测缺口）");
        } else {
            adEv.add(String.format("近30天用药打卡 %d 次：已服 %d 次、漏服 %d 次，按时服药率 %d%%（警戒线 80%%）",
                    medLogs, taken, missed, adherencePct));
        }
        adEv.add(String.format("连续漏服告警 %d 条", missedMedAlerts));
        causes.add(cause("ADHERENCE", "用药依从性", adIssue, adEv));

        // 3) 家属照护缺口
        List<String> careEv = new ArrayList<>();
        boolean careGap = (!hasProxy && r.familySupport != Enums.FamilySupport.STRONG)
                || recent.size() < 8
                || noUploadAlerts > 0
                || (hasProxy && familyUploads == 0);
        careEv.add(String.format("家庭支持程度「%s」，%s", familySupportLabel(r.familySupport),
                hasProxy ? "已设代管家属" : "未设代管家属"));
        careEv.add(String.format("近30天家庭监测上传 %d 次（建议 ≥8 次），其中家属代测 %d 次", recent.size(), familyUploads));
        if (noUploadAlerts > 0) careEv.add(String.format("长期未上传告警 %d 条", noUploadAlerts));
        if (hasProxy && familyUploads == 0) careEv.add("代管家属近30天未代测，代管未落实");
        causes.add(cause("CARE_GAP", "家属照护缺口", careGap, careEv));

        return causes;
    }

    private Map<String, Object> cause(String type, String label, boolean detected, List<String> evidence) {
        Map<String, Object> m = new HashMap<>();
        m.put("type", type);
        m.put("label", label);
        m.put("detected", detected);
        m.put("evidence", evidence);
        return m;
    }

    private static String familySupportLabel(Enums.FamilySupport f) {
        return switch (f) {
            case STRONG -> "强";
            case MODERATE -> "中";
            case WEAK -> "弱";
        };
    }
}
