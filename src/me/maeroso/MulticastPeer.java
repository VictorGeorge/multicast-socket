package me.maeroso;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.*;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.X509EncodedKeySpec;
import java.util.Random;
import java.util.Scanner;
import java.util.Vector;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.FutureTask;
import java.util.concurrent.atomic.AtomicReference;

public class MulticastPeer {

    private static final int DEFAULT_MULTICAST_PORT = 6789;

    private static final String DEFAULT_HOST = "224.0.0.1";
    private static final String PEER_JOINED = "\rPJN:";
    public static final String SEARCH_REQUEST = "\rSRC:";
    private static final String DEFAULT_FOLDER = "files";
    private static final String SEARCH_RESULT = "\rSRR:";

    private final AtomicReference<MulticastSocket> mSocket = new AtomicReference<MulticastSocket>();
    private InetAddress mGroup;
    private Scanner scanner;
    private ExecutorService executorService;
    private Vector<Peer> peers;
    private Peer mPeer;
    private Vector<PeerFileTuple> mSharedLibrary;
    private int unicastPort;

    private MulticastPeer() {
        scanner = new Scanner(System.in);
        executorService = Executors.newSingleThreadExecutor();
        mPeer = new Peer();
    }

    public static void main(String args[]) {
        MulticastPeer multicastPeer = new MulticastPeer();
        multicastPeer.start();
    }

    private void start() {
        try {
            Random random = new Random();
            mPeer.unicastPort = random.nextInt(50) + 4200;
            mPeer.host = InetAddress.getLocalHost();

            mGroup = InetAddress.getByName(DEFAULT_HOST);
            mSocket.set(new MulticastSocket(DEFAULT_MULTICAST_PORT));
            mSocket.get().joinGroup(mGroup);
            System.out.printf("Joined group %s%n", mGroup.getHostAddress());

            KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
            keyPairGenerator.initialize(1024);

            KeyPair keyPair = keyPairGenerator.generateKeyPair();
            mPeer.publicKey = keyPair.getPublic();

            sendJoinMessage();

            syncFileListWithFolder();
            listenForRemoteCommands();

            boolean exitKeyPressed = false;
            String command;
            do {
                System.out.println("Type a command: ");
                command = scanner.nextLine();

                switch (command.trim().toLowerCase()) {
                    case "search":
                        searchCLI();
                        break;
                    case "exit":
                        mSocket.get().leaveGroup(mGroup);
                        executorService.shutdown();
                        exitKeyPressed = true;
                        break;
                }
            } while (!exitKeyPressed);

        } catch (SocketException e) {
            System.out.println("Socket: " + e.getMessage());
        } catch (IOException e) {
            System.out.println("IO: " + e.getMessage());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } finally {
            if (mSocket.get() != null) mSocket.get().close();
        }
    }

    private void sendJoinMessage() throws IOException {
        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
        byte[] publicKeyEncoded = mPeer.publicKey.getEncoded();
        byteBuffer.put(PEER_JOINED.getBytes());
        byteBuffer.put(publicKeyEncoded);
        byteBuffer.put(mPeer.host.getAddress());
        byteBuffer.putChar(':');
        byteBuffer.putInt(mPeer.unicastPort);
        send(mSocket.get(), byteBuffer.array());
    }

    private void sendSearchMessage(String fileName) {
        try {
            ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
            byte[] fileNameBytes = fileName.getBytes();
            byteBuffer.put(SEARCH_REQUEST.getBytes());
            byteBuffer.put(fileNameBytes);
            send(mSocket.get(), byteBuffer.array());

            ServerSocket serverSocket = new ServerSocket(mPeer.unicastPort);

        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void syncFileListWithFolder() {
        //TODO could use FileMonitor from apache commons IO

        Path path = Paths.get(String.format("%s@%d", DEFAULT_FOLDER, unicastPort));
        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        mPeer.files = path.toFile().listFiles();
    }


    private void searchCLI() {
        System.out.print("Search: ");
        String fileName = scanner.nextLine();

        sendSearchMessage(fileName);
        System.out.println("These are the results: ");
//        for (int i = 0, searchResultLength = searchResult.length; i < searchResultLength; i++) {
//            PeerFileTuple peerFileTuple = searchResult[i];
//            System.out.printf("[%d] peerFileTuple.file = %s; peerFileTuple.owner = %s%n", i, peerFileTuple.file.getName(), peerFileTuple.owner);
//        }
//
//        System.out.print("Select file: ");
//        int index = scanner.nextInt();
//
//        if (index > -1)
//            downloadFromLibrary(searchResult[index]);
    }

    private void downloadFromLibrary(PeerFileTuple peerFileTuple) {
        try {
            Socket socket = new Socket(peerFileTuple.owner.host, peerFileTuple.owner.unicastPort);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }



    private void send(DatagramSocket datagramSocket, byte[] message) throws IOException {
        DatagramPacket messagePacket = new DatagramPacket(message, message.length, mGroup, DEFAULT_MULTICAST_PORT);
        datagramSocket.send(messagePacket);
    }

    private void listenForRemoteCommands() {
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                try {
                    while (!mSocket.get().isClosed()) {
                        byte[] buffer = new byte[2048];
                        DatagramPacket messageIn = new DatagramPacket(buffer, buffer.length);

                        if (mSocket.get().isClosed())
                            break;
                        mSocket.get().receive(messageIn);

                        ByteBuffer byteBuffer = ByteBuffer.allocate(2048);
                        byteBuffer.put(messageIn.getData());
                        InetAddress address = messageIn.getAddress();
                        int port = messageIn.getPort();

                        String str = new String(byteBuffer.array());

                        if (str.contains(PEER_JOINED)) {

                            //PEER JOINED
                            int i = str.indexOf(PEER_JOINED) + PEER_JOINED.length();
                            String publicKey = str.substring(i, i+1024);
                            registerPeer(publicKey);
                        }
                        if (str.contains(SEARCH_REQUEST)) {

                        }
                                //SEARCH_REQEUST
                        if(str.contains(SEARCH_RESULT)) {
                                //SEARCH RESULT

                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (NoSuchAlgorithmException e) {
                    e.printStackTrace();
                } catch (InvalidKeySpecException e) {
                    e.printStackTrace();
                }
            }
        };
        executorService.submit(runnable);
    }

    private void registerPeer(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        Peer p = new Peer();
        p.publicKey = KeyFactory.getInstance("RSA").generatePublic(new X509EncodedKeySpec(publicKey.getBytes()));
        peers.add(p);
    }
}


