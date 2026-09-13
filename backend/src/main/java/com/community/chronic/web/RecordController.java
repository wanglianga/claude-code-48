package com.community.chronic.web;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import com.community.chronic.service.EventLogger;
import com.community.chronic.service.RuleEngine;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 慢病档案：建档、详情聚合、药物/家属/随访/转诊/住院/分层，全部回流同一档案。 */
@RestController
@RequestMapping("/api")
public class RecordController {

    private final ChronicRecordRepo recordRepo;
    private final UserAccountRepo userRepo;
    private final FamilyContactRepo contactRepo;
    private final MedicationRepo medicationRepo;
    private final FollowUpPlanRepo planRepo;
    private final FollowUpRepo followUpRepo;
    private final ReferralRepo referralRepo;
    private final HospitalizationRepo hospRepo;
    private final RecordEventRepo eventRepo;
    private final HealthUploadRepo uploadRepo;
    private final AlertRepo alertRepo;
    private final AuthService authService;
    private final EventLogger eventLogger;
    private final RuleEngine ruleEngine;

    public RecordController(ChronicRecordRepo recordRepo, UserAccountRepo userRepo, FamilyContactRepo contactRepo,
                            MedicationRepo medicationRepo, FollowUpPlanRepo planRepo, FollowUpRepo followUpRepo,
                            ReferralRepo referralRepo, HospitalizationRepo hospRepo, RecordEventRepo eventRepo,
                            HealthUploadRepo uploadRepo, AlertRepo alertRepo, AuthService authService,
                            EventLogger eventLogger, RuleEngine ruleEngine) {
        this.recordRepo = recordRepo;
        this.userRepo = userRepo;
        this.contactRepo = contactRepo;
        this.medicationRepo = medicationRepo;
        this.planRepo = planRepo;
        this.followUpRepo = followUpRepo;
        this.referralRepo = referralRepo;
        this.hospRepo = hospRepo;
        this.eventRepo = eventRepo;
        this.uploadRepo = uploadRepo;
        this.alertRepo = alertRepo;
        this.authService = authService;
        this.eventLogger = eventLogger;
        this.ruleEngine = ruleEngine;
    }

    // ---------------- 列表与详情 ----------------

