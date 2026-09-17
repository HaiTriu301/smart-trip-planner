package com.trieu.tripplanner.exception;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.common.constant.ErrorCode;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Valid;
import jakarta.validation.Validator;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.assertj.MockMvcTester;
import org.springframework.test.web.servlet.assertj.MvcTestResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@WebMvcTest(controllers = GlobalExceptionHandlerTest.ThrowingController.class)
@Import(GlobalExceptionHandlerTest.ThrowingController.class)
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvcTester mvc;

    @Test
    void appExceptionUsesStatusAndMessageOfItsErrorCode() {
        MvcTestResult result = mvc.get().uri("/test/not-found").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "RESOURCE_NOT_FOUND",
                          "message": "Không tìm thấy dữ liệu yêu cầu",
                          "path": "/test/not-found"
                        }
                        """);
        assertThat(result).bodyJson().doesNotHavePath("$.details");
        assertThat(result).bodyJson().hasPathSatisfying("$.timestamp", timestamp -> assertThat(timestamp).isNotNull());
    }

    @Test
    void businessRuleExceptionKeepsItsOwnErrorCode() {
        assertThat(mvc.get().uri("/test/business-rule"))
                .hasStatus(HttpStatus.CONFLICT)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "ACTIVITY_TIME_CONFLICT",
                          "message": "Hoạt động bị trùng giờ với một hoạt động khác trong ngày"
                        }
                        """);
    }

    @Test
    void invalidRequestBodyReturnsFieldDetails() {
        MvcTestResult result = mvc.post().uri("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                        {"name": "", "days": 0}
                        """)
                .exchange();

        assertThat(result)
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "VALIDATION_ERROR",
                          "message": "Dữ liệu không hợp lệ",
                          "details": [ { "field": "days" }, { "field": "name" } ]
                        }
                        """);
    }

    @Test
    void malformedJsonReturnsValidationError() {
        assertThat(mvc.post().uri("/test/body")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"name\": "))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "message": "Nội dung yêu cầu không đúng định dạng JSON hoặc sai kiểu dữ liệu"
                        }
                        """);
    }

    @Test
    void invalidRequestParamReturnsFieldDetails() {
        assertThat(mvc.get().uri("/test/param?size=0"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "size" } ]
                        }
                        """);
    }

    @Test
    void pathVariableOfWrongTypeReturnsValidationError() {
        assertThat(mvc.get().uri("/test/items/abc"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "message": "Giá trị của tham số 'id' không hợp lệ"
                        }
                        """);
    }

    @Test
    void constraintViolationFromServiceLayerReturnsFieldDetails() {
        assertThat(mvc.get().uri("/test/service-validation"))
                .hasStatus(HttpStatus.BAD_REQUEST)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "VALIDATION_ERROR",
                          "details": [ { "field": "days" }, { "field": "name" } ]
                        }
                        """);
    }

    @Test
    void unknownUrlReturnsResourceNotFound() {
        assertThat(mvc.get().uri("/test/khong-ton-tai"))
                .hasStatus(HttpStatus.NOT_FOUND)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "errorCode": "RESOURCE_NOT_FOUND",
                          "path": "/test/khong-ton-tai"
                        }
                        """);
    }

    @Test
    void unsupportedHttpMethodReturnsMethodNotAllowedWithAllowHeader() {
        MvcTestResult result = mvc.delete().uri("/test/not-found").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.METHOD_NOT_ALLOWED)
                .hasHeader(HttpHeaders.ALLOW, "GET")
                .bodyJson().extractingPath("$.errorCode").isEqualTo("METHOD_NOT_ALLOWED");
    }

    @Test
    void unexpectedExceptionReturnsGenericMessageWithoutInternalDetails() {
        MvcTestResult result = mvc.get().uri("/test/unexpected").exchange();

        assertThat(result)
                .hasStatus(HttpStatus.INTERNAL_SERVER_ERROR)
                .bodyJson().isLenientlyEqualTo("""
                        {
                          "success": false,
                          "errorCode": "INTERNAL_ERROR",
                          "message": "Đã có lỗi xảy ra, vui lòng thử lại sau"
                        }
                        """);
        assertThat(result).body().asString().doesNotContain("SECRET_INTERNAL_DETAIL");
    }

    record SampleRequest(@NotBlank String name, @Min(1) int days) {
    }

    /**
     * Test-only controller that triggers each exception type handled by GlobalExceptionHandler.
     */
    @RestController
    @RequestMapping("/test")
    static class ThrowingController {

        private final Validator validator;

        ThrowingController(Validator validator) {
            this.validator = validator;
        }

        @GetMapping("/not-found")
        void notFound() {
            throw new ResourceNotFoundException("Trip", 42L);
        }

        @GetMapping("/business-rule")
        void businessRule() {
            throw new BusinessRuleException(ErrorCode.ACTIVITY_TIME_CONFLICT, "Activity 1 overlaps activity 2");
        }

        @PostMapping("/body")
        void body(@Valid @RequestBody SampleRequest request) {
        }

        @GetMapping("/param")
        void param(@RequestParam @Min(1) int size) {
        }

        @GetMapping("/items/{id}")
        void item(@PathVariable Long id) {
        }

        @GetMapping("/service-validation")
        void serviceValidation() {
            // Same exception a @Validated service throws when its method arguments are invalid
            throw new ConstraintViolationException(validator.validate(new SampleRequest("", 0)));
        }

        @GetMapping("/unexpected")
        void unexpected() {
            throw new IllegalStateException("SECRET_INTERNAL_DETAIL");
        }

    }

}
