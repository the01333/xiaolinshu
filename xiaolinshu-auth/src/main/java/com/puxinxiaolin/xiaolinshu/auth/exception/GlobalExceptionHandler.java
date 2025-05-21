package com.puxinxiaolin.xiaolinshu.auth.exception;

import com.puxinxiaolin.framework.common.exception.BizException;
import com.puxinxiaolin.framework.common.response.Response;
import com.puxinxiaolin.xiaolinshu.auth.enums.ResponseCodeEnum;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.Optional;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    /**
     * 捕获自定义业务异常
     *
     * @param request
     * @param ex
     * @return
     */
    @ResponseBody
    @ExceptionHandler({BizException.class})
    public Response<Object> handleBizException(HttpServletRequest request, BizException ex) {
        logError(request, ex.getErrorCode(), ex.getErrorMessage());
        return Response.fail(ex);
    }

    /**
     * 捕获 Guava 参数校验异常
     *
     * @param request
     * @param ex
     * @return
     */
    @ResponseBody
    @ExceptionHandler({IllegalArgumentException.class})
    public Response<Object> handleIllegalArgumentException(HttpServletRequest request, IllegalArgumentException ex) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();
        String errorMessage = ex.getMessage();

        logError(request, errorCode, errorMessage);

        return Response.fail(errorCode, errorMessage);
    }

    /**
     * 捕获参数校验异常
     *
     * @param request
     * @param ex
     * @return
     */
    @ResponseBody
    @ExceptionHandler({MethodArgumentNotValidException.class})
    public Response<Object> handleMethodArgumentNotValidException(HttpServletRequest request, MethodArgumentNotValidException ex) {
        String errorCode = ResponseCodeEnum.PARAM_NOT_VALID.getErrorCode();

        BindingResult bindingResult = ex.getBindingResult();
        StringBuilder sb = new StringBuilder();
        // 获取校验不通过的字段，并组合错误信息，格式为： email 邮箱格式不正确, 当前值: '123124qq.com';
        Optional.of(bindingResult.getFieldErrors()).ifPresent(errors ->
                errors.forEach(error ->
                        sb.append(error.getField()).append(" ")
                                .append(error.getDefaultMessage())
                                .append(", 当前值: '")
                                .append(error.getRejectedValue())
                                .append("';")
                )
        );
        String errorMessage = sb.toString();

        logError(request, errorCode, errorMessage);
        return Response.fail(errorCode, errorMessage);
    }

    /**
     * 其他类型异常
     *
     * @param request
     * @param e
     * @return
     */
    @ExceptionHandler({Exception.class})
    @ResponseBody
    public Response<Object> handleOtherException(HttpServletRequest request, Exception e) {
        log.error("{} request error, ", request.getRequestURI(), e);
        return Response.fail(ResponseCodeEnum.SYSTEM_ERROR);
    }

    /**
     * 统一记录错误日志
     *
     * @param request
     * @param errorCode
     * @param errorMessage
     */
    private static void logError(HttpServletRequest request, String errorCode, String errorMessage) {
        log.warn("{} request fail, errorCode: {}, errorMessage: {}",
                request.getRequestURI(), errorCode, errorMessage);
    }

}
