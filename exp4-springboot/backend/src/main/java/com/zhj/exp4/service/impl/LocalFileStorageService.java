package com.zhj.exp4.service.impl;

import com.zhj.exp4.common.BizException;
import com.zhj.exp4.common.ResultCode;
import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

/**
 * 本地磁盘图片存储实现。
 *
 * <p>上传是最容易被攻击的入口，这里做了五道校验：
 * <ol>
 *   <li><b>非空与大小</b>：空文件拒绝，超过配置上限（默认 5MB）拒绝；</li>
 *   <li><b>扩展名白名单</b>：只允许 jpg/jpeg/png/webp/gif，拒绝 jsp/php/exe 等可执行类型；</li>
 *   <li><b>MIME 校验</b>：Content-Type 必须是 image/*，双重保险；</li>
 *   <li><b>文件名随机化</b>：完全丢弃原始文件名，杜绝 {@code ../../} 路径穿越与同名覆盖；</li>
 *   <li><b>按日期分目录</b>：避免单目录文件过多，也便于按时间清理。</li>
 * </ol>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LocalFileStorageService implements FileStorageService {

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final DateTimeFormatter DATE_DIR = DateTimeFormatter.ofPattern("yyyy/MM/dd");

    private final AppProperties appProperties;

    @Override
    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BizException(ResultCode.FILE_ERROR, "请选择要上传的图片");
        }
        long maxBytes = appProperties.getUpload().getMaxSizeMb() * 1024L * 1024L;
        if (file.getSize() > maxBytes) {
            throw new BizException(ResultCode.FILE_ERROR,
                    "图片不能超过 " + appProperties.getUpload().getMaxSizeMb() + "MB");
        }
        String extension = extractExtension(file.getOriginalFilename());
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BizException(ResultCode.FILE_ERROR, "仅支持 jpg/jpeg/png/webp/gif 格式");
        }
        String contentType = file.getContentType();
        if (contentType == null || !contentType.toLowerCase(Locale.ROOT).startsWith("image/")) {
            throw new BizException(ResultCode.FILE_ERROR, "文件类型不正确");
        }

        String dateDir = LocalDate.now().format(DATE_DIR);
        String fileName = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = Paths.get(appProperties.getUpload().getDir(), dateDir).toAbsolutePath().normalize();
        Path target = targetDir.resolve(fileName).normalize();

        // 防御性校验：最终路径必须仍位于上传根目录之内
        Path root = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        if (!target.startsWith(root)) {
            throw new BizException(ResultCode.FILE_ERROR, "非法的文件路径");
        }
        try {
            Files.createDirectories(targetDir);
            file.transferTo(target.toFile());
        } catch (IOException e) {
            log.error("图片保存失败 target={}", target, e);
            throw new BizException(ResultCode.FILE_ERROR, "图片保存失败，请稍后重试");
        }
        String url = appProperties.getUpload().getUrlPrefix() + "/" + dateDir + "/" + fileName;
        log.info("图片上传成功 url={} size={}B", url, file.getSize());
        return url;
    }

    /** 安全提取扩展名：清理路径、转小写，无扩展名返回空串 */
    private String extractExtension(String originalFilename) {
        String cleaned = StringUtils.cleanPath(originalFilename == null ? "" : originalFilename);
        int dot = cleaned.lastIndexOf('.');
        if (dot < 0 || dot == cleaned.length() - 1) {
            return "";
        }
        return cleaned.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    @Override
    public boolean delete(String url) {
        if (!StringUtils.hasText(url)) {
            return false;
        }
        String prefix = appProperties.getUpload().getUrlPrefix() + "/";
        if (!url.startsWith(prefix)) {
            // 只允许删除本站上传目录下的文件，杜绝 ../ 之类的路径穿越
            return false;
        }
        Path root = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        Path target = root.resolve(url.substring(prefix.length())).normalize();
        if (!target.startsWith(root)) {
            log.warn("拒绝删除越界路径：{}", url);
            return false;
        }
        try {
            boolean deleted = Files.deleteIfExists(target);
            if (deleted) {
                log.info("删除图片 {} ", url);
            }
            return deleted;
        } catch (IOException e) {
            log.warn("删除图片失败 {} : {}", url, e.getMessage());
            return false;
        }
    }
}
