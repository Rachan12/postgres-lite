package com.postgresql.cli;

import com.postgresql.command.Command;
import com.postgresql.parser.CommandParser;

import java.util.Scanner;

public class PostgresLiteShell {
    public static void main(String[] args) {
        Scanner scanner = new Scanner(System.in);
        ConsoleUI.printWelcomeMessage();

        while (true) {
            ConsoleUI.printPrompt();
            String input = scanner.nextLine().trim();

            if (input.equalsIgnoreCase("exit")) {
                ConsoleUI.printExitMessage();
                break;
            }

            try {
                Command command = CommandParser.parse(input);
                if (command != null) {
                    command.execute();
                }
            } catch (Exception e) {
                ConsoleUI.printError(e.getMessage());
            }
        }
    }
}