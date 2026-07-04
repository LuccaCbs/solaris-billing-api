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

    public void sendPurchaseConfirmation(String to, String organizationName, int quantity, String activationCode) {
        if (resendApiKey == null || resendApiKey.isBlank()) {
            System.out.println("==================================================");
            System.out.println("RESEND_API_KEY not configured.");
            System.out.println("SOLARIS PURCHASE CONFIRMATION for " + to);
            System.out.println("Org: " + organizationName + " | Qty: " + quantity + " | Code: " + activationCode);
            System.out.println("==================================================");
            return;
        }

        Resend resend = new Resend(resendApiKey);

        CreateEmailOptions params = CreateEmailOptions.builder()
                .from("Solaris Billing <" + emailFrom + ">")
                .to(to)
                .subject("Solaris purchase confirmed")
                .html(buildPurchaseConfirmationEmail(organizationName, quantity, activationCode))
                .build();

        try {
            resend.emails().send(params);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not send purchase confirmation email: " + exception.getMessage());
        }
    }

    private String buildPurchaseConfirmationEmail(String organizationName, int quantity, String activationCode) {
        return """
                <div style="font-family: Arial, sans-serif; max-width: 480px; margin: 0 auto;">
                  <h2>Solaris Billing</h2>
                  <p>Your purchase was confirmed.</p>
                  <p><strong>Organization:</strong> %s</p>
                  <p><strong>Additional stores:</strong> %d</p>
                  <p><strong>Reference code:</strong> <code>%s</code></p>
                  <p>The add-on is already active on your organization. You can close this email.</p>
                </div>
                """.formatted(organizationName, quantity, activationCode);
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
