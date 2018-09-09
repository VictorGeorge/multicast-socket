package me.maeroso;

import java.io.IOException;
import java.util.Scanner;

public class Application {
    private AsyncMessagesHandler messagesHandler;

    public Application() throws IOException {
        this.messagesHandler = new AsyncMessagesHandler();
    }

    public static void main(String[] args) {
        try {
            Application app = new Application();
            app.start();
            app.cli();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void start() {
        try {
            this.messagesHandler.start();
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
                    //TODO resource search
                    break;
                }
                case "list": {
                    PeerManager.getInstance().printPeerList();
                    break;
                }
                case "exit": {
                    this.close();
                    exitKeyPressed = true;
                    break;
                }
            }
        } while (!exitKeyPressed);
    }

    private void close() {
        this.messagesHandler.close();
    }
}
