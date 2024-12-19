package com.phisebot;

import java.awt.Color;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.events.interaction.component.StringSelectInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import net.dv8tion.jda.api.utils.FileUpload;

public class PhiseBotEventHandler extends ListenerAdapter {

    public void sendSuspiciousEmailNotification(TextChannel channel, String senderIp, String emailBody, String attachments, String suspiciousUrl) {
        EmbedBuilder embed = new EmbedBuilder();
        embed.setTitle("🚨 **Suspicious Email Alert!**")
             .setColor(Color.RED)
             .setDescription("🔍 A potentially malicious email was detected. Please review the details below:\n\n")
             .addField("💻 **Sender IP Address**", senderIp.isEmpty() ? "❓ Unknown" : "**" + senderIp + "**", false)
             .addField("📎 **Attachments**", attachments.isEmpty() ? "❌ None" : "**" + attachments + "**", false)
             .addField("🌐 **Suspicious URL Detected**", suspiciousUrl.isEmpty() ? "❌ None" : "[Click Here](" + suspiciousUrl + ")", false)
             .setFooter("Phise Bot • Protecting your inbox 🛡️", null);

        // Send the embed without the email body
        channel.sendMessageEmbeds(embed.build())
               .setActionRow(
                   Button.success("allow_sender", "🟢 Allow Sender"),
                   Button.danger("block_sender", "🔴 Block Sender")
               )
               .queue();

        // Send the email body as a separate message with code block formatting
        channel.sendMessage("✉️ **Email Body:**\n```" + emailBody + "```").queue();

        // Send the email body as a .txt file attachment
        sendEmailBodyAsAttachment(channel, emailBody);
    }

    private void sendEmailBodyAsAttachment(TextChannel channel, String emailBody) {
        try {
            // Create a temporary .txt file
            File file = File.createTempFile("email_body", ".txt");
            FileWriter writer = new FileWriter(file);
            writer.write(emailBody);
            writer.close();

            // Send the file as an attachment in the channel
            channel.sendMessage("📄 Here's the email body as a .txt file:")
                   .addFiles(FileUpload.fromData(file))
                   .queue(message -> {
                       // Delete the file after sending to avoid storage issues
                       file.delete();
                   });
        } catch (IOException e) {
            e.printStackTrace();
            channel.sendMessage("⚠️ Failed to create and send the .txt file.").queue();
        }
    }

    @Override
    public void onButtonInteraction(ButtonInteractionEvent event) {
        String buttonId = event.getComponentId();
        if (buttonId.equals("allow_sender")) {
            allowSender(event.getUser(), event.getMessage().getEmbeds().get(0).getTitle()); // Replace with actual logic
            event.reply("✅ Sender has been allowed.").setEphemeral(true).queue();
        } else if (buttonId.equals("block_sender")) {
            blockSender(event.getUser(), event.getMessage().getEmbeds().get(0).getTitle()); // Replace with actual logic
            event.reply("🚫 Sender has been blocked and future emails will be prevented.").setEphemeral(true).queue();
        }
    }

    @Override
    public void onStringSelectInteraction(StringSelectInteractionEvent event) {
        String selection = event.getValues().get(0);
        if (selection.equals("allow_sender")) {
            allowSender(event.getUser(), event.getMessage().getEmbeds().get(0).getTitle()); // Replace with actual logic
            event.reply("✅ Sender has been allowed through the dropdown menu.").setEphemeral(true).queue();
        } else if (selection.equals("block_sender")) {
            blockSender(event.getUser(), event.getMessage().getEmbeds().get(0).getTitle()); // Replace with actual logic
            event.reply("🚫 Sender has been blocked through the dropdown menu.").setEphemeral(true).queue();
        }
    }

    private void blockSender(User user, String senderEmail) {
        if (DatabaseUtil.addBlockedSender(senderEmail)) {
            System.out.println("Sender " + senderEmail + " blocked successfully.");
        } else {
            System.out.println("Failed to block sender " + senderEmail + ".");
        }
    }

    private void allowSender(User user, String senderEmail) {
        if (DatabaseUtil.addAllowedSender(senderEmail)) {
            System.out.println("Sender " + senderEmail + " allowed successfully.");
        } else {
            System.out.println("Failed to allow sender " + senderEmail + ".");
        }
    }
}