    @GetMapping("/records")
    public List<ChronicRecord> list(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                    @RequestParam(required = false) String disease,
                                    @RequestParam(required = false) String level,
                                    @RequestParam(required = false) String status,
                                    @RequestParam(required = false) String q) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        List<ChronicRecord> base = user.role == Enums.Role.DOCTOR
                ? recordRepo.findByDoctorId(user.id)
                : recordRepo.findAll();
        return base.stream()
                .filter(r -> disease == null || r.diseaseType.name().equals(disease))
                .filter(r -> level == null || r.manageLevel.name().equals(level))
                .filter(r -> status == null || r.status.name().equals(status))
                .filter(r -> q == null || r.resident.name.contains(q) || r.resident.username.contains(q))
                .sorted(Comparator.comparing((ChronicRecord r) -> r.manageLevel == Enums.ManageLevel.KEY_FOCUS ? 0 : 1)
                        .thenComparing(r -> r.id))
                .toList();
    }

    @GetMapping("/records/{id}")
    public Map<String, Object> detail(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                      @PathVariable Long id) {
        ChronicRecord r = getRecord(id);
        checkRecordAccess(user, r);
        Map<String, Object> m = new HashMap<>();
        m.put("record", r);
        m.put("familyContacts", contactRepo.findByRecordId(id));
        m.put("medications", medicationRepo.findByRecordId(id));
        m.put("plans", planRepo.findByRecordId(id));
        m.put("followUps", followUpRepo.findByRecordIdOrderByVisitDateDesc(id));
        m.put("referrals", referralRepo.findByRecordIdOrderByCreatedAtDesc(id));
        m.put("hospitalizations", hospRepo.findByRecordIdOrderByStartDateDesc(id));
        m.put("events", eventRepo.findByRecordIdOrderByCreatedAtDesc(id));
        m.put("alerts", alertRepo.findByRecordIdOrderByCreatedAtDesc(id));
        m.put("suggestion", ruleEngine.suggestLevel(r));
        m.put("causes", ruleEngine.analyzeCauses(r));
        return m;
    }

    // ---------------- 建档 ----------------

    public record ContactReq(String name, String relation, String phone, Boolean proxy, String linkedUsername) {}
    public record MedReq(String name, String dosage, Integer timesPerDay, String timeSlots) {}
    public record PlanReq(String planType, Integer intervalDays) {}
    public record CreateReq(String residentName, String residentUsername, String residentPassword,
                            String residentPhone, String diseaseType, String diagnosis, String complications,
                            Integer targetSys, Integer targetDia, Double glucoseMin, Double glucoseMax,
                            String insurance, String familySupport, Long nurseId,
                            List<ContactReq> familyContacts, List<MedReq> medications, PlanReq plan) {}

    @PostMapping("/records")
    @Transactional
    public ChronicRecord create(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @RequestBody CreateReq req) {
        authService.require(user, Enums.Role.DOCTOR);

        // 居民账号：已存在则复用，否则创建
        String username = req.residentUsername();
        if (username == null || username.isBlank()) {
            username = "resident_" + System.currentTimeMillis();
        }
        final String uname = username;
        UserAccount resident = userRepo.findByUsername(uname).orElseGet(() -> userRepo.save(new UserAccount(
                uname,
                authService.hash(req.residentPassword() != null ? req.residentPassword() : "resident123"),
                Enums.Role.RESIDENT,
                req.residentName(),
                req.residentPhone())));

        ChronicRecord r = new ChronicRecord();
        r.resident = resident;
        r.doctor = user;
        if (req.nurseId() != null) {
            r.nurse = userRepo.findById(req.nurseId()).orElse(null);
        }
        r.diseaseType = Enums.DiseaseType.valueOf(req.diseaseType());
        r.diagnosis = req.diagnosis();
        r.complications = req.complications();
        if (req.targetSys() != null) r.targetSys = req.targetSys();
        if (req.targetDia() != null) r.targetDia = req.targetDia();
        if (req.glucoseMin() != null) r.glucoseMin = req.glucoseMin();
        if (req.glucoseMax() != null) r.glucoseMax = req.glucoseMax();
        r.insurance = req.insurance();
        if (req.familySupport() != null) r.familySupport = Enums.FamilySupport.valueOf(req.familySupport());
        recordRepo.save(r);

        if (req.familyContacts() != null) {
            for (ContactReq c : req.familyContacts()) {
                FamilyContact fc = new FamilyContact();
                fc.record = r;
                fc.name = c.name();
                fc.relation = c.relation();
                fc.phone = c.phone();
                fc.proxy = Boolean.TRUE.equals(c.proxy());
                if (c.linkedUsername() != null && !c.linkedUsername().isBlank()) {
                    fc.linkedUser = userRepo.findByUsername(c.linkedUsername()).orElse(null);
                }
                contactRepo.save(fc);
            }
        }
        if (req.medications() != null) {
            for (MedReq mr : req.medications()) {
                Medication med = new Medication();
                med.record = r;
                med.name = mr.name();
                med.dosage = mr.dosage();
                if (mr.timesPerDay() != null) med.timesPerDay = mr.timesPerDay();
                if (mr.timeSlots() != null) med.timeSlots = mr.timeSlots();
                medicationRepo.save(med);
            }
        }
        if (req.plan() != null) {
            FollowUpPlan p = new FollowUpPlan();
            p.record = r;
            if (req.plan().planType() != null) p.planType = Enums.PlanType.valueOf(req.plan().planType());
            if (req.plan().intervalDays() != null) p.intervalDays = req.plan().intervalDays();
            p.nextDueDate = LocalDate.now().plusDays(p.intervalDays);
            planRepo.save(p);
        }

        eventLogger.log(r, Enums.EventType.CREATED,
                "建立" + diseaseLabel(r.diseaseType) + "慢病档案，纳入社区管理", user.name);
        return r;
    }

    // ---------------- 档案维护 ----------------

    public record UpdateReq(String diagnosis, String complications, Integer targetSys, Integer targetDia,
                            Double glucoseMin, Double glucoseMax, String insurance, String familySupport,
                            String manageLevel, String status, Long nurseId, String reason) {}

    @PutMapping("/records/{id}")
    @Transactional
    public ChronicRecord update(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @PathVariable Long id, @RequestBody UpdateReq req) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        ChronicRecord r = getRecord(id);

        if (req.diagnosis() != null) r.diagnosis = req.diagnosis();
        if (req.complications() != null) r.complications = req.complications();
        if (req.targetSys() != null) r.targetSys = req.targetSys();
        if (req.targetDia() != null) r.targetDia = req.targetDia();
        if (req.glucoseMin() != null) r.glucoseMin = req.glucoseMin();
        if (req.glucoseMax() != null) r.glucoseMax = req.glucoseMax();
        if (req.insurance() != null) r.insurance = req.insurance();
        if (req.nurseId() != null) r.nurse = userRepo.findById(req.nurseId()).orElse(null);

        if (req.familySupport() != null) {
            Enums.FamilySupport fs = Enums.FamilySupport.valueOf(req.familySupport());
            if (fs != r.familySupport) {
                eventLogger.log(r, Enums.EventType.LEVEL_CHANGE,
                        "家庭支持程度评估为" + familySupportLabel(fs) + (req.reason() != null ? "：" + req.reason() : ""),
                        user.name);
                r.familySupport = fs;
            }
        }
        if (req.manageLevel() != null) {
            Enums.ManageLevel ml = Enums.ManageLevel.valueOf(req.manageLevel());
            if (ml != r.manageLevel) {
                eventLogger.log(r, Enums.EventType.LEVEL_CHANGE,
                        "分层管理级别调整为" + manageLevelLabel(ml) + (req.reason() != null ? "：" + req.reason() : ""),
                        user.name);
                r.manageLevel = ml;
            }
        }
        if (req.status() != null) {
            Enums.RecordStatus st = Enums.RecordStatus.valueOf(req.status());
            if (st != r.status) {
                if (st == Enums.RecordStatus.LOST) {
                    ruleEngine.markLost(r, req.reason() != null ? req.reason() : "医生手工标记失访");
                } else {
                    if (r.status == Enums.RecordStatus.LOST && st == Enums.RecordStatus.ACTIVE) {
                        eventLogger.log(r, Enums.EventType.LEVEL_CHANGE, "失访居民已找回，恢复在管", user.name);
                    }
                    r.status = st;
                }
            }
        }
        return recordRepo.save(r);
    }

    // ---------------- 药物 ----------------

    public record MedCreateReq(String name, String dosage, Integer timesPerDay, String timeSlots, String note) {}

    @PostMapping("/records/{id}/medications")
    @Transactional
    public Medication addMedication(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                    @PathVariable Long id, @RequestBody MedCreateReq req) {
        authService.require(user, Enums.Role.DOCTOR);
        ChronicRecord r = getRecord(id);
        Medication med = new Medication();
        med.record = r;
        med.name = req.name();
        med.dosage = req.dosage();
        if (req.timesPerDay() != null) med.timesPerDay = req.timesPerDay();
        if (req.timeSlots() != null) med.timeSlots = req.timeSlots();
        med.note = req.note();
        medicationRepo.save(med);
        eventLogger.log(r, Enums.EventType.MED_CHANGE,
                "新增药物 " + med.name + " " + (med.dosage != null ? med.dosage : ""), user.name);
        return med;
    }

    @PutMapping("/medications/{mid}/stop")
    @Transactional
    public Medication stopMedication(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                     @PathVariable Long mid, @RequestBody(required = false) Map<String, String> body) {
        authService.require(user, Enums.Role.DOCTOR);
        Medication med = medicationRepo.findById(mid).orElseThrow(() -> ApiException.notFound("药物不存在"));
        med.status = Enums.MedStatus.STOPPED;
        med.endDate = LocalDate.now();
        String reason = body != null ? body.get("reason") : null;
        eventLogger.log(med.record, Enums.EventType.MED_CHANGE,
                "停用药物 " + med.name + (reason != null ? "，原因：" + reason : ""), user.name);
        return medicationRepo.save(med);
    }

    // ---------------- 家属联系人 / 代管 ----------------

    @PostMapping("/records/{id}/family-contacts")
    @Transactional
    public FamilyContact addContact(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                    @PathVariable Long id, @RequestBody ContactReq req) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        ChronicRecord r = getRecord(id);
        FamilyContact fc = new FamilyContact();
        fc.record = r;
        fc.name = req.name();
        fc.relation = req.relation();
        fc.phone = req.phone();
        fc.proxy = Boolean.TRUE.equals(req.proxy());
        if (req.linkedUsername() != null && !req.linkedUsername().isBlank()) {
            fc.linkedUser = userRepo.findByUsername(req.linkedUsername()).orElse(null);
        }
        contactRepo.save(fc);
        if (fc.proxy) {
            eventLogger.log(r, Enums.EventType.PROXY_ASSIGN,
                    "设置家属代管：" + fc.name + "（" + (fc.relation != null ? fc.relation : "家属") + "）", user.name);
        }
        return fc;
    }

    @PutMapping("/family-contacts/{cid}/proxy")
    @Transactional
    public FamilyContact setProxy(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                  @PathVariable Long cid, @RequestBody Map<String, Boolean> body) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        FamilyContact fc = contactRepo.findById(cid).orElseThrow(() -> ApiException.notFound("联系人不存在"));
        boolean proxy = Boolean.TRUE.equals(body.get("proxy"));
        fc.proxy = proxy;
        eventLogger.log(fc.record, Enums.EventType.PROXY_ASSIGN,
                (proxy ? "设置家属代管：" : "取消家属代管：") + fc.name, user.name);
        return contactRepo.save(fc);
    }

    // ---------------- 随访 ----------------

    public record FollowUpReq(String visitDate, String mode, String summary, String adherence,
                              String familyFeedback, String recentMedical, String decision,
                              String decisionDetail, Integer nextIntervalDays) {}

    @PostMapping("/records/{id}/follow-ups")
    @Transactional
    public FollowUp addFollowUp(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @PathVariable Long id, @RequestBody FollowUpReq req) {
        authService.require(user, Enums.Role.DOCTOR);
        ChronicRecord r = getRecord(id);
        FollowUp f = new FollowUp();
        f.record = r;
        f.doctor = user;
        if (req.visitDate() != null) f.visitDate = LocalDate.parse(req.visitDate());
        if (req.mode() != null) f.mode = Enums.PlanType.valueOf(req.mode());
        f.summary = req.summary();
        if (req.adherence() != null) f.adherence = Enums.Adherence.valueOf(req.adherence());
        f.familyFeedback = req.familyFeedback();
        f.recentMedical = req.recentMedical();
        if (req.decision() != null) f.decision = Enums.FollowUpDecision.valueOf(req.decision());
        f.decisionDetail = req.decisionDetail();
        followUpRepo.save(f);

        // 更新随访计划下一次日期
        planRepo.findFirstByRecordIdAndActiveTrue(id).ifPresent(p -> {
            int days = req.nextIntervalDays() != null ? req.nextIntervalDays() : p.intervalDays;
            p.intervalDays = days;
            p.nextDueDate = f.visitDate.plusDays(days);
            planRepo.save(p);
        });

        eventLogger.log(r, Enums.EventType.FOLLOW_UP,
                "完成" + planTypeLabel(f.mode) + "随访，处置：" + decisionLabel(f.decision)
                        + (req.decisionDetail() != null ? "（" + req.decisionDetail() + "）" : ""), user.name);
        return f;
    }

    // ---------------- 转诊 ----------------

    public record ReferralReq(String toHospital, String reason) {}

    @PostMapping("/records/{id}/referrals")
    @Transactional
    public Referral addReferral(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @PathVariable Long id, @RequestBody ReferralReq req) {
        authService.require(user, Enums.Role.DOCTOR);
        ChronicRecord r = getRecord(id);
        Referral ref = new Referral();
        ref.record = r;
        ref.doctor = user;
        ref.toHospital = req.toHospital();
        ref.reason = req.reason();
        referralRepo.save(ref);
        eventLogger.log(r, Enums.EventType.REFERRAL,
                "转诊至 " + req.toHospital() + (req.reason() != null ? "，原因：" + req.reason() : ""), user.name);
        return ref;
    }

    @PutMapping("/referrals/{rid}/complete")
    @Transactional
    public Referral completeReferral(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                     @PathVariable Long rid, @RequestBody Map<String, String> body) {
        authService.require(user, Enums.Role.DOCTOR);
        Referral ref = referralRepo.findById(rid).orElseThrow(() -> ApiException.notFound("转诊单不存在"));
        ref.status = Enums.ReferralStatus.COMPLETED;
        ref.result = body.get("result");
        ref.completedAt = LocalDateTime.now();
        eventLogger.log(ref.record, Enums.EventType.REFERRAL,
                "转诊结果回填：" + (ref.result != null ? ref.result : "上级医院已接诊"), user.name);
        return referralRepo.save(ref);
    }

    // ---------------- 住院 ----------------

    public record HospReq(String hospital, String reason, String startDate, String endDate, String note) {}

    @PostMapping("/records/{id}/hospitalizations")
    @Transactional
    public Hospitalization addHosp(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                   @PathVariable Long id, @RequestBody HospReq req) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        ChronicRecord r = getRecord(id);
        Hospitalization h = new Hospitalization();
        h.record = r;
        h.hospital = req.hospital();
        h.reason = req.reason();
        if (req.startDate() != null) h.startDate = LocalDate.parse(req.startDate());
        if (req.endDate() != null) h.endDate = LocalDate.parse(req.endDate());
        h.note = req.note();
        hospRepo.save(h);
        eventLogger.log(r, Enums.EventType.HOSPITALIZATION,
                "住院：" + req.hospital() + (req.reason() != null ? "，" + req.reason() : ""), user.name);
        return h;
    }

    // ---------------- 分层 ----------------

    public record StratifyReq(String manageLevel, String familySupport, String reason) {}

    @PostMapping("/records/{id}/stratify")
    @Transactional
    public ChronicRecord stratify(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                  @PathVariable Long id, @RequestBody StratifyReq req) {
        authService.require(user, Enums.Role.DOCTOR);
        ChronicRecord r = getRecord(id);
        if (req.familySupport() != null) {
            r.familySupport = Enums.FamilySupport.valueOf(req.familySupport());
        }
        Enums.ManageLevel ml = Enums.ManageLevel.valueOf(req.manageLevel());
        if (ml != r.manageLevel) {
            eventLogger.log(r, Enums.EventType.LEVEL_CHANGE,
                    "分层管理级别调整为" + manageLevelLabel(ml) + (req.reason() != null ? "：" + req.reason() : ""),
                    user.name);
            r.manageLevel = ml;
        }
        return recordRepo.save(r);
    }

    // ---------------- 趋势与依从性（含家属代管 vs 居民自测差异） ----------------

    @GetMapping("/records/{id}/trends")
    public Map<String, Object> trends(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                      @PathVariable Long id, @RequestParam(defaultValue = "30") int days) {
        ChronicRecord r = getRecord(id);
        checkRecordAccess(user, r);
        LocalDateTime since = LocalDateTime.now().minusDays(days);

        List<HealthUpload> bps = uploadRepo.findByRecordIdAndTypeAndMeasuredAtAfterOrderByMeasuredAtAsc(
                id, Enums.UploadType.BP, since);
        List<HealthUpload> glucoses = uploadRepo.findByRecordIdAndTypeAndMeasuredAtAfterOrderByMeasuredAtAsc(
                id, Enums.UploadType.GLUCOSE, since);
        List<HealthUpload> recent = uploadRepo.findByRecordIdAndMeasuredAtAfterOrderByMeasuredAtDesc(id, since);

        long taken = recent.stream().filter(u -> u.type == Enums.UploadType.MEDICATION
                && u.medStatus == Enums.MedLogStatus.TAKEN).count();
        long missed = recent.stream().filter(u -> u.type == Enums.UploadType.MEDICATION
                && u.medStatus == Enums.MedLogStatus.MISSED).count();

        // 家属代测 vs 居民自测血压均值差异
        DoubleSummaryStatistics selfSys = bps.stream().filter(u -> u.uploaderType == Enums.UploaderType.SELF)
                .mapToDouble(u -> u.sys).summaryStatistics();
        DoubleSummaryStatistics famSys = bps.stream().filter(u -> u.uploaderType == Enums.UploaderType.FAMILY)
                .mapToDouble(u -> u.sys).summaryStatistics();
        DoubleSummaryStatistics selfDia = bps.stream().filter(u -> u.uploaderType == Enums.UploaderType.SELF)
                .mapToDouble(u -> u.dia).summaryStatistics();
        DoubleSummaryStatistics famDia = bps.stream().filter(u -> u.uploaderType == Enums.UploaderType.FAMILY)
                .mapToDouble(u -> u.dia).summaryStatistics();

        Map<String, Object> diff = new HashMap<>();
        diff.put("selfCount", selfSys.getCount());
        diff.put("familyCount", famSys.getCount());
        diff.put("selfAvgSys", selfSys.getCount() > 0 ? Math.round(selfSys.getAverage()) : null);
        diff.put("familyAvgSys", famSys.getCount() > 0 ? Math.round(famSys.getAverage()) : null);
        diff.put("selfAvgDia", selfDia.getCount() > 0 ? Math.round(selfDia.getAverage()) : null);
        diff.put("familyAvgDia", famDia.getCount() > 0 ? Math.round(famDia.getAverage()) : null);

        Map<String, Object> m = new HashMap<>();
        m.put("bp", bps);
        m.put("glucose", glucoses);
        m.put("medLogs", recent.stream().filter(u -> u.type == Enums.UploadType.MEDICATION).limit(50).toList());
        m.put("lifestyle", recent.stream()
                .filter(u -> u.type == Enums.UploadType.DIET || u.type == Enums.UploadType.EXERCISE)
                .limit(30).toList());
        m.put("takenCount", taken);
        m.put("missedCount", missed);
        m.put("adherence", (taken + missed) == 0 ? null : Math.round(100.0 * taken / (taken + missed)));
        m.put("selfVsFamily", diff);
        return m;
    }

    @GetMapping("/records/{id}/events")
    public List<RecordEvent> events(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                    @PathVariable Long id) {
        ChronicRecord r = getRecord(id);
        checkRecordAccess(user, r);
        return eventRepo.findByRecordIdOrderByCreatedAtDesc(id);
    }

    // ---------------- 工具 ----------------

    private ChronicRecord getRecord(Long id) {
        return recordRepo.findById(id).orElseThrow(() -> ApiException.notFound("档案不存在"));
    }

    /** 医生只能看自己负责的档案；护士/管理员全量；居民看自己；家属看代管。 */
    private void checkRecordAccess(UserAccount user, ChronicRecord r) {
        authService.require(user);
        switch (user.role) {
            case ADMIN, NURSE -> { /* 全量 */ }
            case DOCTOR -> {
                if (!r.doctor.id.equals(user.id)) throw ApiException.forbidden("非本人负责档案");
            }
            case RESIDENT -> {
                if (!r.resident.id.equals(user.id)) throw ApiException.forbidden("非本人档案");
            }
            case FAMILY -> {
                boolean linked = contactRepo.findByRecordId(r.id).stream()
                        .anyMatch(c -> c.linkedUser != null && c.linkedUser.id.equals(user.id));
                if (!linked) throw ApiException.forbidden("非代管家属");
            }
        }
    }

    static String diseaseLabel(Enums.DiseaseType t) {
        return switch (t) {
            case HYPERTENSION -> "高血压";
            case DIABETES -> "糖尿病";
            case CHD -> "冠心病";
        };
    }

    static String manageLevelLabel(Enums.ManageLevel l) {
        return switch (l) {
            case NORMAL -> "常规管理";
            case FAMILY_PROXY -> "家属代管";
            case HOME_VISIT -> "上门随访";
            case KEY_FOCUS -> "重点慢病名单";
        };
    }

    static String familySupportLabel(Enums.FamilySupport f) {
        return switch (f) {
            case STRONG -> "强";
            case MODERATE -> "中";
            case WEAK -> "弱";
        };
    }

    static String planTypeLabel(Enums.PlanType t) {
        return switch (t) {
            case PHONE -> "电话";
            case CLINIC -> "门诊";
            case HOME -> "上门";
        };
    }

    static String decisionLabel(Enums.FollowUpDecision d) {
        return switch (d) {
            case NONE -> "常规继续";
            case ADJUST_REMINDER -> "调整提醒";
            case REVIEW -> "建议复诊";
            case REFER -> "转诊上级医院";
        };
    }
}
