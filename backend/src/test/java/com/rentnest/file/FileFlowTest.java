package com.rentnest.file;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 文件上传/下载权限测试：真连本机 MySQL(rentnest_test) / MinIO，不做 mock。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileFlowTest {
    private static final String LANDLORD_PHONE = "13800000002";
    private static final String USER_PHONE = "13800000003";

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("PUBLIC 文件游客可直接读取，PRIVATE 文件需归属或管理员")
    void publicReadablePrivateGuarded() throws Exception {
        String landlord = login(LANDLORD_PHONE);
        String user = login(USER_PHONE);

        String publicUuid = upload(landlord, "PUBLIC");
        mockMvc.perform(get("/api/files/" + publicUuid))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.IMAGE_JPEG));

        String privateUuid = upload(landlord, "PRIVATE");
        // 游客与无关用户都不能读私有文件
        mockMvc.perform(get("/api/files/" + privateUuid))
                .andExpect(jsonPath("$.code").value(40300));
        mockMvc.perform(get("/api/files/" + privateUuid)
                        .header("Authorization", "Bearer " + user))
                .andExpect(jsonPath("$.code").value(40300));
        // owner 可读
        mockMvc.perform(get("/api/files/" + privateUuid)
                        .header("Authorization", "Bearer " + landlord))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("上传需登录，伪装扩展名与未知类型被拒绝")
    void uploadValidations() throws Exception {
        String user = login(USER_PHONE);

        mockMvc.perform(multipart("/api/files")
                        .file(jpeg("real.jpg")))
                .andExpect(status().isUnauthorized());

        // 内容是文本却叫 .png —— magic bytes 校验应拦下
        mockMvc.perform(multipart("/api/files")
                        .file(new MockMultipartFile("file", "evil.png", "image/png",
                                "这不是图片".getBytes()))
                        .header("Authorization", "Bearer " + user))
                .andExpect(jsonPath("$.code").value(40000));

        mockMvc.perform(multipart("/api/files")
                        .file(new MockMultipartFile("file", "shell.sh", "text/plain",
                                "echo hi".getBytes()))
                        .header("Authorization", "Bearer " + user))
                .andExpect(jsonPath("$.code").value(40000));
    }

    @Test
    @DisplayName("不存在的文件返回 404 业务码")
    void missingFileReturnsNotFound() throws Exception {
        mockMvc.perform(get("/api/files/" + java.util.UUID.randomUUID()))
                .andExpect(jsonPath("$.code").value(40400));
    }

    // ---------- helpers ----------

    private MockMultipartFile jpeg(String filename) throws Exception {
        return new MockMultipartFile("file", filename, "image/jpeg",
                new ClassPathResource("test-house.jpg").getInputStream());
    }

    private String login(String phone) throws Exception {
        JsonNode body = readTree(mockMvc.perform(post("/api/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "phone", phone, "password", "123456")))).andReturn());
        return body.get("data").get("token").asText();
    }

    private String upload(String token, String visibility) throws Exception {
        JsonNode body = readTree(mockMvc.perform(multipart("/api/files")
                        .file(jpeg("test-house.jpg"))
                        .param("visibility", visibility)
                        .param("dir", "test")
                        .header("Authorization", "Bearer " + token))
                .andExpect(jsonPath("$.code").value(0))
                .andReturn());
        return body.get("data").get("uuid").asText();
    }

    private JsonNode readTree(MvcResult result) throws Exception {
        return objectMapper.readTree(result.getResponse().getContentAsString());
    }
}
