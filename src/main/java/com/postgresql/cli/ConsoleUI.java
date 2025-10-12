package com.postgresql.cli;

import com.postgresql.catalog.ColumnMetadata;

import java.util.List;

public class ConsoleUI {
    // ANSI color codes
    public static final String RESET = "\u001B[0m";
    public static final String CYAN = "\u001B[36m";
    public static final String YELLOW = "\u001B[33m";
    public static final String GREEN = "\u001B[32m";
    public static final String BOLD = "\u001B[1m";
    public static final String MAGENTA = "\u001B[35m";
    public static final String RED = "\u001B[31m";

    // Welcome banner with ASCII art
    public static void printWelcomeMessage() {
        System.out.println(CYAN + BOLD +
                "╔══════════════════════════════════════════════╗\n" +
                "║                                              ║\n" +
                "║   🛠️  Welcome to the " + YELLOW + "PostgresLite CLI" + CYAN + BOLD + "         ║\n" +
                "║                                              ║\n" +
                "║   Type '" + GREEN + "exit" + CYAN + BOLD + "' to quit, and may the queries   ║\n" +
                "║   be ever in your favor. 🔍⚡               ║\n" +
                "║                                              ║\n" +
                "╚══════════════════════════════════════════════╝" +
                RESET
        );
    }

    public static void printPrompt() {
        System.out.print(MAGENTA + "postgres-lite> " + RESET);
    }

    public static void printExitMessage() {
        System.out.println(GREEN + "👋 Exiting PostgresLite. Goodbye!" + RESET);
    }

    public static void printError(String message) {
        System.out.println("\u001B[31m❌ Error: " + message + RESET); // red
    }

    public static void printUnsupportedCommand(String input) {
        System.out.println(RED + "🛑 Whoops! I don't know what to do with: '" + input + "'" + RESET);
        System.out.println(YELLOW + "🤔 Maybe try something like: 'SELECT * FROM users' or 'exit'" + RESET);
    }

    public static void printSuccess(String message) {
        System.out.println(GREEN + "✅ " + message + RESET);
    }

    public static void printSchema(String tableName, List<ColumnMetadata> columns) {
        System.out.println(CYAN + BOLD + "\n📦 Table Schema for '" + tableName + "':" + RESET);
        for (ColumnMetadata col : columns) {
            System.out.println("  - " + MAGENTA + col.getName() + RESET + " : " + YELLOW + col.getType().name() + RESET);
        }
        System.out.println(); // spacing
    }

}

