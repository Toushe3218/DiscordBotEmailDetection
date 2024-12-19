package com.phisebot;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.model.Message;
import com.google.api.services.gmail.model.MessagePart;
import com.google.api.services.gmail.model.MessagePartHeader;

public class GmailHandler {
    private BotEventListener botEventListener;
    private Set<String> processedEmailIds = new HashSet<>(); // Store processed email IDs

    public GmailHandler(BotEventListener botEventListener) {
        this.botEventListener = botEventListener;
        DatabaseUtil.initializeDatabase(); // Initialize the database when the handler is created
    }

    public void startEmailCheck(Gmail service) {
        new Thread(() -> {
            while (true) {
                try {
                    checkNewEmails(service);
                    Thread.sleep(10000); // Wait for 10 seconds before checking again
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
            }
        }).start();
    }

    private void checkNewEmails(Gmail service) throws IOException {
        System.out.println("Checking for new emails...");
        List<Message> messages = service.users().messages().list("me").setMaxResults(10L).execute().getMessages();

        if (messages != null) {
            for (Message message : messages) {
                if (!processedEmailIds.contains(message.getId())) {
                    processedEmailIds.add(message.getId()); // Mark as processed
                    System.out.println("Processing email ID: " + message.getId());
                    Message fullMessage = service.users().messages().get("me", message.getId()).execute();
                    analyzeEmail(fullMessage);
                } else {
                    System.out.println("Skipping already processed email ID: " + message.getId());
                }
            }
        } else {
            System.out.println("No new emails found.");
        }
    }

    private void analyzeEmail(Message message) throws IOException {
        StringBuilder report = new StringBuilder();
        boolean isSuspicious = false;

        String subject = getHeader(message, "Subject");
        String fromHeader = getHeader(message, "From");
        System.out.println("Email Subject: " + (subject != null ? subject : "No Subject"));
        System.out.println("From: " + (fromHeader != null ? fromHeader : "Unknown Sender"));

        if (fromHeader != null) {
            String senderEmail = extractEmail(fromHeader);
            if (DatabaseUtil.isSenderBlocked(senderEmail)) {
                System.out.println("Email from blocked sender detected: " + senderEmail);
                botEventListener.notifyDiscord("Blocked sender detected! Sender: " + senderEmail, senderEmail);
                return; // Stop processing if the sender is blocked
            } else if (DatabaseUtil.isSenderAllowed(senderEmail)) {
                System.out.println("Email from allowed sender: " + senderEmail);
                return; // Stop processing if the sender is allowed
            }
        }

        String ipAddress = extractIPAddress(message);
        if (ipAddress != null) {
            report.append("Sender IP Address: ").append(ipAddress).append("\n");
            System.out.println("Extracted IP Address: " + ipAddress);
        } else {
            System.out.println("No IP Address found in headers.");
        }

        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase("Authentication-Results")) {
                    System.out.println("Analyzing Authentication-Results header...");
                    if (header.getValue().contains("spf=fail") || header.getValue().contains("dkim=fail") || header.getValue().contains("dmarc=fail")) {
                        report.append("Failed SPF/DKIM/DMARC checks.\n");
                        isSuspicious = true;
                        System.out.println("SPF/DKIM/DMARC check failed.");
                    }
                }
            }
        }

        String body = extractBody(message);
        if (body != null) {
            System.out.println("Extracted email body for analysis.");
            report.append("Email Body:\n").append(body).append("\n\n");
            Pattern urlPattern = Pattern.compile("https?://[^\s]+", Pattern.CASE_INSENSITIVE);
            Matcher matcher = urlPattern.matcher(body);
            while (matcher.find()) {
                String url = matcher.group();
                System.out.println("Found URL: " + url);
                if (isSuspiciousURL(url)) {
                    report.append("Suspicious URL detected: ").append(url).append("\n");
                    isSuspicious = true;
                    System.out.println("Suspicious URL flagged: " + url);
                }
            }
        } else {
            System.out.println("Email body could not be extracted.");
        }

        if (body != null && containsSuspiciousPhrases(body)) {
            report.append("Suspicious phrases detected in email body.\n");
            isSuspicious = true;
            System.out.println("Suspicious phrases found in email body.");
        }

        if (message.getPayload() != null && message.getPayload().getParts() != null) {
            for (MessagePart part : message.getPayload().getParts()) {
                if (part.getFilename() != null && !part.getFilename().isEmpty()) {
                    String filename = part.getFilename().toLowerCase();
                    System.out.println("Attachment found: " + filename);
                    if (filename.endsWith(".exe") || filename.endsWith(".bat") || filename.endsWith(".vbs")) {
                        report.append("Harmful attachment detected: ").append(filename).append("\n");
                        isSuspicious = true;
                        System.out.println("Harmful attachment flagged: " + filename);
                    }
                }
            }
        }

        if (isSuspicious) {
            System.out.println("Suspicious email detected. Notifying Discord...");
            botEventListener.notifyDiscord("Suspicious email detected!\n" + report.toString(), fromHeader);
        } else {
            System.out.println("No suspicious activity detected in this email.");
        }
    }

    private String extractBody(Message message) {
        if (message.getPayload() != null) {
            if (message.getPayload().getBody() != null && message.getPayload().getBody().getData() != null) {
                return new String(java.util.Base64.getUrlDecoder().decode(message.getPayload().getBody().getData()));
            } else if (message.getPayload().getParts() != null) {
                for (MessagePart part : message.getPayload().getParts()) {
                    if (part.getMimeType().equals("text/plain") && part.getBody() != null && part.getBody().getData() != null) {
                        return new String(java.util.Base64.getUrlDecoder().decode(part.getBody().getData()));
                    }
                }
            }
        }
        return null;
    }

    private String extractIPAddress(Message message) {
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase("Received")) {
                    Pattern ipPattern = Pattern.compile("\\b(?:\\d{1,3}\\.){3}\\d{1,3}\\b");
                    Matcher matcher = ipPattern.matcher(header.getValue());
                    if (matcher.find()) {
                        return matcher.group();
                    }
                }
            }
        }
        return null;
    }

    private String getHeader(Message message, String name) {
        if (message.getPayload() != null && message.getPayload().getHeaders() != null) {
            for (MessagePartHeader header : message.getPayload().getHeaders()) {
                if (header.getName().equalsIgnoreCase(name)) {
                    return header.getValue();
                }
            }
        }
        return null;
    }

    private String extractEmail(String headerValue) {
        Pattern emailPattern = Pattern.compile("<(.+?)>");
        Matcher matcher = emailPattern.matcher(headerValue);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return headerValue;
    }

    private boolean isSuspiciousURL(String url) {
        return url.contains("bit.ly") || url.contains("tinyurl.com") || url.contains("unknown-domain.com");
    }

    private boolean containsSuspiciousPhrases(String body) {
        String[] suspiciousPhrases = {
            "verify your account", "click here immediately", "urgent action required", 
            "your account is in danger", "login now to secure"
        };
        for (String phrase : suspiciousPhrases) {
            if (body.toLowerCase().contains(phrase)) {
                return true;
            }
        }
        return false;
    }
}
