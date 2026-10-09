package com.rentnest.file.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.rentnest.common.api.ErrorCode;
import com.rentnest.common.auth.CurrentUser;
import com.rentnest.common.auth.UserContextHolder;
import com.rentnest.common.exception.BizException;
import com.rentnest.file.dto.FileResp;
import com.rentnest.file.entity.FileRecord;
import com.rentnest.file.mapper.FileRecordMapper;
import com.rentnest.file.storage.MinioStorage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HexFormat;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileService {
    private final FileRecordMapper fileRecordMapper;
    private final MinioStorage minioStorage;

    @Value("${rentnest.file.max-size-mb:20}")
    private long maxSizeMb;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of(
            "jpg", "jpeg", "png", "gif", "webp", "mp4", "pdf", "txt", "md", "docx");

    public FileResp upload(MultipartFile file, String visibility, String subDir) {
        String originalName = file.getOriginalFilename();
        if (originalName == null || originalName.isBlank()) {
            throw new BizException(ErrorCode.PARAM_ERROR, "缺少文件名");
        }
        String ext = extOf(originalName);
        if (!ALLOWED_EXTENSIONS.contains(ext)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "不支持的文件类型: " + ext);
        }
        if (file.getSize() > maxSizeMb * 1024 * 1024) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件超过大小限制 " + maxSizeMb + "MB");
        }

        CurrentUser user = UserContextHolder.require();
        String normalizedVisibility = FileRecord.VISIBILITY_PRIVATE.equals(visibility)
                ? FileRecord.VISIBILITY_PRIVATE : FileRecord.VISIBILITY_PUBLIC;

        try (InputStream in = file.getInputStream()) {
            byte[] head = in.readNBytes(16);
            validateMagicBytes(ext, head);
        } catch (IOException e) {
            throw new BizException(ErrorCode.BIZ_ERROR, "读取上传文件失败");
        }

        String uuid = UUID.randomUUID().toString();
        String objectKey = subDir + "/"
                + LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy/MM"))
                + "/" + uuid + "." + ext;
        String mimeType = safeMimeType(file.getContentType(), ext);

        minioStorage.store(objectKey, new MultipartInputStream(file), file.getSize(), mimeType);

        FileRecord record = new FileRecord();
        record.setUuid(uuid);
        record.setOriginalName(originalName);
        record.setMimeType(mimeType);
        record.setSizeBytes(file.getSize());
        record.setSha256(sha256(file));
        record.setStorageType(FileRecord.STORAGE_MINIO);
        record.setStoragePath(objectKey);
        record.setVisibility(normalizedVisibility);
        record.setOwnerId(user.getId());
        fileRecordMapper.insert(record);

        return toResp(record);
    }

    public FileRecord requireReadable(String uuid) {
        FileRecord record = fileRecordMapper.selectOne(
                new LambdaQueryWrapper<FileRecord>().eq(FileRecord::getUuid, uuid));
        if (record == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "文件不存在");
        }
        if (FileRecord.VISIBILITY_PRIVATE.equals(record.getVisibility())) {
            CurrentUser user = UserContextHolder.get();
            if (user == null || (!user.isAdmin() && !user.getId().equals(record.getOwnerId()))) {
                throw new BizException(ErrorCode.FORBIDDEN, "无权访问该文件");
            }
        }
        return record;
    }

    public InputStream openStream(FileRecord record) {
        return minioStorage.open(record.getStoragePath());
    }

    public void deleteQuietly(FileRecord record) {
        minioStorage.delete(record.getStoragePath());
        fileRecordMapper.deleteById(record.getId());
    }

    private void validateMagicBytes(String ext, byte[] head) {
        String hex = HexFormat.of().formatHex(head).toUpperCase(Locale.ROOT);
        boolean ok = switch (ext) {
            case "jpg", "jpeg" -> hex.startsWith("FFD8FF");
            case "png" -> hex.startsWith("89504E47");
            case "gif" -> hex.startsWith("47494638");
            case "webp" -> hex.startsWith("52494646");
            case "mp4" -> hex.startsWith("000000") || hex.contains("66747970");
            case "pdf" -> hex.startsWith("25504446");
            case "txt", "md" -> true;
            case "docx" -> hex.startsWith("504B0304");
            default -> false;
        };
        if (!ok) {
            throw new BizException(ErrorCode.PARAM_ERROR, "文件内容与扩展名不符");
        }
    }

    private String sha256(MultipartFile file) {
        try (InputStream in = file.getInputStream()) {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] buffer = new byte[8192];
            int n;
            while ((n = in.read(buffer)) > 0) {
                digest.update(buffer, 0, n);
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (Exception e) {
            throw new BizException(ErrorCode.BIZ_ERROR, "文件摘要计算失败");
        }
    }

    private String safeMimeType(String contentType, String ext) {
        if (contentType != null && !contentType.isBlank()) {
            return contentType;
        }
        return switch (ext) {
            case "jpg", "jpeg" -> "image/jpeg";
            case "png" -> "image/png";
            case "gif" -> "image/gif";
            case "webp" -> "image/webp";
            case "mp4" -> "video/mp4";
            case "pdf" -> "application/pdf";
            case "txt" -> "text/plain";
            case "md" -> "text/markdown";
            case "docx" -> "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
            default -> "application/octet-stream";
        };
    }

    private String extOf(String name) {
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return "";
        }
        return name.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private FileResp toResp(FileRecord record) {
        return new FileResp(record.getUuid(), record.getOriginalName(), record.getMimeType(),
                record.getSizeBytes(), record.getVisibility(), "/api/files/" + record.getUuid());
    }

    /** MultipartFile 流只能读一次，包一层保证 getInputStream 可重复调用 */
    private static class MultipartInputStream extends InputStream {
        private final MultipartFile file;
        private InputStream delegate;

        private MultipartInputStream(MultipartFile file) {
            this.file = file;
        }

        @Override
        public int read() throws IOException {
            return delegate().read();
        }

        @Override
        public int read(byte[] b, int off, int len) throws IOException {
            return delegate().read(b, off, len);
        }

        private InputStream delegate() throws IOException {
            if (delegate == null) {
                delegate = file.getInputStream();
            }
            return delegate;
        }
    }
}
