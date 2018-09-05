package me.maeroso;

import me.maeroso.protocol.Message;
import me.maeroso.protocol.Peer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.*;
import java.nio.file.Files;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicReference;

class AsyncMessagesHandler {
    private final Thread multicastListener;
    private final Thread unicastListener;
    private AtomicReference<MulticastSocket> mSocket = new AtomicReference<>();
    private AtomicReference<ServerSocket> uServerSocket = new AtomicReference<>();
    private AtomicReference<Peer> currentPreferredPeer = new AtomicReference<>();

    AsyncMessagesHandler() throws IOException {
        this.mSocket.set(new MulticastSocket(Configuration.DEFAULT_PORT));
        this.joinGroup();
        this.uServerSocket.set(new ServerSocket(PeerManager.getInstance().getOurPeer().getPort()));
        this.multicastListener = new MulticastEventListener();
        this.unicastListener = new UnicastEventListener();
        this.start();
    }

    private static void uSendMessage(Socket socket, Message message) {
        try {
            OutputStream socketOutputStream = socket.getOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(socketOutputStream);
            objectOutputStream.writeObject(message);
            objectOutputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void mSendMessage(MulticastSocket datagramSocket, Message message) {
        try {
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            ObjectOutputStream objectOutputStream = new ObjectOutputStream(byteArrayOutputStream);
            objectOutputStream.writeObject(message);
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            DatagramPacket packet = new DatagramPacket(byteArray, byteArray.length, InetAddress.getByName(Configuration.DEFAULT_HOST), Configuration.DEFAULT_PORT);
            datagramSocket.send(packet);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void joinGroup() throws IOException {
        this.mSocket.get().joinGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        System.err.printf("Joined group %s%n", Configuration.DEFAULT_HOST);
    }

    private void start() {
        this.multicastListener.start();
        this.unicastListener.start();
        this.mGreeting();
    }

    void close() throws IOException {
        this.unicastListener.interrupt();
        this.multicastListener.interrupt();
        this.uServerSocket.get().close();
        this.mSocket.get().close();
    }

    private void mGreeting() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.GREETING_REQUEST, PeerManager.getInstance().getOurPeer()));
    }

    void searchFile(String fileName) {
        mSendMessage(mSocket.get(), new Message(Message.MessageType.SEARCH_REQUEST, PeerManager.getInstance().getOurPeer(), fileName));
        try {
            Thread.sleep(5000);
            if (currentPreferredPeer.get() != null) {
                Socket uSocket = new Socket(currentPreferredPeer.get().getAddress(), currentPreferredPeer.get().getPort());
                uSendMessage(uSocket, new Message(Message.MessageType.ESTABLISH_TRANSFER_REQUEST, PeerManager.getInstance().getOurPeer(), fileName));
            } else {
                System.out.println("No peer has the file in the network");
            }
        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    class MulticastEventListener extends Thread {
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                while (!mSocket.get().isClosed()) {
                    DatagramPacket packetReceived = new DatagramPacket(buffer, buffer.length);
                    mSocket.get().receive(packetReceived);
                    byte[] data = packetReceived.getData();
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data);
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream);
                    Message messageReceived = (Message) objectInputStream.readObject();

                    if (messageReceived.sourcePeer.getPublicKey().equals(PeerManager.getInstance().getOurPeer().getPublicKey()))
                        continue;

                    System.err.println("Message received: " + messageReceived);
                    switch (messageReceived.messageType) {
                        case GREETING_REQUEST: {
                            PeerManager.getInstance().add(messageReceived.sourcePeer);
                            Socket uSocket = new Socket(messageReceived.sourcePeer.getAddress(), messageReceived.sourcePeer.getPort());
                            uSendMessage(uSocket, new Message(Message.MessageType.GREETING_RESPONSE, PeerManager.getInstance().getOurPeer()));
                            break;
                        }
                        case SEARCH_REQUEST:
                            String fileName = (String) messageReceived.content;
                            List<File> searchResult = FileManager.getInstance().search(fileName);
                            if (searchResult.size() < 1) break;
                            Socket uSocket = new Socket(messageReceived.sourcePeer.getAddress(), messageReceived.sourcePeer.getPort());
                            uSendMessage(uSocket, new Message(Message.MessageType.SEARCH_RESPONSE, PeerManager.getInstance().getOurPeer()));
                            break;
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }

    class UnicastEventListener extends Thread {
        private ExecutorService executorService;

        UnicastEventListener() {
            this.executorService = Executors.newCachedThreadPool();
        }

        @Override
        public void run() {
            while (this.getState() == State.RUNNABLE) {
                try {
                    AtomicReference<Socket> clientSocket = new AtomicReference<>();
                    ServerSocket serverSocket = uServerSocket.get();
                    if (!serverSocket.isClosed()) clientSocket.set(serverSocket.accept());
                    if (clientSocket.get() != null && !clientSocket.get().isClosed()) {
                        executorService.submit(() -> handleUnicastEvent(clientSocket.get()));
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleUnicastEvent(Socket clientSocket) {
            try {
                InputStream inputStream = clientSocket.getInputStream();
                ObjectInputStream objectInputStream = new ObjectInputStream(inputStream);

                Message messageReceived = (Message) objectInputStream.readObject();
                switch (messageReceived.messageType) {
                    case GREETING_RESPONSE: {
                        PeerManager.getInstance().add(messageReceived.sourcePeer);
                    }
                    case SEARCH_RESPONSE: {
                        Peer sourcePeer = messageReceived.sourcePeer;
                        Peer currentPeer = currentPreferredPeer.get();
                        if (currentPeer == null) currentPreferredPeer.set(sourcePeer);
                        else if (currentPeer.getReputation() < sourcePeer.getReputation())
                            currentPreferredPeer.set(sourcePeer);
                    }
                    case ESTABLISH_TRANSFER_REQUEST: {
                        String fileName = (String) messageReceived.content;
                        File selectedFile = FileManager.getInstance().search(fileName).get(0);
                        byte[] fileContent = Files.readAllBytes(selectedFile.toPath());
                        try {
                            byte[] encryptedBytes = CryptoUtils.encrypt(messageReceived.sourcePeer.getPublicKey(), fileContent);
                            FileBytesTuple fileBytesTuple = new FileBytesTuple(selectedFile, encryptedBytes);
                            uSendMessage(clientSocket, new Message(Message.MessageType.ESTABLISH_TRANSFER_RESPONSE, PeerManager.getInstance().getOurPeer(), fileBytesTuple));
                        } catch (InvalidKeyException e) {
                            System.err.println("Invalid key exception: " + e);
                        } catch (BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException | IllegalBlockSizeException e) {
                            e.printStackTrace();
                        }
                    }
                    case ESTABLISH_TRANSFER_RESPONSE: {
                        FileBytesTuple receivedFile = (FileBytesTuple) messageReceived.content;
                        try {
                            byte[] decryptedBytes = CryptoUtils.decrypt(PeerManager.getInstance().getKeyPair().getPrivate(), receivedFile.fileContent);
                            boolean b = FileManager.getInstance().saveFile(receivedFile.fileObject.getName(), decryptedBytes);
                            if (b)
                                uSendMessage(clientSocket, new Message(Message.MessageType.SEND_RATING, PeerManager.getInstance().getOurPeer(), PeerManager.getInstance().getOurPeer().getReputation() + 1));
                        } catch (InvalidKeyException e) {
                            System.err.println("Invalid key exception: " + e);
                            uSendMessage(clientSocket, new Message(Message.MessageType.SEND_RATING, PeerManager.getInstance().getOurPeer(), PeerManager.getInstance().getOurPeer().getReputation() - 1));
                        } catch (BadPaddingException | NoSuchPaddingException | IllegalBlockSizeException | NoSuchAlgorithmException e) {
                            uSendMessage(clientSocket, new Message(Message.MessageType.SEND_RATING, PeerManager.getInstance().getOurPeer(), PeerManager.getInstance().getOurPeer().getReputation() - 1));
                        }
                    }
                    case SEND_RATING: {
                        Integer rating = (Integer) messageReceived.content;
                        PeerManager.getInstance().getOurPeer().setReputation(rating);
                    }
                    default: {
                        break;
                    }
                }
                clientSocket.close();
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}