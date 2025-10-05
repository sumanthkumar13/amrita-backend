package com.amrita.amritabackend.service;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.sendgrid.Method;
import com.sendgrid.Request;
import com.sendgrid.Response;
import com.sendgrid.SendGrid;
import com.sendgrid.helpers.mail.Mail;
import com.sendgrid.helpers.mail.objects.Content; // ✅ ADD THIS IMPORT
import com.sendgrid.helpers.mail.objects.Email;

@Service
public class OtpService {

    private static class OtpEntry {
        private final String otp;
        private final long expiresAt;

        public OtpEntry(String otp, long expiresAt) {
            this.otp = otp;
            this.expiresAt = expiresAt;
        }

        public String getOtp() {
            return otp;
        }

        public long getExpiresAt() {
            return expiresAt;
        }
    }

    private static final Logger logger = LoggerFactory.getLogger(OtpService.class);

    private final long OTP_TTL_SECONDS = 5 * 60;
    private final Map<String, OtpEntry> otpCache = new ConcurrentHashMap<>();
    private final Set<String> verifiedEmails = ConcurrentHashMap.newKeySet();

    @Value("${SENDGRID_API_KEY}")
    private String sendgridApiKey;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OtpService() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredOtps, 1, 1, TimeUnit.MINUTES);
    }

    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", (int) (Math.random() * 900_000) + 100_000);
        long expiresAt = Instant.now().getEpochSecond() + OTP_TTL_SECONDS;
        otpCache.put(email, new OtpEntry(otp, expiresAt));

        try {
            sendEmail(email, otp);
            logger.info("✅ OTP email sent via SendGrid to {}", email);
        } catch (Exception ex) {
            logger.error("❌ Failed to send OTP email to {}: {}", email, ex.getMessage());
            logger.info("OTP (fallback) for {} is {}", email, otp);
        }
    }

    public boolean verifyOtp(String email, String otp) {
        OtpEntry entry = otpCache.get(email);
        if (entry == null)
            return false;

        long now = Instant.now().getEpochSecond();
        if (entry.getExpiresAt() < now) {
            otpCache.remove(email);
            return false;
        }

        if (entry.getOtp().equals(otp)) {
            verifiedEmails.add(email);
            otpCache.remove(email);
            logger.info("✅ OTP verified for {}", email);
            return true;
        }
        return false;
    }

    public boolean isVerified(String email) {
        return verifiedEmails.contains(email);
    }

    public void consumeVerification(String email) {
        verifiedEmails.remove(email);
    }

    private void sendEmail(String to, String otp) throws IOException {
        Email from = new Email("eventlyreplied@gmail.com");
        Email recipient = new Email(to);
        String subject = "Your Amrita Events App OTP";
        Content content = new Content("text/plain",
                String.format("Your OTP is: %s\nIt will expire in %d minutes.", otp, OTP_TTL_SECONDS / 60));

        Mail mail = new Mail(from, subject, recipient, content);

        SendGrid sg = new SendGrid(sendgridApiKey);
        Request request = new Request();
        try {
            request.setMethod(Method.POST);
            request.setEndpoint("mail/send");
            request.setBody(mail.build());
            Response response = sg.api(request);
            logger.info("SendGrid response: status={}, body={}", response.getStatusCode(), response.getBody());
        } catch (IOException ex) {
            throw ex;
        }
    }

    private void cleanupExpiredOtps() {
        long now = Instant.now().getEpochSecond();
        otpCache.entrySet().removeIf(e -> e.getValue().getExpiresAt() < now);
    }
}
