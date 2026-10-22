package com.rentnest.tenant;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rentnest.common.audit.AuditLogEntity;
import com.rentnest.common.audit.AuditLogMapper;
import com.rentnest.tenant.dto.AnnouncementSaveReq;
import com.rentnest.tenant.dto.AnnouncementStatusReq;
import com.rentnest.tenant.dto.FeedbackCreateReq;
import com.rentnest.tenant.dto.FeedbackReplyReq;
import com.rentnest.tenant.dto.RepairCreateReq;
import com.rentnest.tenant.dto.RepairHandleReq;
import com.rentnest.tenant.redis.AnnouncementCache;
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
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 租客服务端到端测试：真连本机 MySQL(rentnest_test) / Redis / MinIO，不做 mock。
 * 图片统一使用 test-house.jpg 走真实上传链路。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TenantFlowTest {
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
     * 公告列表缓存与开发环境共用 Redis，逐个用例前清空避免读到旧数据
     */
    @BeforeEach
    void evictAnnouncementCache() {
        Set<String> keys = redisTemplate.keys(AnnouncementCache.KEY_PREFIX + "*");
        if (!keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @DisplayName("报修：带图提交→本人/他人可见性→状态机流转→审计")
    void repairFlowWithImage() throws Exception {
        String user = login(USER_PHONE);
        String landlord = login(LANDLORD_PHONE);

        long fileId = uploadPhoto(user, "repair");
        long ticketId = createRepair(user, fileId, "卫生间热水器不出热水");

        // 本人详情带出图片 URL，PUBLIC 图片游客可直接读取
        JsonNode detail = data(get("/api/repairs/" + ticketId)
                .header("Authorization", "Bearer " + user));
        assertThat(detail.get("mediaUrls")).hasSize(1);
        String mediaUrl = detail.get("mediaUrls").get(0).asText();
        assertThat(mediaUrl).startsWith("/api/files/");
        mockMvc.perform(get(mediaUrl))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        // 我的工单列表包含该条
        JsonNode mine = findById(data(get("/api/my/repairs/page")
                .param("size", "100")
                .header("Authorization", "Bearer " + user)), "records", ticketId);
        assertThat(mine).as("我的报修列表应包含该工单").isNotNull();
        assertThat(mine.get("status").asText()).isEqualTo("PENDING");
        assertThat(mine.get("userNickname").asText()).isNotBlank();

        // 普通用户不能进管理端
        mockMvc.perform(get("/api/admin/repairs/page")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 其他普通用户不能看别人的工单
        String stranger = registerStranger();
        mockMvc.perform(get("/api/repairs/" + ticketId)
                        .header("Authorization", "Bearer " + stranger))
                .andExpect(jsonPath("$.code").value(40300));

        // 不能把别人上传的图片挂到自己的工单
        long landlordFileId = uploadPhoto(landlord, "repair");
        RepairCreateReq stealReq = repairReq("走廊灯不亮", landlordFileId);
        mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + user)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(stealReq)))
                .andExpect(jsonPath("$.code").value(40000));

        // 状态机：待受理不能直接完成
        handleRepair(landlord, ticketId, "COMPLETED", "跳过受理")
                .andExpect(jsonPath("$.code").value(40900));

        handleRepair(landlord, ticketId, "PROCESSING", "已派单")
                .andExpect(jsonPath("$.code").value(0));
        handleRepair(landlord, ticketId, "COMPLETED", "已更换热水器")
                .andExpect(jsonPath("$.code").value(0));

        JsonNode adminDetail = data(get("/api/admin/repairs/" + ticketId)
                .header("Authorization", "Bearer " + landlord));
        assertThat(adminDetail.get("status").asText()).isEqualTo("COMPLETED");
        assertThat(adminDetail.get("handlerRemark").asText()).isEqualTo("已更换热水器");
        assertThat(adminDetail.get("handledAt").isNull()).isFalse();

        AuditLogEntity audit = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getAction, "HANDLE_REPAIR")
                .eq(AuditLogEntity::getTargetId, String.valueOf(ticketId))
                .last("LIMIT 1"));
        assertThat(audit).as("报修处理应留审计痕迹").isNotNull();
        assertThat(audit.getOperatorRole()).isEqualTo("LANDLORD");
    }

    @Test
    @DisplayName("反馈：带图提交→房东回复→关闭→关闭后不可回复")
    void feedbackFlowWithImage() throws Exception {
        String user = login(USER_PHONE);
        String landlord = login(LANDLORD_PHONE);

        long fileId = uploadPhoto(user, "feedback");
        long feedbackId = createFeedback(user, fileId);

        // 列表批量装配图片 URL
        JsonNode mine = findById(data(get("/api/my/feedbacks/page")
                .param("size", "100")
                .header("Authorization", "Bearer " + user)), "records", feedbackId);
        assertThat(mine).as("我的反馈列表应包含该条").isNotNull();
        assertThat(mine.get("mediaUrls")).hasSize(1);
        assertThat(mine.get("status").asText()).isEqualTo("PENDING");

        // 普通用户不能进管理端
        mockMvc.perform(get("/api/admin/feedbacks/page")
                        .header("Authorization", "Bearer " + user))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));

        // 房东回复
        FeedbackReplyReq replyReq = new FeedbackReplyReq();
        replyReq.setReply("已联系维修师傅，明天上门");
        mockMvc.perform(put("/api/admin/feedbacks/" + feedbackId + "/reply")
                        .header("Authorization", "Bearer " + landlord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("REPLIED"));

        JsonNode replied = findById(data(get("/api/my/feedbacks/page")
                .param("size", "100")
                .header("Authorization", "Bearer " + user)), "records", feedbackId);
        assertThat(replied.get("status").asText()).isEqualTo("REPLIED");
        assertThat(replied.get("reply").asText()).isEqualTo("已联系维修师傅，明天上门");

        // 关闭
        mockMvc.perform(put("/api/admin/feedbacks/" + feedbackId + "/close")
                        .header("Authorization", "Bearer " + landlord))
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.status").value("CLOSED"));

        // 关闭后再回复应冲突
        mockMvc.perform(put("/api/admin/feedbacks/" + feedbackId + "/reply")
                        .header("Authorization", "Bearer " + landlord)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(replyReq)))
                .andExpect(jsonPath("$.code").value(40900));

        assertThat(auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getAction, "REPLY_FEEDBACK")
                .eq(AuditLogEntity::getTargetId, String.valueOf(feedbackId))
                .last("LIMIT 1"))).as("反馈回复应留审计痕迹").isNotNull();
        assertThat(auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getAction, "CLOSE_FEEDBACK")
                .eq(AuditLogEntity::getTargetId, String.valueOf(feedbackId))
                .last("LIMIT 1"))).as("反馈关闭应留审计痕迹").isNotNull();
    }

    @Test
    @DisplayName("公告：游客仅见已发布且置顶优先，写操作失效缓存，管理端仅 ADMIN")
    void announcementPublishedVisibilityAndCache() throws Exception {
        String admin = login(ADMIN_PHONE);
        String landlord = login(LANDLORD_PHONE);
        String suffix = String.valueOf(System.nanoTime());

        long pinnedId = createAnnouncement(admin, "置顶FAQ-" + suffix, "FAQ", "PUBLISHED", true);
        long normalId = createAnnouncement(admin, "普通FAQ-" + suffix, "FAQ", "PUBLISHED", false);
        long hiddenId = createAnnouncement(admin, "未发布FAQ-" + suffix, "FAQ", "UNPUBLISHED", false);

        // 游客列表：未发布不可见，置顶排在普通之前
        JsonNode publicPage = data(get("/api/announcements/public/page")
                .param("type", "FAQ")
                .param("size", "100"));
        JsonNode records = publicPage.get("records");
        int pinnedIndex = indexOfId(records, pinnedId);
        int normalIndex = indexOfId(records, normalId);
        assertThat(indexOfId(records, hiddenId)).as("未发布公告不应出现在游客列表").isEqualTo(-1);
        assertThat(pinnedIndex).isGreaterThanOrEqualTo(0);
        assertThat(normalIndex).isGreaterThan(pinnedIndex);

        // 游客详情：已发布可读，未发布 404
        assertThat(data(get("/api/announcements/public/" + pinnedId))
                .get("title").asText()).startsWith("置顶FAQ-");
        mockMvc.perform(get("/api/announcements/public/" + hiddenId))
                .andExpect(jsonPath("$.code").value(40400));

        // 管理端仅 ADMIN
        mockMvc.perform(get("/api/admin/announcements/page")
                        .header("Authorization", "Bearer " + landlord))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/admin/announcements/page")
                        .header("Authorization", "Bearer " + admin))
                .andExpect(jsonPath("$.code").value(0));

        // 发布未发布公告后，缓存应失效，游客立即可见
        AnnouncementStatusReq statusReq = new AnnouncementStatusReq();
        statusReq.setStatus("PUBLISHED");
        mockMvc.perform(put("/api/admin/announcements/" + hiddenId + "/status")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(statusReq)))
                .andExpect(jsonPath("$.code").value(0));

        JsonNode refreshed = data(get("/api/announcements/public/page")
                .param("type", "FAQ")
                .param("size", "100")).get("records");
        assertThat(indexOfId(refreshed, hiddenId))
                .as("发布后缓存失效，游客列表应立即可见").isGreaterThanOrEqualTo(0);

        // 未知类型被拒绝
        AnnouncementSaveReq badReq = announcementReq("非法类型-" + suffix, "BAD", "PUBLISHED", false);
        mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + admin)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(badReq)))
                .andExpect(jsonPath("$.code").value(40000));

        AuditLogEntity audit = auditLogMapper.selectOne(new LambdaQueryWrapper<AuditLogEntity>()
                .eq(AuditLogEntity::getAction, "CREATE_ANNOUNCEMENT")
                .eq(AuditLogEntity::getTargetId, String.valueOf(pinnedId))
                .last("LIMIT 1"));
        assertThat(audit).as("公告创建应留审计痕迹且 targetId 来自返回结果").isNotNull();
        assertThat(audit.getOperatorRole()).isEqualTo("ADMIN");
    }

    // ---------- helpers ----------

    private String login(String phone) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone, "password", PASSWORD))))
                .andExpect(status().isOk())
                .andReturn());
        assertThat(body.get("code").asInt()).isZero();
        return body.get("data").get("token").asText();
    }

    private String registerStranger() throws Exception {
        String phone = "139" + String.format("%08d",
                Math.floorMod(System.nanoTime(), 100_000_000L));
        JsonNode body = readTree(mockMvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "phone", phone,
                                "password", PASSWORD,
                                "nickname", "无关用户"))))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("token").asText();
    }

    /**
     * 上传返回的 id 即业务表关联用的 file_record.id
     */
    private long uploadPhoto(String token, String dir) throws Exception {
        JsonNode data = readTree(mockMvc.perform(multipart("/api/files")
                        .file(new MockMultipartFile("file", "test-house.jpg", "image/jpeg",
                                new ClassPathResource("test-house.jpg").getInputStream()))
                        .param("visibility", "PUBLIC")
                        .param("dir", dir)
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()).get("data");
        assertThat(data.get("uuid").asText()).isNotBlank();
        return data.get("id").asLong();
    }

    private long createRepair(String userToken, long fileId, String description) throws Exception {
        RepairCreateReq req = repairReq(description, fileId);
        JsonNode body = readTree(mockMvc.perform(post("/api/repairs")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("id").asLong();
    }

    private RepairCreateReq repairReq(String description, long fileId) {
        RepairCreateReq req = new RepairCreateReq();
        req.setLocation("卫生间");
        req.setDescription(description);
        req.setContactPhone(USER_PHONE);
        req.setSupplement("工作日晚上七点后在家");
        req.setFileIds(List.of(fileId));
        return req;
    }

    private ResultActions handleRepair(String landlordToken, long ticketId,
                                       String status, String remark) throws Exception {
        RepairHandleReq req = new RepairHandleReq();
        req.setStatus(status);
        req.setRemark(remark);
        return mockMvc.perform(put("/api/admin/repairs/" + ticketId + "/status")
                .header("Authorization", "Bearer " + landlordToken)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)));
    }

    private long createFeedback(String userToken, long fileId) throws Exception {
        FeedbackCreateReq req = new FeedbackCreateReq();
        req.setContent("楼道卫生打扫不及时");
        req.setProblemDesc("垃圾桶经常满溢");
        req.setContactPhone(USER_PHONE);
        req.setFileIds(List.of(fileId));

        JsonNode body = readTree(mockMvc.perform(post("/api/feedbacks")
                        .header("Authorization", "Bearer " + userToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("id").asLong();
    }

    private long createAnnouncement(String adminToken, String title, String type,
                                    String statusValue, boolean pinned) throws Exception {
        AnnouncementSaveReq req = announcementReq(title, type, statusValue, pinned);
        JsonNode body = readTree(mockMvc.perform(post("/api/admin/announcements")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("id").asLong();
    }

    private AnnouncementSaveReq announcementReq(String title, String type,
                                                String statusValue, boolean pinned) {
        AnnouncementSaveReq req = new AnnouncementSaveReq();
        req.setTitle(title);
        req.setContent(title + " 的内容");
        req.setType(type);
        req.setStatus(statusValue);
        req.setIsPinned(pinned);
        return req;
    }

    private JsonNode findById(JsonNode pageData, String arrayField, long id) {
        for (JsonNode item : pageData.withArray(arrayField)) {
            if (item.get("id").asLong() == id) {
                return item;
            }
        }
        return null;
    }

    private int indexOfId(JsonNode array, long id) {
        for (int i = 0; i < array.size(); i++) {
            if (array.get(i).get("id").asLong() == id) {
                return i;
            }
        }
        return -1;
    }

    private JsonNode data(MockHttpServletRequestBuilder builder) throws Exception {
        return readTree(mockMvc.perform(builder)
                .andExpect(jsonPath("$.code").value(0))
                .andReturn()).get("data");
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
