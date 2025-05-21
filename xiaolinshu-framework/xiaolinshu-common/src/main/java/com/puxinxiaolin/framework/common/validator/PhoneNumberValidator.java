package com.puxinxiaolin.framework.common.validator;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * @Description: 手机号校验器, 第一个参数为自定义注解, 第二个参数为需要校验的参数类型
 * @Author: YCcLin
 * @Date: 2025/5/21 15:40
 */
public class PhoneNumberValidator implements ConstraintValidator<PhoneNumber, String> {

    @Override
    public boolean isValid(String phoneNumber, ConstraintValidatorContext context) {
        return phoneNumber != null && phoneNumber.matches("^1[3-9]\\d{9}$");
    }

    @Override
    public void initialize(PhoneNumber constraintAnnotation) {

    }

}
