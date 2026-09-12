package com.community.chronic.config;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import com.community.chronic.service.EventLogger;
import com.community.chronic.service.RuleEngine;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Random;

/** 首次启动写入演示账号与演示数据（数据库已有用户则跳过）。 */
@Component
public class DataInitializer implements CommandLineRunner {

    private final UserAccountRepo userRepo;
    private final ChronicRecordRepo recordRepo;
    private final FamilyContactRepo contactRepo;
    private final MedicationRepo medicationRepo;
    private final FollowUpPlanRepo planRepo;
    private final HealthUploadRepo uploadRepo;
    private final FollowUpRepo followUpRepo;
    private final AuthService authService;
    private final RuleEngine ruleEngine;
    private final EventLogger eventLogger;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    public DataInitializer(UserAccountRepo userRepo, ChronicRecordRepo recordRepo, FamilyContactRepo contactRepo,
                           MedicationRepo medicationRepo, FollowUpPlanRepo planRepo, HealthUploadRepo uploadRepo,
                           FollowUpRepo followUpRepo, AuthService authService, RuleEngine ruleEngine,
                           EventLogger eventLogger) {
        this.userRepo = userRepo;
        this.recordRepo = recordRepo;
        this.contactRepo = contactRepo;
        this.medicationRepo = medicationRepo;
        this.planRepo = planRepo;
        this.uploadRepo = uploadRepo;
        this.followUpRepo = followUpRepo;
        this.authService = authService;
        this.ruleEngine = ruleEngine;
        this.eventLogger = eventLogger;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (!seedDemoData || userRepo.count() > 0) {
            return;
        }
        Random rnd = new Random(42);

        // ---------- 账号 ----------
        UserAccount admin = userRepo.save(new UserAccount("admin", authService.hash("admin123"), Enums.Role.ADMIN, "系统管理员", "0000"));
        UserAccount doctor = userRepo.save(new UserAccount("doctor", authService.hash("doctor123"), Enums.Role.DOCTOR, "王建国", "13800000001"));
        UserAccount nurse = userRepo.save(new UserAccount("nurse", authService.hash("nurse123"), Enums.Role.NURSE, "李秀兰", "13800000002"));
        UserAccount zhang = userRepo.save(new UserAccount("resident", authService.hash("resident123"), Enums.Role.RESIDENT, "张福生", "13900000001"));
        UserAccount liu = userRepo.save(new UserAccount("resident2", authService.hash("resident123"), Enums.Role.RESIDENT, "刘桂芳", "13900000002"));
        UserAccount chen = userRepo.save(new UserAccount("resident3", authService.hash("resident123"), Enums.Role.RESIDENT, "陈国强", "13900000003"));
        UserAccount zhao = userRepo.save(new UserAccount("resident4", authService.hash("resident123"), Enums.Role.RESIDENT, "赵永贵", "13900000004"));
        UserAccount zhangSon = userRepo.save(new UserAccount("family", authService.hash("family123"), Enums.Role.FAMILY, "张小明", "13700000001"));
        UserAccount liuDaughter = userRepo.save(new UserAccount("family2", authService.hash("family123"), Enums.Role.FAMILY, "刘敏", "13700000002"));

        // ---------- 档案 ----------
        ChronicRecord r1 = newRecord(zhang, doctor, nurse, Enums.DiseaseType.HYPERTENSION,
                "原发性高血压 2 级，确诊 8 年", "轻度肾功能不全", 135, 85, "职工医保（签约家庭医生）",
                Enums.FamilySupport.STRONG, Enums.ManageLevel.FAMILY_PROXY);
        ChronicRecord r2 = newRecord(liu, doctor, nurse, Enums.DiseaseType.DIABETES,
                "2 型糖尿病，确诊 5 年", "糖尿病周围神经病变", 140, 90, "居民医保（签约家庭医生）",
                Enums.FamilySupport.MODERATE, Enums.ManageLevel.NORMAL);
        ChronicRecord r3 = newRecord(chen, doctor, nurse, Enums.DiseaseType.CHD,
                "冠心病，2023 年支架植入术后", "稳定型心绞痛", 130, 80, "职工医保（签约家庭医生）",
                Enums.FamilySupport.WEAK, Enums.ManageLevel.HOME_VISIT);
        ChronicRecord r4 = newRecord(zhao, doctor, nurse, Enums.DiseaseType.HYPERTENSION,
                "原发性高血压 3 级，确诊 12 年", "高血压性心脏病", 140, 90, "居民医保",
                Enums.FamilySupport.WEAK, Enums.ManageLevel.KEY_FOCUS);

        // ---------- 家属联系人 ----------
        contact(r1, "张小明", "儿子", "13700000001", zhangSon, true);
        contact(r1, "王淑华", "配偶", "13700000011", null, false);
        contact(r2, "刘敏", "女儿", "13700000002", liuDaughter, false);
        contact(r3, "陈兵", "儿子", "13700000003", null, false);
        contact(r4, "赵芳", "女儿（外地）", "13700000004", null, false);

        // ---------- 药物 ----------
        med(r1, "苯磺酸氨氯地平", "5mg", 1, "08:00");
        med(r1, "缬沙坦", "80mg", 1, "08:00");
        med(r2, "二甲双胍", "0.5g", 2, "08:00,18:00");
        med(r2, "格列美脲", "2mg", 1, "08:00");
        med(r3, "阿司匹林肠溶片", "100mg", 1, "08:00");
        med(r3, "阿托伐他汀", "20mg", 1, "20:00");
        med(r4, "硝苯地平控释片", "30mg", 1, "08:00");

        // ---------- 随访计划 ----------
        plan(r1, Enums.PlanType.PHONE, 30, LocalDate.now().plusDays(5));
        plan(r2, Enums.PlanType.CLINIC, 30, LocalDate.now().plusDays(2));
        plan(r3, Enums.PlanType.HOME, 14, LocalDate.now().minusDays(1)); // 已到期，演示待办
        plan(r4, Enums.PlanType.HOME, 7, LocalDate.now().minusDays(3));  // 重点名单，已逾期

        // ---------- 张福生：30 天血压（近 3 次连续超标）+ 用药（近 3 天漏服 3 次） ----------
        for (int i = 29; i >= 0; i--) {
            LocalDateTime t = LocalDateTime.now().minusDays(i).withHour(8).withMinute(30);
            int sys = 118 + rnd.nextInt(18);
            int dia = 72 + rnd.nextInt(12);
            if (i < 3) { sys = 142 + rnd.nextInt(10); dia = 88 + rnd.nextInt(8); } // 近 3 天连续超标
            boolean byFamily = i % 4 == 0; // 儿子每周代测
            upload(r1, byFamily ? zhangSon : zhang, byFamily ? Enums.UploaderType.FAMILY : Enums.UploaderType.SELF,
                    Enums.UploadType.BP, sys, dia, 68 + rnd.nextInt(10), null, null, null, null, null, null, t);
            // 用药打卡：近 3 天连续漏服（触发连续漏服告警）
            boolean missed = i < 3;
            upload(r1, zhang, Enums.UploaderType.SELF, Enums.UploadType.MEDICATION, null, null, null, null,
                    "苯磺酸氨氯地平", missed ? Enums.MedLogStatus.MISSED : Enums.MedLogStatus.TAKEN,
                    null, null, missed ? "早上忘记服药" : null, t.plusMinutes(10));
        }
        upload(r1, zhang, Enums.UploaderType.SELF, Enums.UploadType.DIET, null, null, null, null, null, null,
                "今日低盐饮食，晚餐清蒸鱼", null, null, LocalDateTime.now().minusDays(1).withHour(19));
        upload(r1, zhang, Enums.UploaderType.SELF, Enums.UploadType.EXERCISE, null, null, null, null, null, null,
                null, "晚饭后快走 40 分钟", null, LocalDateTime.now().minusDays(1).withHour(20));

        // ---------- 刘桂芳：血糖记录 + 一次低血糖 ----------
        for (int i = 20; i >= 0; i -= 2) {
            LocalDateTime t = LocalDateTime.now().minusDays(i).withHour(7).withMinute(0);
            double g = 5.2 + rnd.nextDouble() * 2.4;
            if (i == 4) g = 3.5; // 低血糖，触发告警
            upload(r2, liu, Enums.UploaderType.SELF, Enums.UploadType.GLUCOSE, null, null, null, g,
                    null, null, null, null, null, t);
            upload(r2, liu, Enums.UploaderType.SELF, Enums.UploadType.MEDICATION, null, null, null, null,
                    "二甲双胍", Enums.MedLogStatus.TAKEN, null, null, null, t.plusMinutes(30));
        }

        // ---------- 陈国强：血压 + 一次药物不良反应 ----------
        for (int i = 14; i >= 0; i -= 2) {
            LocalDateTime t = LocalDateTime.now().minusDays(i).withHour(9);
            upload(r3, chen, Enums.UploaderType.SELF, Enums.UploadType.BP, 122 + rnd.nextInt(14),
                    74 + rnd.nextInt(10), 62 + rnd.nextInt(8), null, null, null, null, null, null, t);
        }
        upload(r3, chen, Enums.UploaderType.SELF, Enums.UploadType.MEDICATION, null, null, null, null,
                "阿司匹林肠溶片", Enums.MedLogStatus.ADVERSE, null, null, "服药后胃部不适、恶心",
                LocalDateTime.now().minusDays(1).withHour(8));

        // ---------- 赵永贵：16 天未上传 → 失访 → 重点名单 ----------
        for (int i = 30; i >= 16; i -= 2) {
            LocalDateTime t = LocalDateTime.now().minusDays(i).withHour(8);
            upload(r4, zhao, Enums.UploaderType.SELF, Enums.UploadType.BP, 150 + rnd.nextInt(20),
                    90 + rnd.nextInt(10), 70 + rnd.nextInt(10), null, null, null, null, null, null, t);
        }
        ruleEngine.markLost(r4, "超过 14 天未上传家庭监测数据，电话随访未接通");

        // ---------- 历史随访 ----------
        followUp(r1, doctor, 30, "电话随访：血压总体平稳，嘱继续低盐饮食、规律服药。", Enums.Adherence.GOOD,
                "家属反馈老人服药自觉", "无近期就医", Enums.FollowUpDecision.NONE, null);
        followUp(r2, doctor, 35, "门诊随访：空腹血糖控制一般，调整二甲双胍剂量并加强饮食指导。", Enums.Adherence.FAIR,
                "女儿反映老人爱吃甜食", "上月社区门诊就诊 1 次", Enums.FollowUpDecision.ADJUST_REMINDER, "增加晚间血糖测量提醒");
        followUp(r3, doctor, 20, "上门随访：术后恢复可，偶有胸闷，嘱随身携带硝酸甘油。", Enums.Adherence.GOOD,
                "儿子在外地，日常独居", "2 月前心内科复诊", Enums.FollowUpDecision.REVIEW, "建议 1 个月内上级医院心内科复诊");
    }

