package com.community.chronic.web;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import com.community.chronic.service.RuleEngine;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

/** 居民/家属家庭数据上传：血压、血糖、用药、饮食、运动。 */
@RestController
@RequestMapping("/api/uploads")
public class UploadController {

    private final HealthUploadRepo uploadRepo;
    private final ChronicRecordRepo recordRepo;
    private final FamilyContactRepo contactRepo;
    private final AuthService authService;
    private final RuleEngine ruleEngine;

    public UploadController(HealthUploadRepo uploadRepo, ChronicRecordRepo recordRepo,
                            FamilyContactRepo contactRepo, AuthService authService, RuleEngine ruleEngine) {
        this.uploadRepo = uploadRepo;
        this.recordRepo = recordRepo;
        this.contactRepo = contactRepo;
        this.authService = authService;
        this.ruleEngine = ruleEngine;
    }

    public record UploadReq(Long recordId, String type,
                            Integer sys, Integer dia, Integer heartRate,
                            Double glucose,
                            String medicationName, String medStatus,
                            String dietNote, String exerciseNote, String note,
                            String measuredAt) {}

    @PostMapping
    @Transactional
    public HealthUpload upload(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                               @RequestBody UploadReq req) {
        authService.require(user, Enums.Role.RESIDENT, Enums.Role.FAMILY);

        ChronicRecord record;
        Enums.UploaderType uploaderType;
        if (user.role == Enums.Role.RESIDENT) {
            record = recordRepo.findFirstByResidentId(user.id)
                    .orElseThrow(() -> ApiException.badRequest("您还没有慢病档案，请联系社区医生建档"));
            uploaderType = Enums.UploaderType.SELF;
        } else {
            if (req.recordId() == null) throw ApiException.badRequest("家属代传需指定 recordId");
            record = recordRepo.findById(req.recordId())
                    .orElseThrow(() -> ApiException.notFound("档案不存在"));
            boolean linked = contactRepo.findByRecordId(record.id).stream()
                    .anyMatch(c -> c.linkedUser != null && c.linkedUser.id.equals(user.id));
            if (!linked) throw ApiException.forbidden("您不是该居民的家属联系人");
            uploaderType = Enums.UploaderType.FAMILY;
        }

        HealthUpload u = new HealthUpload();
        u.record = record;
        u.uploader = user;
        u.uploaderType = uploaderType;
        u.type = Enums.UploadType.valueOf(req.type());
        u.sys = req.sys();
        u.dia = req.dia();
        u.heartRate = req.heartRate();
        u.glucose = req.glucose();
        u.medicationName = req.medicationName();
        if (req.medStatus() != null) u.medStatus = Enums.MedLogStatus.valueOf(req.medStatus());
        u.dietNote = req.dietNote();
        u.exerciseNote = req.exerciseNote();
        u.note = req.note();
        if (req.measuredAt() != null) u.measuredAt = LocalDateTime.parse(req.measuredAt());
        uploadRepo.save(u);

        // 规则引擎：达标判断、连续漏服、低血糖、不良反应
        ruleEngine.onUpload(u);
        return u;
    }

    @GetMapping("/mine")
    public List<HealthUpload> mine(@RequestAttribute(value = "authUser", required = false) UserAccount user) {
        authService.require(user, Enums.Role.RESIDENT, Enums.Role.FAMILY);
        if (user.role == Enums.Role.RESIDENT) {
            return recordRepo.findFirstByResidentId(user.id)
                    .map(r -> uploadRepo.findByRecordIdOrderByMeasuredAtDesc(r.id).stream().limit(50).toList())
                    .orElse(List.of());
        }
        return uploadRepo.findByUploaderIdOrderByCreatedAtDesc(user.id).stream().limit(50).toList();
    }
}
