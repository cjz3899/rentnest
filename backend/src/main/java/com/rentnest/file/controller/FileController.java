package com.rentnest.file.controller;

import com.rentnest.common.api.Result;
import com.rentnest.file.dto.FileResp;
import com.rentnest.file.entity.FileRecord;
import com.rentnest.file.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.InputStreamResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileService fileService;

    @PostMapping
    public Result<FileResp> upload(@RequestParam("file") MultipartFile file,
                                   @RequestParam(value = "visibility", defaultValue = "PRIVATE") String visibility,
                                   @RequestParam(value = "dir", defaultValue = "common") String dir) {
        return Result.ok(fileService.upload(file, visibility, dir));
    }

    @GetMapping("/{uuid}")
    public ResponseEntity<InputStreamResource> download(@PathVariable String uuid) {
        FileRecord record = fileService.requireReadable(uuid);
        InputStreamResource body = new InputStreamResource(fileService.openStream(record));
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "inline; filename=\"" + record.getOriginalName() + "\"")
                .contentType(MediaType.parseMediaType(record.getMimeType()))
                .contentLength(record.getSizeBytes())
                .body(body);
    }
}