    private ChronicRecord newRecord(UserAccount resident, UserAccount doctor, UserAccount nurse,
                                    Enums.DiseaseType type, String diagnosis, String complications,
                                    int targetSys, int targetDia, String insurance,
                                    Enums.FamilySupport support, Enums.ManageLevel level) {
        ChronicRecord r = new ChronicRecord();
        r.resident = resident;
        r.doctor = doctor;
        r.nurse = nurse;
        r.diseaseType = type;
        r.diagnosis = diagnosis;
        r.complications = complications;
        r.targetSys = targetSys;
        r.targetDia = targetDia;
        r.insurance = insurance;
        r.familySupport = support;
        r.manageLevel = level;
        recordRepo.save(r);
        eventLogger.log(r, Enums.EventType.CREATED, "建立慢病档案，纳入社区管理", doctor.name);
        return r;
    }

    private void contact(ChronicRecord r, String name, String relation, String phone, UserAccount linked, boolean proxy) {
        FamilyContact c = new FamilyContact();
        c.record = r;
        c.name = name;
        c.relation = relation;
        c.phone = phone;
        c.linkedUser = linked;
        c.proxy = proxy;
        contactRepo.save(c);
        if (proxy) {
            eventLogger.log(r, Enums.EventType.PROXY_ASSIGN, "设置家属代管：" + name + "（" + relation + "）", "王建国");
        }
    }

