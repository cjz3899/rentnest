package com.rentnest.rental;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentnest.common.audit.AuditLogEntity;
import com.rentnest.common.audit.AuditLogMapper;
import com.rentnest.rental.dto.AppointmentCreateReq;
import com.rentnest.rental.dto.AppointmentStatusReq;
import com.rentnest.rental.dto.HouseSaveReq;
import com.rentnest.rental.dto.HouseStatusReq;
import com.rentnest.rental.redis.CacheConstant;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 房源租赁端到端流程测试：真连本机 MySQL(rentnest_test) / Redis / MinIO，不做 mock。
 * 测试账号来自 db/seed.sql，密码 123456。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class RentalFlowTest {
    private static final String ADMIN_PHONE = "13800000001";
    private static final String LANDLORD_PHONE = "13800000002";
    private static final String USER_PHONE = "13800000003";
    private static final String PASSWORD = "123456";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private StringRedisTemplate redisTemplate;
    @Autowired
    private AuditLogMapper auditLogMapper;

    /**
     * 房源列表缓存与开发环境共用一个 Redis，先清掉避免读到开发库的数据
     */
    @BeforeEach
    void evictHouseCache() {
        Set<String> keys = redisTemplate.keys(CacheConstant.KEY_PREFIX + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("上传图片→建房源→游客浏览→预约→六态流转→房源状态联动")
    void fullRentalFlow() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        String user = login(USER_PHONE);

        long fileId = uploadHousePhoto(landlord);
        long houseId = createHouse(landlord, fileId);
        assertThat(adminHouseStatus(houseId, landlord)).isEqualTo("RENTABLE");

        // 游客可见列表与封面
        JsonNode guestItem = guestPageItem(houseId);
        assertThat(guestItem).as("新建房源应出现在游客列表").isNotNull();
        String coverUrl = guestItem.get("coverUrl").asText();
        assertThat(coverUrl).startsWith("/api/files/");

        // 游客可直接读取 PUBLIC 封面，走 MinIO 下载链路
        mockMvc.perform(get(coverUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        // 详情带出设施与媒体
        JsonNode detail = data(get("/api/houses/" + houseId));
        assertThat(detail.get("facilities")).hasSize(2);
        assertThat(detail.get("media").get(0).get("mediaType").asText()).isEqualTo("IMAGE");

        // 提交预约
        long appointmentId = createAppointment(user, houseId);

        // 非法跳转：待审核直接到已完成
        updateAppointmentStatus(landlord, appointmentId, "COMPLETED")
                .andExpect(jsonPath("$.code").value(40900));

        // 正常推进
        updateAppointmentStatus(landlord, appointmentId, "CONTACTED").andExpect(jsonPath("$.code").value(0));
        updateAppointmentStatus(landlord, appointmentId, "VIEWING").andExpect(jsonPath("$.code").value(0));
        updateAppointmentStatus(landlord, appointmentId, "CONFIRMED").andExpect(jsonPath("$.code").value(0));
        assertThat(adminHouseStatus(houseId, landlord))
                .as("预约已确认后房源应变为预约中").isEqualTo("RESERVED");

        updateAppointmentStatus(landlord, appointmentId, "COMPLETED").andExpect(jsonPath("$.code").value(0));
        assertThat(adminHouseStatus(houseId, landlord))
                .as("预约完成后房源应变为已出租").isEqualTo("RENTED");

        // 我的预约列表按 id 精确查找，避免与其他用例数据互相干扰
        JsonNode mine = null;
        for (JsonNode item : data(get("/api/my/appointments/page")
                .param("size", "50")
                .header("Authorization", "Bearer " + user)).withArray("records")) {
            if (item.get("id").asLong() == appointmentId) {
                mine = item;
            }
        }
        assertThat(mine).as("我的预约列表应包含该条").isNotNull();
        assertThat(mine.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(mine.get("houseTitle").asText()).startsWith("测试房源-");
    }

    @Test
    @DisplayName("已确认后取消预约应释放房源回到可出租")
    void cancelConfirmedAppointmentReleasesHouse() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        String user = login(USER_PHONE);
        long houseId = createHouse(landlord, uploadHousePhoto(landlord));
        long appointmentId = createAppointment(user, houseId);

        updateAppointmentStatus(landlord, appointmentId, "CONTACTED");
        updateAppointmentStatus(landlord, appointmentId, "VIEWING");
        updateAppointmentStatus(landlord, appointmentId, "CONFIRMED");
        assertThat(adminHouseStatus(houseId, landlord)).isEqualTo("RESERVED");

        updateAppointmentStatus(landlord, appointmentId, "CANCELLED")
                .andExpect(jsonPath("$.code").value(0));
        assertThat(adminHouseStatus(houseId, landlord))
                .as("已确认后取消，房源应回到可出租").isEqualTo("RENTABLE");
    }

    @Test
    @DisplayName("同一房源重复进行中预约被拒绝")
    void duplicateOngoingAppointmentRejected() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        String user = login(USER_PHONE);
        long houseId = createHouse(landlord, uploadHousePhoto(landlord));
        createAppointment(user, houseId);

        mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentReq(houseId))))
                .andExpect(jsonPath("$.code").value(40900));
    }

    @Test
    @DisplayName("普通用户与游客无法访问管理端房源接口")
    void adminEndpointsGuarded() throws Exception {
        String user = login(USER_PHONE);

        mockMvc.perform(get("/api/admin/houses/page")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        mockMvc.perform(get("/api/admin/houses/page"))
                .andExpect(status().isUnauthorized());

        // ADMIN 是超集角色，隐式放行
        mockMvc.perform(get("/api/admin/houses/page")
                        .header("Authorization", "Bearer " + login(ADMIN_PHONE)))
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    @DisplayName("暂停出租的房源对游客不可见，且状态变更写入审计日志")
    void suspendedHouseHiddenAndAudited() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        long houseId = createHouse(landlord, uploadHousePhoto(landlord));

        HouseStatusReq req = new HouseStatusReq();
        req.setStatus("SUSPENDED");
        mockMvc.perform(put("/api/admin/houses/" + houseId + "/status")
                        .header("Authorization", "Bearer " + landlord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(0));

        assertThat(guestPageItem(houseId)).as("暂停出租房源不应出现在游客列表").isNull();

        mockMvc.perform(get("/api/houses/" + houseId))
                .andExpect(jsonPath("$.code").value(40100));

        AuditLogEntity audit = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getAction, "CHANGE_HOUSE_STATUS")
                .eq(AuditLogEntity::getTargetId, String.valueOf(houseId))
                .last("LIMIT 1"));
        assertThat(audit).as("房源状态变更应留审计痕迹").isNotNull();
        assertThat(audit.getOperatorRole()).isEqualTo("LANDLORD");
    }

    @Test
    @DisplayName("存在进行中预约的房源不可删除")
    void houseWithOngoingAppointmentCannotBeDeleted() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        String user = login(USER_PHONE);
        long houseId = createHouse(landlord, uploadHousePhoto(landlord));
        createAppointment(user, houseId);

        mockMvc.perform(delete("/api/admin/houses/" + houseId)
                        .header("Authorization", "Bearer " + landlord))
                .andExpect(jsonPath("$.code").value(40900));
    }

    // ---------- helpers ----------

    private String login(String phone) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("phone", phone, "password", PASSWORD)))
        ).andExpect(status().isOk()).andReturn());
        assertThat(body.get("code").asInt()).isZero();
        return body.get("data").get("token").asText();
    }

    /**
     * 上传返回的 id 即业务表关联用的 file_record.id
     */
    private long uploadHousePhoto(String token) throws Exception {
        JsonNode data = readTree(mockMvc.perform(multipart("/api/files")
                        .file(new MockMultipartFile("file", "test-house.jpg", "image/jpeg",
                                new ClassPathResource("test-house.jpg").getInputStream()))
                        .param("visibility", "PUBLIC")
                        .param("dir", "house")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()).get("data");
        assertThat(data.get("uuid").asText()).isNotBlank();
        return data.get("id").asLong();
    }

    private long createHouse(String landlordToken, long fileId) throws Exception {
        HouseSaveReq req = new HouseSaveReq();
        req.setTitle("测试房源-" + System.nanoTime());
        req.setBuilding("3号楼");
        req.setFloorNo("5");
        req.setOrientation("南");
        req.setLayout("一室一厅");
        req.setArea(new java.math.BigDecimal("36.50"));
        req.setRent(new java.math.BigDecimal("1800.00"));
        req.setDeposit(new java.math.BigDecimal("1800.00"));
        req.setDescription("近地铁，采光好");
        req.setLeaseTerm("一年起租");
        req.setMoveInRequirement("限在校学生");
        req.setNotes("禁止养宠物");
        req.setFacilities(List.of("BED", "AC"));
        HouseSaveReq.MediaReq media = new HouseSaveReq.MediaReq();
        media.setFileId(fileId);
        media.setMediaType("IMAGE");
        req.setMedia(List.of(media));

        JsonNode body = readTree(mockMvc.perform(post("/api/admin/houses")
                        .header("Authorization", "Bearer " + landlordToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("id").asLong();
    }

    private AppointmentCreateReq appointmentReq(long houseId) {
        AppointmentCreateReq req = new AppointmentCreateReq();
        req.setHouseId(houseId);
        req.setContactName("李四");
        req.setContactPhone(USER_PHONE);
        req.setLeaseTerm("12个月");
        req.setRemark("希望尽快看房");
        return req;
    }

    private long createAppointment(String userToken, long houseId) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/appointments")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(appointmentReq(houseId))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("id").asLong();
    }

    private ResultActions updateAppointmentStatus(String landlordToken, long appointmentId, String status)
            throws Exception {
        AppointmentStatusReq req = new AppointmentStatusReq();
        req.setStatus(status);
        req.setRemark("测试流转到 " + status);
        return mockMvc.perform(put("/api/admin/appointments/" + appointmentId + "/status")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private String adminHouseStatus(long houseId, String landlordToken) throws Exception {
        return data(get("/api/admin/houses/" + houseId)
                .header("Authorization", "Bearer " + landlordToken)).get("status").asText();
    }

    private JsonNode data(org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder builder)
            throws Exception {
        return readTree(mockMvc.perform(builder).andExpect(jsonPath("$.code").value(0)).andReturn())
                .get("data");
    }

    private JsonNode guestPageItem(long houseId) throws Exception {
        for (JsonNode item : data(get("/api/houses/page").param("size", "50")).withArray("records")) {
            if (item.get("id").asLong() == houseId) {
                return item;
            }
        }
        return null;
    }

    private JsonNode readTree(org.springframework.test.web.servlet.MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
