package me.maeroso;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;
import me.maeroso.protocol.Message;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Classe de troca de mensagens da instância.
 */
class AsyncMessagesHandler {
    private final Thread multicastListener;
    private AtomicReference<MulticastSocket> mSocket;

    AsyncMessagesHandler() throws IOException {
        mSocket = new AtomicReference<>();
        mSocket.set(new MulticastSocket(Configuration.DEFAULT_PORT));
        multicastListener = new MulticastEventListener();
    }

    /**
     * Método para enviar mensagem via socket Multicast
     *
     * @param datagramSocket
     * @param message
     */
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

    /**
     * Método de inicialização do fluxo onde:
     * - o socket se registra no grupo multicast;
     * - inicia a thread de tratamento assíncrono de mensagens;
     * - manda mensagem de cumprimento a todos os pares.
     *
     * @throws IOException
     */
    void start() throws IOException {
        this.mSocket.get().joinGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        this.multicastListener.start();
        this.mGreeting();
    }


    /**
     * Método de encerramento do fluxo onde:
     * - manda mensagem de adeus a todos os pares;
     * - interrompe a thread de tratamento assíncrono de mensagens;
     * - o socket sai do grupo multicast;
     * - o socket multicast é encerrado.
     */
    void close() throws IOException {
        this.mGoodbye();
        this.multicastListener.interrupt();
        this.mSocket.get().leaveGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        this.mSocket.get().close();
    }

    /**
     * método que constrói e envia mensagem de greeting aos pares.
     */
    private void mGreeting() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.GREETING_REQUEST, PeerManager.getInstance().getOurPeer()));
    }

    /**
     * método que constrói e envia mensagem de goodbye aos pares.
     */
    private void mGoodbye() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.LEAVE_REQUEST, PeerManager.getInstance().getOurPeer()));
    }

    /**
     * método que constrói e envia mensagem de requisição de recurso aos pares.
     */
    private void mResourceRequest(EnumResourceId resouceId) {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.RESOURCE_REQUEST, PeerManager.getInstance().getOurPeer(), resouceId));
    }

    /**
     * método que trata a requisição de um request.
     */
    public void resourceRequest(EnumResourceId resouceId){
        // Atualiza o estado desse peer sobre esse recurso para WANTED
        PeerManager.getInstance().getOurPeer().getResourcesState().put(resouceId, EnumResourceStatus.WANTED);

        // Envia uma mensagem de requisição do recurso
        mResourceRequest(resouceId);
        //TODO rest of algorithm
    }

    /**
     * Listener de eventos assíncronos no grupo multicast.
     */
    class MulticastEventListener extends Thread {
        @Override
        public void run() {
            try {
                byte[] buffer = new byte[4096];
                while (!mSocket.get().isClosed()) { // enquanto o socket não for fechado no foreground, rode...
                    DatagramPacket packetReceived = new DatagramPacket(buffer, buffer.length); // cria referencia do pacote
                    mSocket.get().receive(packetReceived); // recebe dados dentro de pacote
                    byte[] data = packetReceived.getData(); // armazena bytes do pacote dentro de array
                    ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(data); // cria fluxo de bytes
                    ObjectInputStream objectInputStream = new ObjectInputStream(byteArrayInputStream); // cria input stream de objeto
                    Message messageReceived = (Message) objectInputStream.readObject(); // converte array de bytes do pacote em objeto

                    // se a mensagem capturada é do própria instancia, pule
                    if (messageReceived.sourcePeer.equals(PeerManager.getInstance().getOurPeer()))
                        continue;

                    System.err.println("Message received: " + messageReceived);
                    switch (messageReceived.messageType) { // verifica tipo da mensagem
                        case GREETING_REQUEST: { // recebe pedido de cumprimento a todos
                            PeerManager.getInstance().add(messageReceived.sourcePeer);
                            mSendMessage(mSocket.get(), new Message(Message.MessageType.GREETING_RESPONSE, PeerManager.getInstance().getOurPeer(), messageReceived.sourcePeer)); // envia mensagem de auto-apresentação destinada ao novo par
                            break;
                        }
                        case GREETING_RESPONSE: { // recebe resposta pedido de cumprimento
                            // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
                            if (messageReceived.destinationPeer.equals(PeerManager.getInstance().getOurPeer()))
                                PeerManager.getInstance().add(messageReceived.sourcePeer);
                            break;
                        }
                        case LEAVE_REQUEST: { // mensagem de adeus
                            // remova o par da lista de pares online
                            PeerManager.getInstance().remove(messageReceived.sourcePeer);
                            break;
                        }
                        case RESOURCE_REQUEST: { // mensagem de requisição de recurso
                            EnumResourceId requestedResource = messageReceived.getResource();// Responde à requisição de recursos
                            EnumResourceStatus requestResourceSituation = PeerManager.getInstance().getOurPeer().getResourcesState().get(requestedResource); //verifica em que situação o estado está para este peer
                            mSendMessage(mSocket.get(), new Message(Message.MessageType.RESOURCE_RESPONSE, PeerManager.getInstance().getOurPeer(), messageReceived.sourcePeer, requestedResource, requestResourceSituation)); // envia mensagem de resposta a requisição
                            System.out.println("RESOURCE_REQUEST to " + requestedResource + "\n");
                            break;
                        }
                        case RESOURCE_RESPONSE: { // mensagem de requisição de recurso
                            if (messageReceived.destinationPeer.equals(PeerManager.getInstance().getOurPeer())) // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
                                System.out.println("Receive response from " + messageReceived.sourcePeer);
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