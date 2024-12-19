package com.phisebot;

import java.util.List;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.entities.channel.concrete.TextChannel;
import net.dv8tion.jda.api.events.message.MessageReceivedEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.components.buttons.Button;

public class BotEventListener extends ListenerAdapter {
    private JDA jda;

    public BotEventListener(JDA jda) {
        this.jda = jda;
    }

    public void notifyDiscord(String message, String sender) {
        // Replace with your channel ID
        String channelId = "1303179987112824876";
        TextChannel channel = jda.getTextChannelById(channelId);

        if (channel != null) {
            List<String> allowedSenders = DatabaseUtil.getAllowedSenders();
            if (allowedSenders.contains(sender)) {
                System.out.println("Sender is in the allowed list. Notification skipped.");
                return;
            }

            // Send a message with interactive buttons for allow/block actions
            channel.sendMessage(message)
                    .setActionRow(
                            Button.success("allow_sender:" + sender, "Allow Sender"),
                            Button.danger("block_sender:" + sender, "Block Sender")
                    ).queue();
        } else {
            System.out.println("Channel not found!");
        }
    }

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        String content = event.getMessage().getContentRaw();
        TextChannel channel = event.getChannel().asTextChannel(); // Ensure using concrete TextChannel

        // Command to allow a sender
        if (content.startsWith("!allow")) {
            String[] parts = content.split(" ");
            if (parts.length > 1) {
                String email = parts[1];
                if (DatabaseUtil.addAllowedSender(email)) {
                    channel.sendMessage("Sender **" + email + "** has been added to the allowed list.").queue();
                } else {
                    channel.sendMessage("Failed to add **" + email + "** to the allowed list.").queue();
                }
            } else {
                channel.sendMessage("Usage: !allow [email]").queue();
            }
        }

        // Command to block a sender
        else if (content.startsWith("!block")) {
            String[] parts = content.split(" ");
            if (parts.length > 1) {
                String email = parts[1];
                if (DatabaseUtil.addBlockedSender(email)) {
                    channel.sendMessage("Sender **" + email + "** has been added to the blocked list.").queue();
                } else {
                    channel.sendMessage("Failed to add **" + email + "** to the blocked list.").queue();
                }
            } else {
                channel.sendMessage("Usage: !block [email]").queue();
            }
        }

        // Command to view allowed senders
        else if (content.equalsIgnoreCase("!viewAllowed")) {
            List<String> allowedSenders = DatabaseUtil.getAllowedSenders();
            if (allowedSenders.isEmpty()) {
                channel.sendMessage("**No allowed senders found.**").queue();
            } else {
                channel.sendMessage("**Allowed Senders:**\n" + String.join("\n", allowedSenders)).queue();
            }
        }

        // Command to view blocked senders
        else if (content.equalsIgnoreCase("!viewBlocked")) {
            List<String> blockedSenders = DatabaseUtil.getBlockedSenders();
            if (blockedSenders.isEmpty()) {
                channel.sendMessage("**No blocked senders found.**").queue();
            } else {
                channel.sendMessage("**Blocked Senders:**\n" + String.join("\n", blockedSenders)).queue();
            }
        }

        // Command to remove a sender from the allowed list
        else if (content.startsWith("!removeAllowed")) {
            String[] parts = content.split(" ");
            if (parts.length > 1) {
                String email = parts[1];
                if (DatabaseUtil.removeAllowedSender(email)) {
                    channel.sendMessage("Sender **" + email + "** has been removed from the allowed list.").queue();
                } else {
                    channel.sendMessage("Failed to remove **" + email + "** from the allowed list.").queue();
                }
            } else {
                channel.sendMessage("Usage: !removeAllowed [email]").queue();
            }
        }

        // Command to remove a sender from the blocked list
        else if (content.startsWith("!removeBlocked")) {
            String[] parts = content.split(" ");
            if (parts.length > 1) {
                String email = parts[1];
                if (DatabaseUtil.removeBlockedSender(email)) {
                    channel.sendMessage("Sender **" + email + "** has been removed from the blocked list.").queue();
                } else {
                    channel.sendMessage("Failed to remove **" + email + "** from the blocked list.").queue();
                }
            } else {
                channel.sendMessage("Usage: !removeBlocked [email]").queue();
            }
        }
    }
}
