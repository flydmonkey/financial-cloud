package com.financial.cloud.exception;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.exc.InvalidFormatException;
import com.financial.cloud.common.Message;
import com.financial.cloud.exception.BusinessException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.UnexpectedTypeException;

import org.apache.commons.lang3.StringUtils;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.validation.ObjectError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.List;
import java.util.Objects;

@Slf4j
@RestControllerAdvice
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final MessageSource messageSource;

    /**
     * 缺少请求体异常处理器
     * @param e 缺少请求体异常 使用get方式请求 而实体使用@RequestBody修饰
     */
    @ExceptionHandler(HttpMessageNotReadableException.class)
    public Message<Void> parameterBodyMissingExceptionHandler(HttpMessageNotReadableException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',请求体无法解析'{}'", requestURI, e.getMessage(), e);
        String detail = e.getMostSpecificCause() != null ? e.getMostSpecificCause().getMessage() : e.getMessage();
        if (detail != null && detail.contains("Cannot map `null` into type")) {
            return new Message<>(Message.FAIL, "请求参数格式错误：数值字段不能为空");
        }
        return new Message<>(Message.FAIL, "缺少请求体或请求体无法解析");
    }

    // get请求的对象参数校验异常
    @ExceptionHandler({MissingServletRequestParameterException.class})
    public Message<Void> bindExceptionHandler(MissingServletRequestParameterException e,HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',get方式请求参数'{}'必传", requestURI, e.getMessage(),e);
        return new Message<>(Message.FAIL, "请求的对象参数校验异常");
    }

    /**
     * 请求方式不支持
     */
    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public Message<Void> handleHttpRequestMethodNotSupported(HttpRequestMethodNotSupportedException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址 '{}',不支持'{}' 请求", requestURI, e.getMethod(),e);
        return new Message<>(HttpStatus.METHOD_NOT_ALLOWED.value(),HttpStatus.METHOD_NOT_ALLOWED.getReasonPhrase());
    }

    /**
     * 参数不正确
     */
    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public Message<Void> methodArgumentTypeMismatchException(MethodArgumentTypeMismatchException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        String error = String.format("%s 应该是 %s 类型", e.getName(), e.getRequiredType().getSimpleName());
        log.error("请求地址'{}',{},参数类型不正确", requestURI,error,e);
        return new Message<>(Message.FAIL, "参数类型不正确");
    }

    /**
     * 系统异常
     */
    @ExceptionHandler(Exception.class)
    public Message<Void> handleException(Exception e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',发生系统异常.", requestURI, e);
        return new Message<>(Message.FAIL, HttpStatus.INTERNAL_SERVER_ERROR.getReasonPhrase());
    }

    /**
     * 捕获转换类型异常
     */
    @ExceptionHandler(UnexpectedTypeException.class)
    public Message<String> unexpectedTypeHandler(UnexpectedTypeException e)
    {
        log.error("类型转换错误：{}",e.getMessage(), e);
        return  new Message<>(HttpStatus.INTERNAL_SERVER_ERROR.value(),e.getMessage());
    }

    /**
     * 捕获参数校验异常
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public Message<String> methodArgumentNotValidException(MethodArgumentNotValidException e)
    {
        BindingResult bindingResult =  e.getBindingResult();
        List<ObjectError> errors = bindingResult.getAllErrors();
        log.error("参数验证异常：{}",e.getMessage(), e);
        if (!errors.isEmpty()) {
            return new Message<>(HttpStatus.BAD_REQUEST.value(), resolveValidationMessage(errors.get(0)));
        }
        return new Message<>(HttpStatus.BAD_REQUEST.value(),"MethodArgumentNotValid");
    }

    // 运行时异常
    @ExceptionHandler(RuntimeException.class)
    public Message<String> runtimeExceptionHandler(RuntimeException e, HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',捕获运行时异常'{}'", requestURI, e.getMessage(),e);
        return new Message<>(Message.FAIL, e.getMessage());
    }
    // 系统级别异常
    @ExceptionHandler(Throwable.class)
    public Message<String> throwableExceptionHandler(Throwable e,HttpServletRequest request) {
        String requestURI = request.getRequestURI();
        log.error("请求地址'{}',捕获系统级别异常'{}'", requestURI,e.getMessage(),e);
        return new Message<>(HttpStatus.INTERNAL_SERVER_ERROR.value(), e.getMessage());
    }

    /**
     * IllegalArgumentException
     */
    @ExceptionHandler(IllegalArgumentException.class)
    public Message<?> illegalArgumentException(IllegalArgumentException e)
    {
        String message = e.getMessage();
        log.error("IllegalArgumentException：{}",e.getMessage(),e);
        return  new Message<>(HttpStatus.BAD_REQUEST.value(),message);
    }

    /**
     * InvalidFormatException
     */
    @ExceptionHandler(InvalidFormatException.class)
    public Message<?> invalidFormatException(InvalidFormatException e)
    {
        String message = e.getMessage();
        log.error("InvalidFormatException：{}",e.getMessage(),e);
        if (message != null) {
            return new Message<>(HttpStatus.BAD_REQUEST.value(),message);
        }
        return new Message<>(HttpStatus.BAD_REQUEST.value(),"error");
    }

    /**
     * 自定义验证异常
     */
    @ExceptionHandler(BindException.class)
    public Message<Void> handleBindException(BindException e) {
        BindingResult bindingResult =  e.getBindingResult();
        List<ObjectError> errors = bindingResult.getAllErrors();
        log.error("参数验证异常：{}",e.getMessage(), e);
        if (!errors.isEmpty()) {
            return new Message<>(HttpStatus.BAD_REQUEST.value(), resolveValidationMessage(errors.get(0)));
        }
        return new Message<>(HttpStatus.BAD_REQUEST.value(),"MethodArgumentNotValid");
    }

    /**
     * 业务异常处理
     */
    @ExceptionHandler(BusinessException.class)
    public Message<String> handleBusinessException(BusinessException e) {
        log.error("业务异常: code={}", e.getCode(), e);
        return new Message<>(e.getCode(), e.resolveMessage());
    }

    private String resolveValidationMessage(ObjectError error) {
        String defaultMessage = error.getDefaultMessage();
        if (StringUtils.isBlank(defaultMessage)) {
            return "参数验证失败";
        }
        String code = defaultMessage;
        if (code.startsWith("{") && code.endsWith("}")) {
            code = code.substring(1, code.length() - 1);
        }
        try {
            return messageSource.getMessage(code, error.getArguments(), defaultMessage, LocaleContextHolder.getLocale());
        } catch (Exception ex) {
            return defaultMessage;
        }
    }
}
