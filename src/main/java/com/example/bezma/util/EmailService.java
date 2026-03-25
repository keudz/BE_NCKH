package com.example.bezma.util;

import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    // Tiêm cấu hình từ file properties/env
    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Gửi mail xác thực
     * @Async giúp phương thức chạy ở luồng khác, trả về kết quả API ngay lập tức
     */
    @Async
    public void sendVerificationEmail(String to, String token) {
        // Tự động nhận http://localhost:8080 (dev) hoặc domain thật (prod)
        String verificationUrl = String.format("%s/api/v1/tenants/public/verify?token=%s", baseUrl, token);

        String subject = "[EVENT POS] Xác thực email để kích hoạt gian hàng";
        String content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #1e40af;">Chào mừng bạn đến với EVENT POS!</h2>
                <p>Cảm ơn bạn đã đăng ký. Vui lòng click nút dưới đây để xác thực email:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background: #1e40af; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;">Xác thực ngay</a>
                </div>
                <hr>
                <p style="color: #666; font-size: 12px;">Trân trọng,<br>Đội ngũ EVENT POS</p>
            </div>
            """.formatted(verificationUrl);

        sendHtmlEmail(to, subject, content);
    }

    /**
     * Gửi tài khoản cho khách sau khi xác thực thành công
     */
    @Async
    public void sendCredentialsEmail(String to, String username, String password) {
        String loginUrl = frontendUrl + "/check-shop";
        String subject = "[EVENT POS] Tài khoản quản trị của bạn";

        String content = """
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #16a34a;">Kích hoạt thành công!</h2>
                <p>Thông tin đăng nhập của bạn:</p>
                <div style="background: #f3f4f6; padding: 16px; border-radius: 8px; margin: 20px 0;">
                    <p><strong>Username:</strong> %s</p>
                    <p><strong>Mật khẩu tạm thời:</strong> %s</p>
                </div>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background: #16a34a; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;">Đăng nhập ngay</a>
                </div>
            </div>
            """.formatted(username, password, loginUrl);

        sendHtmlEmail(to, subject, content);
    }

    private void sendHtmlEmail(String to, String subject, String content) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(to);
            helper.setSubject(subject);
            helper.setText(content, true);

            mailSender.send(message);
            log.info("Email đã gửi thành công tới: {}", to);
        } catch (Exception e) {
            log.error("Gửi email thất bại tới {}: {}", to, e.getMessage());
            // @Async chạy ngầm nên không nên throw RuntimeException làm chết thread pool
        }
    }
}