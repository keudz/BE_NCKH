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

    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${app.frontend-url}")
    private String frontendUrl;

    /**
     * Gửi mail xác thực đăng ký Tenant
     */
    @Async
    public void sendVerificationEmail(String to, String token) {
        String verificationUrl = String.format("%s/api/v1/tenants/public/verify?token=%s", baseUrl, token);
        String subject = "[LaptopHN POS] Xác thực email để kích hoạt gian hàng";
        
        String content = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #1e40af;">Chào mừng bạn đến với LaptopHN POS!</h2>
                <p>Cảm ơn bạn đã đăng ký. Vui lòng click nút dưới đây để xác thực email:</p>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background: #1e40af; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;">Xác thực ngay</a>
                </div>
                <hr>
                <p style="color: #666; font-size: 12px;">Trân trọng,<br>Đội ngũ LaptopHN POS</p>
            </div>
            """, verificationUrl);

        sendHtmlEmail(to, subject, content);
    }

    /**
     * Gửi tài khoản Admin sau khi xác thực thành công
     */
    @Async
    public void sendCredentialsEmail(String to, String username, String password) {
        String loginUrl = frontendUrl + "/login";
        String subject = "[LaptopHN POS] Tài khoản quản trị của bạn";

        String content = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #16a34a;">Kích hoạt thành công!</h2>
                <p>Thông tin đăng nhập của bạn:</p>
                <div style="background: #f3f4f6; padding: 16px; border-radius: 8px; margin: 20px 0;">
                    <p><strong>Tên đăng nhập:</strong> %s</p>
                    <p><strong>Mật khẩu tạm thời:</strong> %s</p>
                </div>
                <div style="text-align: center; margin: 30px 0;">
                    <a href="%s" style="background: #16a34a; color: white; padding: 14px 28px; text-decoration: none; border-radius: 8px; font-weight: bold;">Đăng nhập ngay</a>
                </div>
                <p style="color: #ef4444; font-weight: bold;">Lưu ý: Bạn sẽ được yêu cầu đổi mật khẩu trong lần đăng nhập đầu tiên.</p>
            </div>
            """, username, password, loginUrl);

        sendHtmlEmail(to, subject, content);
    }

    /**
     * Gửi mã OTP xác thực kép (2FA)
     */
    @Async
    public void sendTwoStepResetEmail(String to, String recipientRole, String otp) {
        String subject = "[LaptopHN POS] Mã xác thực đổi mật khẩu - " + recipientRole;
        
        String content = String.format("""
            <div style="font-family: Arial, sans-serif; max-width: 600px; margin: auto; padding: 20px; border: 1px solid #eee; border-radius: 10px;">
                <h2 style="color: #6d28d9;">Xác thực tài khoản (2-Step)</h2>
                <p>Bạn đang yêu cầu thay đổi mật khẩu quản trị. Đây là mã xác thực dành cho <strong>%s</strong>:</p>
                <div style="background: #f5f3ff; padding: 20px; border-radius: 8px; text-align: center; margin: 20px 0;">
                    <span style="font-size: 32px; font-weight: bold; color: #6d28d9; letter-spacing: 5px;">%s</span>
                </div>
                <p style="color: #666; font-size: 14px;">Mã này có hiệu lực trong 10 phút. Nếu bạn không thực hiện yêu cầu này, hãy đổi mật khẩu ngay để bảo mật.</p>
            </div>
            """, recipientRole, otp);

        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendEmployeeInvitation(String to, String fullName, String token) {
        String activationUrl = String.format("%s/api/v1/users/public/activate?token=%s", baseUrl, token);
        String subject = "[LaptopHN POS] Lời mời gia nhập đội ngũ";
        
        String content = String.format("""
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <h2 style="color: #1e40af; margin-bottom: 20px;">Chào mừng %s!</h2>
                <p style="color: #475569; line-height: 1.6;">Bạn đã được mời tham gia hệ thống quản trị <strong>LaptopHN POS</strong>. Vui lòng nhấn nút bên dưới để xác nhận và kích hoạt tài khoản nhân viên của bạn.</p>
                <div style="text-align: center; margin: 35px 0;">
                    <a href="%s" style="background: linear-gradient(135deg, #1e40af 0%%, #2563eb 100%%); color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold; box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1);">Xác nhận gia nhập</a>
                </div>
                <p style="color: #64748b; font-size: 13px;">Sau khi xác nhận, chúng tôi sẽ gửi mật khẩu đăng nhập vào email này cho bạn.</p>
                <hr style="border: none; border-top: 1px solid #f1f5f9; margin: 30px 0;">
                <p style="color: #94a3b8; font-size: 12px; text-align: center;">Trân trọng,<br>Ban quản trị LaptopHN POS</p>
            </div>
            """, fullName, activationUrl);

        sendHtmlEmail(to, subject, content);
    }

    @Async
    public void sendEmployeeCredentials(String to, String fullName, String username, String password) {
        String loginUrl = frontendUrl + "/login";
        String subject = "[LaptopHN POS] Tài khoản nhân viên của bạn";

        String content = String.format("""
            <div style="font-family: 'Segoe UI', Tahoma, Geneva, Verdana, sans-serif; max-width: 600px; margin: auto; padding: 30px; border: 1px solid #e2e8f0; border-radius: 16px; background-color: #ffffff;">
                <h2 style="color: #10b981; margin-bottom: 20px;">Kích hoạt thành công!</h2>
                <p style="color: #475569;">Chào <strong>%s</strong>, tài khoản của bạn đã sẵn sàng. Dưới đây là thông tin đăng nhập:</p>
                <div style="background: #f8fafc; padding: 20px; border-radius: 12px; margin: 25px 0; border: 1px solid #f1f5f9;">
                    <p style="margin: 0 0 10px 0;"><strong>Tên đăng nhập:</strong> <span style="color: #1e40af;">%s</span></p>
                    <p style="margin: 0;"><strong>Mật khẩu:</strong> <span style="color: #1e40af;">%s</span></p>
                </div>
                <div style="text-align: center; margin: 35px 0;">
                    <a href="%s" style="background: #10b981; color: white; padding: 16px 32px; text-decoration: none; border-radius: 12px; font-weight: bold;">Đăng nhập ngay</a>
                </div>
                <p style="color: #f43f5e; font-size: 13px; font-weight: 500;">Vì lý do bảo mật, bạn hãy đổi mật khẩu ngay sau lần đầu đăng nhập thành công.</p>
            </div>
            """, fullName, username, password, loginUrl);

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
        }
    }
}