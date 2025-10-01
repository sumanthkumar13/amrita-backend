package com.amrita.amritabackend.service;

import java.time.Instant;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

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

    @Autowired
    private JavaMailSender mailSender;

    @Value("${spring.mail.username}") // ✅ ensures from = configured mail user
    private String fromEmail;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

    public OtpService() {
        scheduler.scheduleAtFixedRate(this::cleanupExpiredOtps, 1, 1, TimeUnit.MINUTES);
    }

    public void generateAndSendOtp(String email) {
        String otp = String.format("%06d", (int) (Math.random() * 900_000) + 100_000);
        long expiresAt = Instant.now().getEpochSecond() + OTP_TTL_SECONDS;
        otpCache.put(email, new OtpEntry(otp, expiresAt));

        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(email);
            message.setSubject("Your Amrita Events App OTP");
            message.setText(String.format("Your OTP is: %s\nIt will expire in %d minutes.", otp, OTP_TTL_SECONDS / 60));
            mailSender.send(message);
            logger.info("✅ OTP email sent to {}", email);
        } catch (Exception ex) {
            logger.error("❌ Failed to send OTP email to {}: {}", email, ex.getMessage());
            // fallback: log OTP for dev
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

    private void cleanupExpiredOtps() {
        long now = Instant.now().getEpochSecond();
        otpCache.entrySet().removeIf(e -> e.getValue().getExpiresAt() < now);
    }
}
