package com.phisebot;

import com.google.api.services.gmail.Gmail;

import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.JDABuilder;
import net.dv8tion.jda.api.entities.Activity;

public class Main {
    public static void main(String[] args) throws Exception {
        // Securely load the bot token from an environment variable
        String token = System.getenv("DISCORD_BOT_TOKEN");
        if (token == null || token.isEmpty()) {
            System.out.println("Bot token is not set. Please set the DISCORD_BOT_TOKEN environment variable.");
            return;
        }

        // Initialize the Discord bot
        JDA jda = JDABuilder.createDefault(token)
                .setActivity(Activity.watching("for suspicious emails"))
                .build();

        // Wait for JDA to be ready
        jda.awaitReady();
        System.out.println("Bot is ready and connected to Discord.");

        // Initialize the SQLite database
        DatabaseUtil.initializeDatabase();
        System.out.println("Database initialized successfully.");

        // Create BotEventListener and pass it to GmailHandler
        BotEventListener botEventListener = new BotEventListener(jda);
        jda.addEventListener(botEventListener);

        // Register PhiseBotEventHandler
        jda.addEventListener(new PhiseBotEventHandler()); // Add this line

        // Start Gmail service and create the handler
        Gmail service = GmailService.getGmailService();
        GmailHandler gmailHandler = new GmailHandler(botEventListener);

        System.out.println("Starting to monitor emails...");

        // Start the email checking process
        gmailHandler.startEmailCheck(service);
    }
}
