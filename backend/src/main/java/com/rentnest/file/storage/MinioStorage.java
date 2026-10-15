package com.rentnest.file.storage;

import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.exception.BizException;
import io.minio.BucketExistsArgs;
import io.minio.GetObjectArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.RemoveObjectArgs;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.InputStream;

@Slf4j
@Component
public class MinioStorage {
    private final MinioClient client;
    private final String endpoint;
    private final String bucket;

    public MinioStorage(@Value("${rentnest.file.minio.endpoint}") String endpoint,
                        @Value("${rentnest.file.minio.access-key}") String accessKey,
                        @Value("${rentnest.file.minio.secret-key}") String secretKey,
                        @Value("${rentnest.file.minio.bucket}") String bucket) {
        this.client = MinioClient.builder()
                .endpoint(endpoint)
                .credentials(accessKey, secretKey)
                .build();
        this.endpoint = endpoint;
        this.bucket = bucket;
    }

    @PostConstruct
    void init() {
        try {
            if (!client.bucketExists(BucketExistsArgs.builder().bucket(bucket).build())) {
                client.makeBucket(MakeBucketArgs.builder().bucket(bucket).build());
                log.info("MinIO bucket 不存在，已自动创建: {}", bucket);
            }
            log.info("MinioStorage ready, endpoint={}, bucket={}", endpoint, bucket);
        } catch (Exception e) {
            throw new IllegalStateException("MinIO 初始化失败（endpoint=" + endpoint + "），请检查 .env 中 MINIO_* 配置是否正确且服务已启动", e);
        }
    }

    public void store(String objectKey, InputStream in, long size, String contentType) {
        try {
            client.putObject(PutObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .stream(in, size, -1)
                    .contentType(contentType)
                    .build());
        } catch (Exception e) {
            log.error("minio putObject failed, key={}", objectKey, e);
            throw new BizException(ErrorCode.BIZ_ERROR, "文件保存失败");
        }
    }

    public InputStream open(String objectKey) {
        try {
            return client.getObject(GetObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("minio getObject failed, key={}", objectKey, e);
            throw new BizException(ErrorCode.BIZ_ERROR, "文件读取失败");
        }
    }

    public void delete(String objectKey) {
        try {
            client.removeObject(RemoveObjectArgs.builder()
                    .bucket(bucket)
                    .object(objectKey)
                    .build());
        } catch (Exception e) {
            log.error("minio removeObject failed, key={}", objectKey, e);
        }
    }
}
