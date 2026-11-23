package com.zhj.exp4.controller;

import com.zhj.exp4.aspect.Audit;
import com.zhj.exp4.common.Result;
import com.zhj.exp4.service.FileStorageService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

/**
 * 图片上传接口。
 *
 * <p>返回的是相对 URL（{@code /uploads/2025/12/21/xxx.jpg}），
 * 前端直接用于 img 标签；部署换域名或加 CDN 时只需改 nginx，不动数据库。
 */
@Tag(name = "05. 上传", description = "商品图片上传")
@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class UploadController {

    private final FileStorageService fileStorageService;

    @Operation(summary = "上传单张图片（≤5MB，jpg/jpeg/png/webp/gif）")
    @Audit(module = "文件", action = "上传图片", saveParams = false)
    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Result<Map<String, String>> uploadImage(@RequestParam("file") MultipartFile file) {
        String url = fileStorageService.store(file);
        return Result.ok("上传成功", Map.of("url", url));
    }

    /**
     * 删除图片：用户在发布表单里移除图片、或直接放弃发布时调用，避免服务器留下孤儿文件。
     * 只允许删除 /uploads/ 下的文件，且不会影响已被商品引用的图片（引用关系由调用方保证）。
     */
    @Operation(summary = "删除图片（释放未使用的上传文件）")
    @DeleteMapping("/image")
    public Result<Map<String, Boolean>> deleteImage(@RequestParam("url") String url) {
        boolean deleted = fileStorageService.delete(url);
        return Result.ok("已删除", Map.of("deleted", deleted));
    }
}
