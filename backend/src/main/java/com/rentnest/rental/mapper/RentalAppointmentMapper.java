package com.rentnest.rental.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.rentnest.rental.dto.AppointmentQuery;
import com.rentnest.rental.dto.AppointmentResp;
import com.rentnest.rental.entity.RentalAppointment;
import org.apache.ibatis.annotations.Mapper;

import java.util.List;

@Mapper
public interface RentalAppointmentMapper extends BaseMapper<RentalAppointment> {
    List<AppointmentResp> selectPage(AppointmentQuery query);

    long countByQuery(AppointmentQuery query);
}