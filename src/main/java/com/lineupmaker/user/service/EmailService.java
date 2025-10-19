package com.lineupmaker.user.service;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class EmailService {

    private final JavaMailSender mailSender;

    // application.yml 파일의 'app.base-url' 값을 주입
    @Value("${app.base-url}")
    private String baseUrl;

    @Value("${spring.mail.username}")
    private String senderEmail;

    /**
     * 회원가입 인증 코드가 포함된 메일을 발송합니다.
     * @param toEmail 수신자 이메일
     * @param code 6자리 인증 코드 (UUID 대신 사용)
     */
    public void sendVerificationCodeEmail(String toEmail, String code) {

        MimeMessage message = mailSender.createMimeMessage();

        try {
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(senderEmail);
            helper.setTo(toEmail);
            helper.setSubject("[Lineup_Maker] 회원가입 인증 코드입니다.");

            // 💡 [수정] 이메일 본문: 인증 링크 대신 코드를 강조
            String htmlContent = "<h2>안녕하세요, Lineup Maker 입니다.</h2>"
                    + "<p>아래 6자리 인증 코드를 회원가입 화면에 입력하여 계정 활성화를 완료해주세요.</p>"
                    + "<div style='margin: 30px 0; padding: 15px; background-color: #f0f0f0; border-radius: 8px; text-align: center;'>"
                    + "<span style='font-size: 28px; font-weight: bold; color: #4CAF50; letter-spacing: 5px;'>"
                    + code
                    + "</span>"
                    + "</div>"
                    + "<p>이 코드는 30분 동안 유효합니다.</p>";

            helper.setText(htmlContent, true);

            mailSender.send(message);

        } catch (MessagingException e) {
            throw new RuntimeException("이메일 전송에 실패했습니다. 서버 설정을 점검하거나 이메일 주소를 확인해주세요.", e);
        }
    }
}
