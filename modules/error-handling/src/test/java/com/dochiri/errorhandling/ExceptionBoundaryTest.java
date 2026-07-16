package com.dochiri.errorhandling;

import com.example.member.application.exception.MemberNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExceptionBoundaryTest {

    private static final List<String> WEB_COUPLED_EXCEPTION_CONTRACTS = List.of(
            "com.dochiri.errorhandling.BaseException",
            "com.dochiri.errorhandling.ErrorCode"
    );
    private static final List<String> LEGACY_WEB_ERROR_CONTRACTS = List.of(
            "com.dochiri.errorhandling.CommonErrorCode",
            "com.dochiri.errorhandling.ProblemDetails"
    );

    @Test
    @DisplayName("Web MVC 모듈은 안쪽 계층용 공통 예외 계약을 공개하지 않는다")
    void Web_MVC_모듈은_안쪽_계층용_공통_예외_계약을_공개하지_않는다() {
        // given
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // when
        List<String> exposedContracts = WEB_COUPLED_EXCEPTION_CONTRACTS.stream()
                .filter(className -> isPresent(className, classLoader))
                .toList();

        // then
        assertThat(exposedContracts).isEmpty();
    }

    @Test
    @DisplayName("소비 Application 예외의 공개 계약은 Spring 타입을 참조하지 않는다")
    void 소비_Application_예외의_공개_계약은_Spring_타입을_참조하지_않는다() {
        // given
        Class<?> exceptionType = MemberNotFoundException.class;

        // when
        List<Class<?>> signatureTypes = signatureTypesOf(exceptionType);

        // then
        assertThat(signatureTypes)
                .extracting(Class::getName)
                .noneMatch(typeName -> typeName.startsWith("org.springframework"));
        assertThat(exceptionType.getSuperclass()).isEqualTo(RuntimeException.class);
    }

    @Test
    @DisplayName("HTTP 매핑과 사용자 메시지를 합친 기존 Web 오류 계약을 공개하지 않는다")
    void HTTP_매핑과_사용자_메시지를_합친_기존_Web_오류_계약을_공개하지_않는다() {
        // given
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();

        // when
        List<String> exposedContracts = LEGACY_WEB_ERROR_CONTRACTS.stream()
                .filter(className -> isPresent(className, classLoader))
                .toList();

        // then
        assertThat(exposedContracts).isEmpty();
    }

    private boolean isPresent(String className, ClassLoader classLoader) {
        try {
            Class.forName(className, false, classLoader);
            return true;
        } catch (ClassNotFoundException exception) {
            return false;
        }
    }

    private List<Class<?>> signatureTypesOf(Class<?> type) {
        List<Class<?>> signatureTypes = new ArrayList<>();
        signatureTypes.add(type.getSuperclass());
        for (Field field : type.getDeclaredFields()) {
            signatureTypes.add(field.getType());
        }
        for (Constructor<?> constructor : type.getDeclaredConstructors()) {
            signatureTypes.addAll(Arrays.asList(constructor.getParameterTypes()));
        }
        for (Method method : type.getDeclaredMethods()) {
            signatureTypes.add(method.getReturnType());
            signatureTypes.addAll(Arrays.asList(method.getParameterTypes()));
        }
        return List.copyOf(signatureTypes);
    }
}
