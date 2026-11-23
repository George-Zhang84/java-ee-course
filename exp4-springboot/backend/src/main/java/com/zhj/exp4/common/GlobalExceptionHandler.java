package com.zhj.exp4.common;

import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.validation.BindException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理器。
 *
 * <p>目标：无论异常来自参数校验、业务规则还是未捕获的运行时异常，
 * 前端收到的响应结构始终是 {@link Result}，不需要为每种错误写不同解析逻辑。
 *
 * <p>异常分三类处理：
 * <ol>
 *   <li>校验类（1xxx）：把字段名和原因拼进 message，前端可直接提示；</li>
 *   <li>业务类（2xxx）：{@link BizException} 自带 code 与可读 message，原样返回；</li>
 *   <li>兜底类（5000）：记录 error 日志（含 traceId），但绝不把堆栈返回给前端。</li>
 * </ol>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** 业务异常：可预期，不打 error 日志，避免污染告警 */
    @ExceptionHandler(BizException.class)
    public Result<Void> handleBiz(BizException e) {
        log.warn("业务异常 code={} msg={}", e.getCode(), e.getMessage());
        return Result.fail(e.getCode(), e.getMessage());
    }

    /** @RequestBody 上的 @Valid 校验失败 */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Result<Void> handleMethodArgumentNotValid(MethodArgumentNotValidException e) {
        String msg = e.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    /** 表单/查询参数绑定校验失败 */
    @ExceptionHandler(BindException.class)
    public Result<Void> handleBind(BindException e) {
        String msg = e.getFieldErrors().stream()
                .map(this::formatFieldError)
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    /** 方法参数上的 @Validated 约束失败（如 @Min 直接标在参数上） */
    @ExceptionHandler(ConstraintViolationException.class)
    public Result<Void> handleConstraintViolation(ConstraintViolationException e) {
        String msg = e.getConstraintViolations().stream()
                .map(v -> v.getPropertyPath() + " " + v.getMessage())
                .collect(Collectors.joining("；"));
        return Result.fail(ResultCode.PARAM_ERROR, msg);
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public Result<Void> handleMissingParam(MissingServletRequestParameterException e) {
        return Result.fail(ResultCode.PARAM_ERROR, "缺少必要参数：" + e.getParameterName());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Result<Void> handleTypeMismatch(MethodArgumentTypeMismatchException e) {
        return Result.fail(ResultCode.PARAM_ERROR, "参数 " + e.getName() + " 类型不正确");
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Result<Void> handleNotReadable(HttpMessageNotReadableException e) {
        return Result.fail(ResultCode.PARAM_ERROR, "请求体格式错误");
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public Result<Void> handleMaxUpload(MaxUploadSizeExceededException e) {
        return Result.fail(ResultCode.FILE_ERROR, "文件过大，单个文件不能超过 5MB");
    }

    /**
     * 静态资源不存在（例如图片已被删除、前端请求了不存在的文件）。
     *
     * <p>Spring 6.1 对未匹配的静态资源抛 {@link NoResourceFoundException}，若落到兜底
     * Exception 处理器会返回 HTTP 200 + 业务码 5000，导致"文件删了但访问仍返回 200"的假象；
     * 这里明确返回 404。
     */
    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public Result<Void> handleNoResource(NoResourceFoundException e) {
        return Result.fail(ResultCode.NOT_FOUND, "资源不存在");
    }

    /** 方法级权限校验失败（@PreAuthorize 拒绝） */
    @ExceptionHandler(AccessDeniedException.class)
    public Result<Void> handleAccessDenied(AccessDeniedException e) {
        log.warn("越权访问被拒绝：{}", e.getMessage());
        return Result.fail(ResultCode.FORBIDDEN);
    }

    /** 兜底：未预期异常，必须记录完整堆栈 */
    @ExceptionHandler(Exception.class)
    public Result<Void> handleUnexpected(Exception e) {
        log.error("系统异常 traceId={}", TraceIdHolder.current(), e);
        return Result.fail(ResultCode.SYSTEM_ERROR);
    }

    private String formatFieldError(FieldError error) {
        return error.getField() + " " + error.getDefaultMessage();
    }
}
