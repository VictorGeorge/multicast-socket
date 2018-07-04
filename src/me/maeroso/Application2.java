package me.maeroso;

import java.io.IOException;
import java.util.Scanner;

public class Application2 {
    private AsyncMessagesHandler messagesHandler;

    public Application2() throws IOException {
        this.messagesHandler = new AsyncMessagesHandler();
    }

    public static void main(String[] args) {
        try {
            Application2 application2 = new Application2();
            application2.cli();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    private void cli() {
        Scanner scanner = new Scanner(System.in);
        boolean exitKeyPressed = false;
        String command;
        do {
            System.out.println("Type a command: ");
            command = scanner.nextLine();

            switch (command.trim().toLowerCase()) {
                case "search": {
                    String fileName;
                    System.out.println("Type a file name: ");
                    fileName = scanner.nextLine();
                    messagesHandler.searchFile(fileName);
                }
                break;
                case "add": {
                    String fileName;
                    System.out.println("Type a path: ");
                    fileName = scanner.nextLine();
                    try {
                        FileManager.getInstance().addFile(fileName);
                        System.out.println("File added successfully");
                    } catch (IOException e) {
                        e.printStackTrace();
                        System.out.println("It was not possible to add file! :(");
                    }
                }
                case "exit": {
                    this.close();
                    exitKeyPressed = true;
                }
                break;
            }
        } while (!exitKeyPressed);
    }

    private void close() {
        try {
            this.messagesHandler.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
