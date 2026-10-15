package com.rentnest.rental.controller;

import com.rentnest.common.api.Result;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.page.PageResult;
import com.rentnest.rental.dto.AppointmentCreateReq;
import com.rentnest.rental.dto.AppointmentQuery;
import com.rentnest.rental.dto.AppointmentResp;
import com.rentnest.rental.entity.RentalAppointment;
import com.rentnest.rental.service.AppointmentService;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api")
public class AppointmentController {
    @Resource
    private AppointmentService appointmentService;

    @PostMapping("/appointments")
    public Result<RentalAppointment> create(@Valid @RequestBody AppointmentCreateReq req) {
        return Result.ok(appointmentService.create(req, UserContextHolder.require()));
    }

    @GetMapping("/my/appointments/page")
    public Result<PageResult<AppointmentResp>> myAppointments(AppointmentQuery query) {
        query.setUserId(UserContextHolder.require().getId());
        query.setOwnerUserId(null);
        return Result.ok(appointmentService.page(query));
    }

    @PutMapping("/my/appointments/{id}/cancel")
    public Result<RentalAppointment> cancel(@PathVariable Long id) {
        return Result.ok(appointmentService.cancelOwn(id, UserContextHolder.require()));
    }
}