package com.community.chronic.web;

import com.community.chronic.model.*;
import com.community.chronic.repo.*;
import com.community.chronic.service.AuthService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/** 告警任务：推送给社区护士、医生和家属。 */
@RestController
@RequestMapping("/api/alerts")
public class AlertController {

    private final AlertRepo alertRepo;
    private final FamilyContactRepo contactRepo;
    private final AuthService authService;

    public AlertController(AlertRepo alertRepo, FamilyContactRepo contactRepo, AuthService authService) {
        this.alertRepo = alertRepo;
        this.contactRepo = contactRepo;
        this.authService = authService;
    }

    @GetMapping
    public List<Alert> list(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                            @RequestParam(defaultValue = "OPEN") String status) {
        authService.require(user);
        Enums.AlertStatus st = "ALL".equals(status) ? null : Enums.AlertStatus.valueOf(status);
        List<Alert> base = st != null
                ? alertRepo.findByStatusOrderByCreatedAtDesc(st)
                : alertRepo.findAll();
        return switch (user.role) {
            case ADMIN, NURSE -> base;
            case DOCTOR -> base.stream().filter(a -> a.record.doctor.id.equals(user.id)).toList();
            case FAMILY -> {
                Set<Long> ids = contactRepo.findByLinkedUserId(user.id).stream()
                        .map(c -> c.record.id).collect(Collectors.toSet());
                yield base.stream().filter(a -> ids.contains(a.record.id)).toList();
            }
            case RESIDENT -> base.stream().filter(a -> a.record.resident.id.equals(user.id)).toList();
        };
    }

    @PostMapping("/{id}/ack")
    @Transactional
    public Alert ack(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                     @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        Alert a = alertRepo.findById(id).orElseThrow(() -> ApiException.notFound("告警不存在"));
        a.status = Enums.AlertStatus.ACKED;
        a.handledBy = user;
        a.handledAt = LocalDateTime.now();
        a.handleNote = body != null ? body.get("note") : null;
        return alertRepo.save(a);
    }

    @PostMapping("/{id}/resolve")
    @Transactional
    public Alert resolve(@RequestAttribute(value = "authUser", required = false) UserAccount user,
                         @PathVariable Long id, @RequestBody(required = false) Map<String, String> body) {
        authService.require(user, Enums.Role.DOCTOR, Enums.Role.NURSE);
        Alert a = alertRepo.findById(id).orElseThrow(() -> ApiException.notFound("告警不存在"));
        a.status = Enums.AlertStatus.RESOLVED;
        a.handledBy = user;
        a.handledAt = LocalDateTime.now();
        a.handleNote = body != null ? body.get("note") : null;
        return alertRepo.save(a);
    }
}
