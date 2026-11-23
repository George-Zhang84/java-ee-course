package com.zhj.exp4.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 图片存储服务。
 *
 * <p>用接口隔离"存哪里"这件事：当前实现是本地磁盘，
 * 后续换成对象存储（OSS/MinIO）只需新增一个实现类并改配置，调用方零改动——这就是策略模式的用处。
 */
public interface FileStorageService {

    /**
     * 保存图片并返回可访问的相对 URL。
     *
     * @param file 上传的文件
     * @return 形如 /uploads/2025/12/21/uuid.jpg 的相对路径
     */
    String store(MultipartFile file);

    /**
     * 删除由本服务保存的图片（用于"用户在表单中移除图片"或"清理孤儿文件"）。
     *
     * <p>出于安全，只接受以配置的 urlPrefix 开头、且最终路径仍位于上传根目录内的地址；
     * 文件不存在时静默返回，不抛异常。
     *
     * @param url 上传接口返回的相对 URL
     * @return 是否真的删除了文件
     */
    boolean delete(String url);
}
