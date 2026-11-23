package com.zhj.exp4.task;

import com.zhj.exp4.config.AppProperties;
import com.zhj.exp4.mapper.ProductImageMapper;
import com.zhj.exp4.mapper.ProductMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Stream;

/**
 * 孤儿图片清理任务。
 *
 * <p><b>为什么需要它</b>：上传接口是"先落盘、后引用"的设计——用户选中图片就立刻上传并返回 URL，
 * 表单提交时才把 URL 写进商品。这是主流做法（避免提交时再传大文件、支持即时预览），
 * 但副作用是"用户上传了图片却没提交商品"会在服务器留下无主文件。
 *
 * <p><b>清理策略</b>：只删除同时满足两个条件的文件：
 * <ol>
 *   <li>没有被任何 {@code product.cover_url} 或 {@code product_image.url} 引用；</li>
 *   <li>文件修改时间超过 24 小时（给正在编辑、还没提交的表单留出安全窗口）。</li>
 * </ol>
 * 单次删除量不做限制，但按 6 小时周期执行，避免频繁扫描。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UploadCleanupTask {

    /** 未被引用的文件保留时长：24 小时 */
    private static final long ORPHAN_TTL_MILLIS = 24L * 60 * 60 * 1000;
    /** 首次执行延迟 10 分钟，避免和应用启动抢 IO */
    private static final long INITIAL_DELAY = 10L * 60 * 1000;

    private final ProductMapper productMapper;
    private final ProductImageMapper productImageMapper;
    private final AppProperties appProperties;

    @Scheduled(initialDelay = INITIAL_DELAY, fixedDelay = 6L * 60 * 60 * 1000)
    public void cleanOrphanImages() {
        Path root = Paths.get(appProperties.getUpload().getDir()).toAbsolutePath().normalize();
        if (!Files.isDirectory(root)) {
            return;
        }
        try {
            Set<String> referenced = collectReferencedUrls();
            long cutoff = System.currentTimeMillis() - ORPHAN_TTL_MILLIS;
            int deleted = 0;
            String prefix = appProperties.getUpload().getUrlPrefix() + "/";

            try (Stream<Path> files = Files.walk(root)) {
                List<Path> candidates = files.filter(Files::isRegularFile).toList();
                for (Path path : candidates) {
                    String url = prefix + root.relativize(path).toString().replace('\\', '/');
                    if (referenced.contains(url)) {
                        continue;
                    }
                    if (Files.getLastModifiedTime(path).toMillis() > cutoff) {
                        continue;   // 太新，可能是用户正在编辑的表单
                    }
                    Files.delete(path);
                    deleted++;
                }
            }
            if (deleted > 0) {
                log.info("孤儿图片清理完成：删除 {} 个未被引用的文件", deleted);
            }
        } catch (IOException e) {
            log.error("孤儿图片清理任务异常", e);
        }
    }

    /** 收集所有仍被商品引用的图片 URL */
    private Set<String> collectReferencedUrls() {
        Set<String> referenced = new HashSet<>();
        referenced.addAll(productMapper.selectAllCovers());
        referenced.addAll(productImageMapper.selectAllUrls());
        return referenced;
    }
}
