package com.community.chronic.web;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/** 工作台：按角色返回待办与统计。 */
@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ChronicRecordRepo recordRepo;
    private final AlertRepo alertRepo;
    private final FollowUpPlanRepo planRepo;
    private final MedicationRepo medicationRepo;
    private final HealthUploadRepo uploadRepo;
    private final FamilyContactRepo contactRepo;
    private final AuthService authService;

    public DashboardController(ChronicRecordRepo recordRepo, AlertRepo alertRepo, FollowUpPlanRepo planRepo,
                               MedicationRepo medicationRepo, HealthUploadRepo uploadRepo,
                               FamilyContactRepo contactRepo, AuthService authService) {
        this.recordRepo = recordRepo;
        this.alertRepo = alertRepo;
        this.planRepo = planRepo;
        this.medicationRepo = medicationRepo;
        this.uploadRepo = uploadRepo;
        this.contactRepo = contactRepo;
        this.authService = authService;
    }

    @GetMapping
    public Map<String, Object> dashboard(@RequestAttribute(value = "authUser", required = false) UserAccount user) {
        authService.require(user);
        return switch (user.role) {
            case DOCTOR -> doctorDashboard(user);
            case NURSE -> nurseDashboard();
            case RESIDENT -> residentDashboard(user);
            case FAMILY -> familyDashboard(user);
            case ADMIN -> nurseDashboard();
        };
    }

    private Map<String, Object> doctorDashboard(UserAccount doctor) {
        List<ChronicRecord> mine = recordRepo.findByDoctorId(doctor.id);
        Set<Long> myIds = mine.stream().map(r -> r.id).collect(Collectors.toSet());

        List<Alert> openAlerts = alertRepo.findByRecordDoctorIdAndStatusOrderByCreatedAtDesc(
                doctor.id, Enums.AlertStatus.OPEN);

        List<FollowUpPlan> duePlans = planRepo.findByActiveTrueAndNextDueDateLessThanEqual(LocalDate.now())
                .stream().filter(p -> myIds.contains(p.record.id)).toList();

        Map<String, Long> levelDist = mine.stream()
                .collect(Collectors.groupingBy(r -> r.manageLevel.name(), Collectors.counting()));

        Map<String, Object> m = new HashMap<>();
        m.put("role", "DOCTOR");
        m.put("totalRecords", mine.size());
        m.put("activeRecords", mine.stream().filter(r -> r.status == Enums.RecordStatus.ACTIVE).count());
        m.put("lostCount", mine.stream().filter(r -> r.status == Enums.RecordStatus.LOST).count());
        m.put("keyFocusCount", mine.stream().filter(r -> r.manageLevel == Enums.ManageLevel.KEY_FOCUS).count());
        m.put("openAlertCount", openAlerts.size());
        m.put("duePlanCount", duePlans.size());
        m.put("levelDist", levelDist);
        m.put("recentAlerts", openAlerts.stream().limit(10).toList());
        m.put("duePlans", duePlans.stream().limit(10).toList());
        return m;
    }

    private Map<String, Object> nurseDashboard() {
        List<Alert> open = alertRepo.findByStatusOrderByCreatedAtDesc(Enums.AlertStatus.OPEN);
        Map<String, Object> m = new HashMap<>();
        m.put("role", "NURSE");
        m.put("openAlertCount", open.size());
        m.put("recentAlerts", open.stream().limit(20).toList());
        m.put("lostCount", recordRepo.countByStatus(Enums.RecordStatus.LOST));
        m.put("keyFocusCount", recordRepo.countByManageLevel(Enums.ManageLevel.KEY_FOCUS));
        return m;
    }

    private Map<String, Object> residentDashboard(UserAccount resident) {
        Map<String, Object> m = new HashMap<>();
        m.put("role", "RESIDENT");
        Optional<ChronicRecord> rec = recordRepo.findFirstByResidentId(resident.id);
        if (rec.isEmpty()) {
            m.put("record", null);
            return m;
        }
        ChronicRecord r = rec.get();
        List<Medication> meds = medicationRepo.findByRecordIdAndStatus(r.id, Enums.MedStatus.ACTIVE);

        // 今日已打卡的用药（按药名）
        LocalDateTime dayStart = LocalDate.now().atStartOfDay();
        Set<String> takenToday = uploadRepo.findByRecordIdAndMeasuredAtAfterOrderByMeasuredAtDesc(r.id, dayStart)
                .stream()
                .filter(u -> u.type == Enums.UploadType.MEDICATION && u.medStatus == Enums.MedLogStatus.TAKEN)
                .map(u -> u.medicationName)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        List<Map<String, Object>> todayMeds = meds.stream().map(med -> {
            Map<String, Object> mm = new HashMap<>();
            mm.put("id", med.id);
            mm.put("name", med.name);
            mm.put("dosage", med.dosage);
            mm.put("timeSlots", med.timeSlots);
            mm.put("takenToday", takenToday.contains(med.name));
            return mm;
        }).toList();

        m.put("record", r);
        m.put("todayMeds", todayMeds);
        m.put("nextFollowUp", planRepo.findFirstByRecordIdAndActiveTrue(r.id).map(p -> p.nextDueDate).orElse(null));
        m.put("recentUploads", uploadRepo.findByRecordIdOrderByMeasuredAtDesc(r.id).stream().limit(10).toList());
        return m;
    }

    private Map<String, Object> familyDashboard(UserAccount family) {
        List<FamilyContact> contacts = contactRepo.findByLinkedUserId(family.id);
        List<Map<String, Object>> records = contacts.stream().map(c -> {
            ChronicRecord r = c.record;
            Map<String, Object> mm = new HashMap<>();
            mm.put("contactId", c.id);
            mm.put("proxy", c.proxy);
            mm.put("relation", c.relation);
            mm.put("record", r);
            mm.put("lastUpload", uploadRepo.findFirstByRecordIdOrderByMeasuredAtDesc(r.id)
                    .map(u -> u.measuredAt).orElse(null));
            return mm;
        }).toList();

        Set<Long> recordIds = contacts.stream().map(c -> c.record.id).collect(Collectors.toSet());
        List<Alert> alerts = alertRepo.findByStatusOrderByCreatedAtDesc(Enums.AlertStatus.OPEN)
                .stream().filter(a -> recordIds.contains(a.record.id)).toList();

        Map<String, Object> m = new HashMap<>();
        m.put("role", "FAMILY");
        m.put("records", records);
        m.put("openAlerts", alerts);
        return m;
    }
}
