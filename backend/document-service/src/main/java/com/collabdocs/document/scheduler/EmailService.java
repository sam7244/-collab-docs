package com.collabdocs.document.scheduler;

import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);
    private static final DateTimeFormatter FMT =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm 'UTC'").withZone(ZoneId.of("UTC"));

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromAddress;

    @Value("${app.frontend-url:http://localhost:3000}")
    private String frontendUrl;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void sendInactivityEmail(String toEmail, String displayName,
                                    String docTitle, String docId, Instant lastEdited) {
        try {
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setFrom(fromAddress);
            helper.setTo(toEmail);
            helper.setSubject("[CollabDocs] Your document \"" + docTitle + "\" has been inactive");
            helper.setText(buildHtml(displayName, docTitle, docId, lastEdited), true);

            mailSender.send(message);
            log.info("Inactivity email sent to {} for document '{}'", toEmail, docTitle);
        } catch (Exception e) {
            log.error("Failed to send inactivity email to {} for doc {}: {}", toEmail, docId, e.getMessage());
        }
    }

    private String buildHtml(String displayName, String docTitle, String docId, Instant lastEdited) {
        String docUrl = frontendUrl + "/documents/" + docId;
        String lastEditedStr = FMT.format(lastEdited);

        return """
            <!DOCTYPE html>
            <html>
            <head>
              <meta charset="UTF-8">
              <style>
                body        { background:#0a0a0a; color:#00ff41; font-family:'Courier New',monospace; margin:0; padding:0; }
                .container  { max-width:560px; margin:40px auto; border:1px solid #00b32c;
                              padding:32px; background:#0d1a0f;
                              box-shadow:0 0 20px rgba(0,255,65,0.15); }
                .title      { font-size:28px; letter-spacing:4px; text-align:center;
                              text-shadow:0 0 10px #00ff41; margin-bottom:6px; }
                .subtitle   { font-size:11px; color:#00b32c; letter-spacing:3px;
                              text-align:center; margin-bottom:28px; }
                hr          { border:none; border-top:1px solid #003b0f; margin:20px 0; }
                .label      { font-size:10px; color:#00b32c; letter-spacing:2px;
                              text-transform:uppercase; margin-bottom:4px; }
                .value      { font-size:14px; color:#00ff41; margin-bottom:16px; }
                .doc-title  { font-size:18px; color:#ffb000; letter-spacing:1px; }
                .cta        { display:block; margin:24px auto; padding:12px 28px;
                              background:transparent; border:1px solid #00ff41; color:#00ff41;
                              text-decoration:none; text-align:center; font-size:13px;
                              letter-spacing:2px; text-transform:uppercase; }
                .cta:hover  { background:#00ff41; color:#000; }
                .footer     { font-size:10px; color:#003b0f; text-align:center;
                              letter-spacing:1px; margin-top:24px; }
              </style>
            </head>
            <body>
              <div class="container">
                <div class="title">COLLABDOCS</div>
                <div class="subtitle">INACTIVITY ALERT — DOCUMENT SYSTEM</div>
                <hr>

                <div class="label">USER</div>
                <div class="value">%s</div>

                <div class="label">INACTIVE DOCUMENT</div>
                <div class="value doc-title">%s</div>

                <div class="label">LAST MODIFIED</div>
                <div class="value">%s</div>

                <hr>
                <p style="font-size:13px; line-height:1.7; color:#00b32c;">
                  This document has not been edited for a while.<br>
                  Resume editing to keep your work active.
                </p>

                <a href="%s" class="cta">&gt; OPEN DOCUMENT</a>

                <hr>
                <div class="footer">
                  COLLABDOCS SYSTEM — AUTOMATED NOTIFICATION<br>
                  DO NOT REPLY TO THIS EMAIL
                </div>
              </div>
            </body>
            </html>
            """.formatted(displayName, docTitle, lastEditedStr, docUrl);
    }
}
