package com.collabdocs.document.scheduler;

import com.collabdocs.document.entity.Document;
import com.collabdocs.document.repository.DocumentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Component
public class InactiveDocumentScheduler {

    private static final Logger log = LoggerFactory.getLogger(InactiveDocumentScheduler.class);

    private final DocumentRepository documentRepository;
    private final AuthServiceClient authServiceClient;
    private final EmailService emailService;

    // How many minutes of no edits before a doc is considered inactive (default: 5 min)
    @Value("${app.inactivity-threshold-minutes:5}")
    private long inactivityThresholdMinutes;

    // Don't re-send email to the same doc within this many minutes (default: 10 min)
    @Value("${app.inactivity-email-cooldown-minutes:10}")
    private long emailCooldownMinutes;

    @Value("${app.email-notifications-enabled:true}")
    private boolean emailNotificationsEnabled;

    public InactiveDocumentScheduler(DocumentRepository documentRepository,
                                     AuthServiceClient authServiceClient,
                                     EmailService emailService) {
        this.documentRepository = documentRepository;
        this.authServiceClient = authServiceClient;
        this.emailService = emailService;
    }

    @Scheduled(cron = "${app.inactivity-check-cron:0 0 9 * * *}")
    public void checkInactiveDocuments() {
        if (!emailNotificationsEnabled) {
            log.info("Email notifications disabled — skipping inactivity check.");
            return;
        }
        Instant now = Instant.now();
        Instant inactiveSince = now.minus(inactivityThresholdMinutes, ChronoUnit.MINUTES);
        Instant emailCooldown = now.minus(emailCooldownMinutes, ChronoUnit.MINUTES);

        log.info("Running inactivity check — looking for docs not edited since {} (threshold: {} min)",
                inactiveSince, inactivityThresholdMinutes);

        List<Document> inactiveDocs = documentRepository.findInactiveDocuments(inactiveSince, emailCooldown);

        if (inactiveDocs.isEmpty()) {
            log.info("No inactive documents found.");
            return;
        }

        log.info("Found {} inactive document(s). Sending notifications...", inactiveDocs.size());

        for (Document doc : inactiveDocs) {
            AuthServiceClient.UserInfo owner = authServiceClient.getUserById(doc.getOwnerId());

            if (owner == null || owner.email() == null) {
                log.warn("Could not resolve owner for doc {} — skipping", doc.getId());
                continue;
            }

            emailService.sendInactivityEmail(
                owner.email(),
                owner.displayName(),
                doc.getTitle(),
                doc.getId().toString(),
                doc.getUpdatedAt()
            );

            // Mark email sent so we don't spam the same doc
            doc.setInactivityEmailSentAt(now);
            documentRepository.save(doc);
        }

        log.info("Inactivity check complete. Notified {} owner(s).", inactiveDocs.size());
    }
}
