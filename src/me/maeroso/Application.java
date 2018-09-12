package me.maeroso;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;
import me.maeroso.protocol.Peer;

import java.io.IOException;
import java.time.Instant;
import java.util.Map;
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
        do {
            System.out.println("\nCommands:");
            System.out.println("list");
            System.out.println("resource1");
            System.out.println("resource2");
            if(isResourceHeld(EnumResourceId.RESOURCE1)) //Se peer tem posse do recurso 1
                System.out.println("free r1");
            if(isResourceHeld(EnumResourceId.RESOURCE2)) //Se peer tem posse do recurso 2
                System.out.println("free r2");
            System.out.println("exit");
            System.out.println("\nType a command: ");
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
                case "free r1": {
                    if(isResourceHeld(EnumResourceId.RESOURCE1)) { //Checa se peer realmente tem posse do recurso 1
                        PeerManager.getInstance().getOurPeer().getResourcesState().put(EnumResourceId.RESOURCE1, EnumResourceStatus.RELEASED);
                        Map.Entry <Instant,Peer> Head = PeerManager.getInstance().getResourceWanted(EnumResourceId.RESOURCE1).entrySet().iterator().next();//Pega o cabeça da fila que tem o recurso
                        PeerManager.getInstance().getResourceWanted(EnumResourceId.RESOURCE1).remove(Head.getKey());//Remove ele proprio da cabeça da fila
                        this.messagesHandler.resourceRelease(EnumResourceId.RESOURCE1);//avisar os outros por multicast que liberou
                    }
                    else {
                        System.out.println("\nYou don't have the resource 1 to free it! jerk\n");
                    }
                    break;
                }
                case "free r2": {
                    if(isResourceHeld(EnumResourceId.RESOURCE2)) { //Checa peer realmente tem posse do recurso 2
                        PeerManager.getInstance().getOurPeer().getResourcesState().put(EnumResourceId.RESOURCE2, EnumResourceStatus.RELEASED);
                        Map.Entry <Instant,Peer> Head = PeerManager.getInstance().getResourceWanted(EnumResourceId.RESOURCE2).entrySet().iterator().next();//Pega o cabeça da fila que tem o recurso
                        PeerManager.getInstance().getResourceWanted(EnumResourceId.RESOURCE2).remove(Head.getKey());//Remove ele proprio da cabeça da fila
                        this.messagesHandler.resourceRelease(EnumResourceId.RESOURCE2);//avisar os outros por multicast que liberou
                    }
                    else {
                        System.out.println("\nYou don't have the resource 2 to free it! jerk\n");
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
        this.messagesHandler.mGoodbye();
    }

    private boolean isResourceHeld(EnumResourceId resourceId){
        if(PeerManager.getInstance().getOurPeer().getResourcesState().get(resourceId) == EnumResourceStatus.HELD) //Se peer tem posse do recurso
            return true;
        return false;
    }
}
