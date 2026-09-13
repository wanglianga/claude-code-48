package com.community.chronic.web;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import com.community.chronic.service.EventLogger;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 连续高血压预警全流程：
 * 居民/家属补充测量信息 → 护士电话确认（记录症状和用药）→ 医生处置（调整随访/建议门诊/联系家属）。
 */
@RestController
@RequestMapping("/api/warnings")
public class WarningController {

    private final BpWarningRepo warningRepo;
    private final FamilyContactRepo contactRepo;
    private final FollowUpPlanRepo planRepo;
    private final AuthService authService;
    private final EventLogger eventLogger;

    public WarningController(BpWarningRepo warningRepo, FamilyContactRepo contactRepo,
                             FollowUpPlanRepo planRepo, AuthService authService, EventLogger eventLogger) {
        this.warningRepo = warningRepo;
        this.contactRepo = contactRepo;
        this.planRepo = planRepo;
        this.authService = authService;
        this.eventLogger = eventLogger;
    }

    @GetMapping
    public List<BpWarning> list(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @RequestParam(defaultValue = "OPEN") String status) {
        authService.require(user);
        List<BpWarning> base = "ALL".equals(status)
                ? warningRepo.findAll()
                : warningRepo.findByStatusOrderByCreatedAtDesc(Enums.WarningStatus.valueOf(status));
        return switch (user.role) {
            case ADMIN, NURSE -> base;
            case DOCTOR -> base.stream().filter(w -> w.record.doctor.id.equals(user.id)).toList();
            case RESIDENT -> base.stream().filter(w -> w.record.resident.id.equals(user.id)).toList();
            case FAMILY -> {
                Set<Long> ids = contactRepo.findByLinkedUserId(user.id).stream()
                        .map(c -> c.record.id).collect(Collectors.toSet());
                yield base.stream().filter(w -> ids.contains(w.record.id)).toList();
            }
        };
    }

    // ---------------- 居民/家属补充：测量时间、是否服药、症状、是否就医 ----------------

    public record SupplementReq(String suppMeasuredAt, Boolean tookMed, String symptoms, Boolean sawDoctor) {}

    @PostMapping("/{id}/supplement")
    @Transactional
    public BpWarning supplement(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                @PathVariable Long id, @RequestBody SupplementReq req) {
        authService.require(user, Enums.Role.RESIDENT, Enums.Role.FAMILY);
        BpWarning w = getWarning(id);
        checkFamilyAccess(user, w);

        w.suppMeasuredAt = req.suppMeasuredAt();
        w.tookMed = req.tookMed();
        w.symptoms = req.symptoms();
        w.sawDoctor = req.sawDoctor();
        w.suppBy = user.name;
        w.suppAt = LocalDateTime.now();
        warningRepo.save(w);

        eventLogger.log(w.record, Enums.EventType.ALERT,
                String.format("高血压预警补充信息（%s）：测量时间=%s，%s，症状=%s，%s",
                        user.name,
                        req.suppMeasuredAt() != null ? req.suppMeasuredAt() : "未填写",
                        Boolean.TRUE.equals(req.tookMed()) ? "已服药" : "未服药",
                        req.symptoms() != null ? req.symptoms() : "无",
                        Boolean.TRUE.equals(req.sawDoctor()) ? "已就医" : "未就医"),
                user.name);
        return w;
    }

    // ---------------- 护士电话确认：记录症状和用药情况 ----------------

    public record NurseConfirmReq(String symptoms, String medNote, String note) {}

