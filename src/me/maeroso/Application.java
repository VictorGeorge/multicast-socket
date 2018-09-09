package me.maeroso;

import me.maeroso.enums.EnumResourceId;

import java.io.IOException;
import java.util.Scanner;

public class Application {
    private MessagesHandler messagesHandler;

    public Application() throws IOException {
        this.messagesHandler = new MessagesHandler();
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


    private void cli() throws IOException {
        Scanner scanner = new Scanner(System.in);
        boolean exitKeyPressed = false;
        String command;
        System.out.println("Commands:");
        System.out.println("list");
        System.out.println("resource1");
        System.out.println("resource2");
        System.out.println("exit\n");

        do {
            System.out.println("Type a command: ");
            command = scanner.nextLine();

            switch (command.trim().toLowerCase()) {
                case "list": {
                    PeerManager.getInstance().printPeerList();
                    break;
                }
                case "resource1": {
                    if (!PeerManager.getInstance().isStarted())
                        System.out.println("Minimum " + Configuration.MINIMUM_PEERS + " peers to initiate");
                    else {
                        this.messagesHandler.resourceRequest(EnumResourceId.RESOURCE1);
                    }
                    break;
                }
                case "resource2": {
                    if (!PeerManager.getInstance().isStarted())
                        System.out.println("Minimum" + Configuration.MINIMUM_PEERS + " peers to initiate");
                    else {
                        this.messagesHandler.resourceRequest(EnumResourceId.RESOURCE2);
                    }
                    break;
                }
                case "exit": {
                    this.close();
                    exitKeyPressed = true;
                    break;
                }
                default:
                    System.out.println("Unknown Command");
                    break;
            }
        } while (!exitKeyPressed);
    }

    private void close() throws IOException {
        this.messagesHandler.close();
    }
}
