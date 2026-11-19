package com.mycompany.econesttask8and9;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import java.nio.charset.StandardCharsets;
import java.util.Properties;

/** Sends UTF-8 plain-text mail using SMTP with STARTTLS. */
public class Tasks89SmtpEmailSender implements Tasks89EmailSender {

    private final String host;
    private final int port;
    private final String fromAddress;
    private final String username;
    private final String password;

    public Tasks89SmtpEmailSender(String host, int port, String fromAddress,
            String username, String password) {
        this.host = requireText(host, "SMTP host");
        if (port < 1 || port > 65535) {
            throw new IllegalArgumentException("SMTP port must be between 1 and 65535.");
        }
        this.port = port;
        this.fromAddress = requireText(fromAddress, "From email address");
        this.username = username == null ? "" : username.trim();
        this.password = password == null ? "" : password;
        if (this.username.isBlank() != this.password.isBlank()) {
            throw new IllegalArgumentException(
                    "Enter both the SMTP username and password, or leave both blank.");
        }
    }

    @Override
    public void send(String recipient, String subject, String body)
            throws Tasks89EmailDeliveryException {
        try {
            Properties properties = new Properties();
            properties.put("mail.smtp.host", host);
            properties.put("mail.smtp.port", Integer.toString(port));
            properties.put("mail.smtp.auth", Boolean.toString(!username.isBlank()));
            properties.put("mail.smtp.starttls.enable", "true");
            properties.put("mail.smtp.starttls.required", "true");
            properties.put("mail.smtp.connectiontimeout", "10000");
            properties.put("mail.smtp.timeout", "10000");
            properties.put("mail.smtp.writetimeout", "10000");

            Authenticator authenticator = username.isBlank() ? null : new Authenticator() {
                @Override
                protected PasswordAuthentication getPasswordAuthentication() {
                    return new PasswordAuthentication(username, password);
                }
            };

            Session session = Session.getInstance(properties, authenticator);
            MimeMessage message = new MimeMessage(session);
            message.setFrom(new InternetAddress(fromAddress));
            message.setRecipients(Message.RecipientType.TO,
                    InternetAddress.parse(requireText(recipient, "Recipient email address"), true));
            message.setSubject(requireText(subject, "Email subject"), StandardCharsets.UTF_8.name());
            message.setText(requireText(body, "Email body"), StandardCharsets.UTF_8.name());
            Transport.send(message);
        } catch (MessagingException | IllegalArgumentException exception) {
            throw new Tasks89EmailDeliveryException(
                    "Email could not be sent: " + describeFailure(exception), exception);
        }
    }

    private static String describeFailure(Exception exception) {
        Exception detail = exception;
        while (detail instanceof MessagingException messagingException
                && messagingException.getNextException() != null) {
            detail = messagingException.getNextException();
        }
        String message = detail.getMessage();
        return message == null || message.isBlank()
                ? detail.getClass().getSimpleName()
                : message;
    }

    private static String requireText(String value, String label) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException(label + " is required.");
        }
        return value.trim();
    }
}
