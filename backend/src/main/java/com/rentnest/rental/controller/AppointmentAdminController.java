package com.rentnest.rental.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.audit.AuditLog;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.auth.RequireRole;
import com.rentnest.common.auth.Roles;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.AppointmentQuery;
import com.rentnest.rental.dto.AppointmentResp;
import com.rentnest.rental.dto.AppointmentStatusReq;
import com.rentnest.rental.entity.RentalAppointment;
import com.rentnest.rental.service.AppointmentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/appointments")
@RequireRole(Roles.LANDLORD)
public class AppointmentAdminController {
    @Resource
    private AppointmentService appointmentService;

    @GetMapping("/page")
    public Result<PageResult<AppointmentResp>> page(AppointmentQuery query) {
        CurrentUser current = UserContextHolder.require();
        if (!current.isAdmin()) {
            query.setOwnerUserId(current.getId());
        }
        query.setUserId(null);
        return Result.ok(appointmentService.page(query));
    }

    @PutMapping("/{id}/status")
    @AuditLog(action = "UPDATE_APPOINTMENT_STATUS", targetType = "APPOINTMENT", targetId = "#id")
    public Result<RentalAppointment> updateStatus(@PathVariable Long id,
                                                  @Valid @RequestBody AppointmentStatusReq req) {
        return Result.ok(appointmentService.updateStatus(
                id, req.getStatus(), req.getRemark(), UserContextHolder.require()));
    }
}