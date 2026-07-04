package com.luccavergara.solaris.billing.service;

import com.resend.Resend;
import com.resend.services.emails.model.CreateEmailOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    @Value("${application.email.resend-api-key:}")
    private String resendApiKey;

    @Value("${application.email.from}")
    private String emailFrom;

    public void sendBillingOtp(String to, String otp) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("==================================================");
            System.out.println("RESEND_API_KEY not configured.");
            System.out.println("SOLARIS BILLING OTP for " + to + ": " + otp);
            System.out.println("==================================================");
            return;
        }

        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Solaris Billing <" + emailFrom + ">")
                .to(to)
                .subject("Your Solaris billing verification code")
                .html(buildOtpEmail(otp))
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not send verification email: " + exception.getMessage());
        }
    }

    private String buildOtpEmail(String otp) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2>Solaris Billing</h2>
                  <p>Use this code to continue in the billing portal:</p>
                  <p style="font-size: 32px; letter-spacing: 8px; font-weight: bold;">%s</p>
                  <p>This code expires in 10 minutes. If you did not request it, you can ignore this email.</p>
                </div>
                """.formatted(otp);
    }
}