    @PostMapping("/{id}/nurse-confirm")
    @Transactional
    public BpWarning nurseConfirm(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                  @PathVariable Long id, @RequestBody NurseConfirmReq req) {
        authService.require(user, Enums.Role.NURSE);
        BpWarning w = getWarning(id);
        if (w.status != Enums.WarningStatus.OPEN) {
            throw ApiException.badRequest("该预警不在待电话确认状态");
        }
        w.nurse = user;
        w.nurseConfirmedAt = LocalDateTime.now();
        w.nurseSymptoms = req.symptoms();
        w.nurseMedNote = req.medNote();
        w.nurseNote = req.note();
        w.status = Enums.WarningStatus.NURSE_CONFIRMED;
        warningRepo.save(w);

        eventLogger.log(w.record, Enums.EventType.ALERT,
                String.format("护士电话确认（%s）：症状=%s，用药情况=%s%s",
                        user.name,
                        req.symptoms() != null ? req.symptoms() : "无不适",
                        req.medNote() != null ? req.medNote() : "不详",
                        req.note() != null ? "，备注：" + req.note() : ""),
                user.name);
        return w;
    }

    // ---------------- 医生处置：调整随访提醒 / 建议门诊 / 联系家属 ----------------

    public record DoctorHandleReq(String action, String note, Integer reminderDays) {}

    @PostMapping("/{id}/doctor-handle")
    @Transactional
    public BpWarning doctorHandle(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                                  @PathVariable Long id, @RequestBody DoctorHandleReq req) {
        authService.require(user, Enums.Role.DOCTOR);
        BpWarning w = getWarning(id);
        if (w.status == Enums.WarningStatus.RESOLVED) {
            throw ApiException.badRequest("该预警已办结");
        }
        Enums.DoctorAction action = Enums.DoctorAction.valueOf(req.action());
        w.doctor = user;
        w.doctorAction = action;
        w.doctorNote = req.note();
        w.doctorHandledAt = LocalDateTime.now();
        w.status = Enums.WarningStatus.RESOLVED;
        warningRepo.save(w);

        ChronicRecord r = w.record;
        switch (action) {
            case ADJUST_FOLLOWUP -> {
                // 调整提醒频次：随访计划结合血压趋势/依从性/家属反馈后由医生给出新间隔
                if (req.reminderDays() == null || req.reminderDays() < 1) {
                    throw ApiException.badRequest("调整提醒频次需给出 reminderDays（天）");
                }
                FollowUpPlan p = planRepo.findFirstByRecordIdAndActiveTrue(r.id).orElseGet(() -> {
                    FollowUpPlan np = new FollowUpPlan();
                    np.record = r;
                    np.planType = Enums.PlanType.PHONE;
                    return np;
                });
                p.intervalDays = req.reminderDays();
                p.nextDueDate = LocalDate.now().plusDays(req.reminderDays());
                p.active = true;
                planRepo.save(p);
                eventLogger.log(r, Enums.EventType.FOLLOW_UP,
                        String.format("医生处置高血压预警：调整提醒频次为每 %d 天随访一次%s",
                                req.reminderDays(), req.note() != null ? "，" + req.note() : ""),
                        user.name);
            }
            case CLINIC -> eventLogger.log(r, Enums.EventType.FOLLOW_UP,
                    "医生处置高血压预警：建议门诊复诊" + (req.note() != null ? "，" + req.note() : ""), user.name);
            case CONTACT_FAMILY -> eventLogger.log(r, Enums.EventType.FOLLOW_UP,
                    "医生处置高血压预警：联系家属加强照护" + (req.note() != null ? "，" + req.note() : ""), user.name);
        }
        return w;
    }

    // ---------------- 工具 ----------------

    private BpWarning getWarning(Long id) {
        return warningRepo.findById(id).orElseThrow(() -> ApiException.notFound("预警不存在"));
    }

    private void checkFamilyAccess(UserAccount user, BpWarning w) {
        if (user.role == Enums.Role.RESIDENT) {
            if (!w.record.resident.id.equals(user.id)) throw ApiException.forbidden("非本人档案");
        } else {
            boolean linked = contactRepo.findByRecordId(w.record.id).stream()
                    .anyMatch(c -> c.linkedUser != null && c.linkedUser.id.equals(user.id));
            if (!linked) throw ApiException.forbidden("非该居民家属联系人");
        }
    }
}
