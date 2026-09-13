package com.community.chronic.model;

/** 平台全部枚举定义（集中一处，便于前后端对照）。 */
public final class Enums {

    private Enums() {}

    /** 用户角色 */
    public enum Role { DOCTOR, NURSE, RESIDENT, FAMILY, ADMIN }

    /** 慢病病种 */
    public enum DiseaseType { HYPERTENSION, DIABETES, CHD }

    /** 家庭支持程度 */
    public enum FamilySupport { STRONG, MODERATE, WEAK }

    /** 分层管理级别 */
    public enum ManageLevel { NORMAL, FAMILY_PROXY, HOME_VISIT, KEY_FOCUS }

    /** 档案状态 */
    public enum RecordStatus { ACTIVE, LOST, CLOSED }

    /** 药物状态 */
    public enum MedStatus { ACTIVE, STOPPED }

    /** 家庭上传记录类型 */
    public enum UploadType { BP, GLUCOSE, MEDICATION, DIET, EXERCISE }

    /** 上传来源：居民自测 / 家属代测 */
    public enum UploaderType { SELF, FAMILY }

    /** 用药执行状态 */
    public enum MedLogStatus { TAKEN, MISSED, ADVERSE }

    /** 告警类型 */
    public enum AlertType { MISSED_MED, BP_HIGH, GLUCOSE_LOW, ADVERSE_REACTION, NO_UPLOAD }

    /** 告警级别 */
    public enum AlertLevel { INFO, WARN, CRITICAL }

    /** 告警处理状态 */
    public enum AlertStatus { OPEN, ACKED, RESOLVED }

    /** 随访方式 */
    public enum PlanType { PHONE, CLINIC, HOME }

    /** 随访结论 */
    public enum FollowUpDecision { NONE, ADJUST_REMINDER, REVIEW, REFER }

    /** 用药依从性评估 */
    public enum Adherence { GOOD, FAIR, POOR }

    /** 转诊状态 */
    public enum ReferralStatus { OPEN, COMPLETED }

    /** 连续高血压预警状态：待补充/待电话确认 → 待医生处置 → 已办结 */
    public enum WarningStatus { OPEN, NURSE_CONFIRMED, RESOLVED }

    /** 医生对高血压预警的处置：调整随访提醒 / 建议门诊 / 联系家属 */
    public enum DoctorAction { ADJUST_FOLLOWUP, CLINIC, CONTACT_FAMILY }

    /** 档案事件类型（事件流，所有关键变化都回到同一档案） */
    public enum EventType {
        CREATED,            // 建档
        MED_CHANGE,         // 药物变更
        PROXY_ASSIGN,       // 家属代管设置/取消
        HOSPITALIZATION,    // 住院
        LOST,               // 失访
        REFERRAL,           // 转诊及转诊结果
        LEVEL_CHANGE,       // 分层调整
        FOLLOW_UP,          // 随访
        ALERT               // 告警
    }
}