    private void med(ChronicRecord r, String name, String dosage, int times, String slots) {
        Medication m = new Medication();
        m.record = r;
        m.name = name;
        m.dosage = dosage;
        m.timesPerDay = times;
        m.timeSlots = slots;
        medicationRepo.save(m);
    }

    private void plan(ChronicRecord r, Enums.PlanType type, int interval, LocalDate next) {
        FollowUpPlan p = new FollowUpPlan();
        p.record = r;
        p.planType = type;
        p.intervalDays = interval;
        p.nextDueDate = next;
        planRepo.save(p);
    }

    private void upload(ChronicRecord r, UserAccount uploader, Enums.UploaderType ut, Enums.UploadType type,
                        Integer sys, Integer dia, Integer heartRate, Double glucose,
                        String medName, Enums.MedLogStatus medStatus,
                        String dietNote, String exerciseNote, String note, LocalDateTime measuredAt) {
        HealthUpload u = new HealthUpload();
        u.record = r;
        u.uploader = uploader;
        u.uploaderType = ut;
        u.type = type;
        u.sys = sys;
        u.dia = dia;
        u.heartRate = heartRate;
        u.glucose = glucose;
        u.medicationName = medName;
        u.medStatus = medStatus;
        u.dietNote = dietNote;
        u.exerciseNote = exerciseNote;
        u.note = note;
        u.measuredAt = measuredAt;
        uploadRepo.save(u);
        ruleEngine.onUpload(u); // 让告警自然产生
    }

    private void followUp(ChronicRecord r, UserAccount doctor, int daysAgo, String summary, Enums.Adherence adherence,
                          String familyFeedback, String recentMedical, Enums.FollowUpDecision decision, String detail) {
        FollowUp f = new FollowUp();
        f.record = r;
        f.doctor = doctor;
        f.visitDate = LocalDate.now().minusDays(daysAgo);
        f.summary = summary;
        f.adherence = adherence;
        f.familyFeedback = familyFeedback;
        f.recentMedical = recentMedical;
        f.decision = decision;
        f.decisionDetail = detail;
        f.createdAt = LocalDateTime.now().minusDays(daysAgo);
        followUpRepo.save(f);
        eventLogger.log(r, Enums.EventType.FOLLOW_UP, "随访：" + summary, doctor.name);
    }
}
