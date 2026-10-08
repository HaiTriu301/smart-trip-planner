package com.trieu.tripplanner.service;

import static org.assertj.core.api.Assertions.assertThat;

import com.trieu.tripplanner.config.properties.AppProperties;
import com.trieu.tripplanner.provider.mail.MailMessage;
import com.trieu.tripplanner.provider.mail.MockMailProvider;
import org.junit.jupiter.api.Test;
import org.springframework.context.support.ResourceBundleMessageSource;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.spring6.SpringTemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;

/**
 * Renders the real templates with a hand-built Thymeleaf engine (no Spring), so a broken template or a wrong
 * link fails here in milliseconds. @Async is a Spring proxy feature and does not apply to a plain `new`.
 */
class MailServiceTest {

    private final MockMailProvider mailProvider = new MockMailProvider();
    private final MailService mailService = new MailService(templateEngine(), mailProvider, messageSource(), properties());

    @Test
    void verificationMailContainsGreetingAndFrontendLinkWithToken() {
        mailService.sendVerificationMail("an@example.com", "Nguyễn An", "raw-token-123");

        assertThat(mailProvider.sent()).hasSize(1);
        MailMessage mail = mailProvider.sent().get(0);
        assertThat(mail.to()).isEqualTo("an@example.com");
        assertThat(mail.subject()).isEqualTo("Xác thực email cho Smart Trip Planner");
        assertThat(mail.htmlBody())
                .contains("Nguyễn An")
                .contains("http://localhost:5173/verify-email?token=raw-token-123")
                .contains("24 giờ")
                .doesNotContain("${");   // every Thymeleaf expression was evaluated
    }

    @Test
    void passwordResetMailUsesItsOwnTemplateAndPath() {
        mailService.sendPasswordResetMail("an@example.com", "Nguyễn An", "reset-456");

        MailMessage mail = mailProvider.sent().get(0);
        assertThat(mail.subject()).isEqualTo("Đặt lại mật khẩu Smart Trip Planner");
        assertThat(mail.htmlBody())
                .contains("http://localhost:5173/reset-password?token=reset-456")
                .contains("1 giờ")
                .doesNotContain("verify-email");
    }

    @Test
    void invitationMailNamesInviterTripAndRoleAndLinksToTheInvitePage() {
        mailService.sendInvitationMail("friend@example.com", "Nguyễn An", "Đà Lạt mùa hoa", true, 42L, "invite-789");

        assertThat(mailProvider.sent()).hasSize(1);
        MailMessage mail = mailProvider.sent().get(0);
        assertThat(mail.to()).isEqualTo("friend@example.com");
        assertThat(mail.subject()).isEqualTo("Nguyễn An mời bạn cùng lên kế hoạch cho chuyến đi \"Đà Lạt mùa hoa\"");
        assertThat(mail.htmlBody())
                .contains("Nguyễn An")
                .contains("Đà Lạt mùa hoa")
                .contains("cùng chỉnh sửa")
                // Two query parameters: HTML writes the "&" between them as "&amp;", the mail client reads "&" back
                .contains("http://localhost:5173/invite?trip=42&amp;token=invite-789")
                .doesNotContain("trip=42&token")
                .contains("7 ngày")
                .doesNotContain("${");
    }

    @Test
    void viewerInvitationSaysViewOnly() {
        mailService.sendInvitationMail("friend@example.com", "Nguyễn An", "Huế 2 ngày", false, 7L, "invite-000");

        assertThat(mailProvider.sent().get(0).htmlBody())
                .contains("chỉ xem")
                .doesNotContain("cùng chỉnh sửa");
    }

    @Test
    void invitationMailEscapesHtmlInTheTripTitle() {
        // The title is typed by the owner; a tag in it must show as text, not run in the invitee's mail client
        mailService.sendInvitationMail("friend@example.com", "Nguyễn An", "<script>alert(1)</script>", false, 7L, "t");

        assertThat(mailProvider.sent().get(0).htmlBody())
                .doesNotContain("<script>")
                .contains("&lt;script&gt;");
    }

    private static TemplateEngine templateEngine() {
        // Same resolution Spring Boot configures: classpath:/templates/<name>.html, HTML mode, UTF-8.
        // SpringTemplateEngine evaluates ${...} with SpEL; the plain TemplateEngine would need OGNL, which Boot excludes.
        ClassLoaderTemplateResolver resolver = new ClassLoaderTemplateResolver();
        resolver.setPrefix("templates/");
        resolver.setSuffix(".html");
        resolver.setTemplateMode(TemplateMode.HTML);
        resolver.setCharacterEncoding("UTF-8");
        SpringTemplateEngine engine = new SpringTemplateEngine();
        engine.setTemplateResolver(resolver);
        return engine;
    }

    private static ResourceBundleMessageSource messageSource() {
        ResourceBundleMessageSource source = new ResourceBundleMessageSource();
        source.setBasename("messages");
        source.setDefaultEncoding("UTF-8");
        source.setFallbackToSystemLocale(false);
        return source;
    }

    private static AppProperties properties() {
        return new AppProperties("http://localhost:5173", "no-reply@test.local",
                new AppProperties.Providers("mock", "mock", "mock", "mock", "local", "mock"));
    }

}
