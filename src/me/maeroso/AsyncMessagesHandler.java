package me.maeroso;

import me.maeroso.protocol.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicReference;

class AsyncMessagesHandler {
    private final Thread multicastListener;
    private AtomicReference<MulticastSocket> mSocket;

    AsyncMessagesHandler() throws IOException {
        mSocket = new AtomicReference<>();
        mSocket.set(new MulticastSocket(Configuration.DEFAULT_PORT));
        multicastListener = new MulticastEventListener();
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

    void start() throws IOException {
        this.mSocket.get().joinGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        System.err.printf("Joined group %s%n", Configuration.DEFAULT_HOST);
        this.multicastListener.start();
        this.mGreeting();
    }

    void close() {
        this.mGoodbye();
        this.multicastListener.interrupt();
        this.mSocket.get().close();
    }

    private void mGreeting() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.GREETING_REQUEST, PeerManager.getInstance().getOurPeer()));
    }

    private void mGoodbye() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.LEAVE_REQUEST, PeerManager.getInstance().getOurPeer()));
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

                    if (messageReceived.sourcePeer.getId().equals(PeerManager.getInstance().getOurPeer().getId()))
                        continue;

                    System.err.println("Message received: " + messageReceived);
                    switch (messageReceived.messageType) {
                        case GREETING_REQUEST: {
                            PeerManager.getInstance().add(messageReceived.sourcePeer);
                            mSendMessage(mSocket.get(), new Message(Message.MessageType.GREETING_RESPONSE, PeerManager.getInstance().getOurPeer(), messageReceived.sourcePeer));
                            break;
                        }
                        case GREETING_RESPONSE: {
                            if (messageReceived.destinationPeer.getId().equals(PeerManager.getInstance().getOurPeer().getId()))
                                PeerManager.getInstance().add(messageReceived.sourcePeer);
                            break;
                        }
                        case LEAVE_REQUEST: {
                            PeerManager.getInstance().remove(messageReceived.sourcePeer);
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
    }
}