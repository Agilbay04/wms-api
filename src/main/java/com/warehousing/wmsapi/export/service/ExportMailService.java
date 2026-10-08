package com.warehousing.wmsapi.export.service;

import com.warehousing.wmsapi.export.repository.ExportJobRepository.Job;
import java.nio.file.Path;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
public class ExportMailService {
    private final JavaMailSender mailSender;

    public ExportMailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void send(Job job, Path file) {
        try {
            var message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");
            helper.setTo(job.requesterEmail());
            helper.setSubject("Warehouse report export " + job.id());
            helper.setText("Your " + job.reportType().name().toLowerCase(java.util.Locale.ROOT)
                    + " report is attached.");
            helper.addAttachment(file.getFileName().toString(), new FileSystemResource(file));
            mailSender.send(message);
        } catch (jakarta.mail.MessagingException exception) {
            throw new IllegalStateException("Export email could not be sent.", exception);
        }
    }
}
