package com.digitwin.digitaltwin.service;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class NotificationService {

    private static final Logger logger = LogManager.getLogger(NotificationService.class);

    @Value("${error_detection.notify.email_recipients}")
    private String[] emailRecipients;

    @Value("${error_detection.notify.whatsapp_recipients}")
    private String[] whatsappRecipients;

    public void sendEmail(String subject, String message) {
        logger.info("Sending email: {} - {}", subject, message);
        for (String recipient : emailRecipients) {
            logger.info("Email sent to {}", recipient);
        }
    }

    public void sendWhatsapp(String message) {
        logger.info("Sending WhatsApp message: {}", message);
        for (String recipient : whatsappRecipients) {
            logger.info("WhatsApp sent to {}", recipient);
        }
    }
}