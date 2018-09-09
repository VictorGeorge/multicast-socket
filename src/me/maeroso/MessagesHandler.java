package me.maeroso;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;
import me.maeroso.protocol.Message;
import me.maeroso.protocol.Peer;

import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;


/**
 * Classe de troca de mensagens da instância.
 */
class MessagesHandler {
    private final AsyncMessagesHandler asyncMessagesHandler;
    private AtomicReference<MulticastSocket> mSocket;
    private AtomicReference<Instant> lastResourceRequestTimestamp;
    private List<Message> requestAnswers;

    MessagesHandler() throws IOException {
        mSocket = new AtomicReference<>();
        mSocket.set(new MulticastSocket(Configuration.DEFAULT_PORT));
        asyncMessagesHandler = new AsyncMessagesHandler();
        lastResourceRequestTimestamp = new AtomicReference<Instant>();
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
        this.asyncMessagesHandler.start();
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
        this.asyncMessagesHandler.interrupt();
        this.mSocket.get().leaveGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        this.mSocket.get().close();
    }

    /**
     * método que trata a requisição de um request.
     */
    public void resourceRequest(EnumResourceId resourceId) {
        // Atualiza o estado desse peer sobre esse recurso para WANTED
        PeerManager.getInstance().getOurPeer().getResourcesState().put(resourceId, EnumResourceStatus.WANTED);

        lastResourceRequestTimestamp.set(Instant.now());
        requestAnswers = new LinkedList<Message>();

        // Envia uma mensagem de requisição do recurso
        mResourceRequest(resourceId, lastResourceRequestTimestamp.get());

        while (requestAnswers.size() != PeerManager.getInstance().getPeerList().size()) {
            if (ChronoUnit.SECONDS.between(lastResourceRequestTimestamp.get(),Instant.now()) >= Configuration.MAXIMUM_DELTA_SEC) { //Segundos entre requisição e resposta
                List<Peer> peersWhoAnswered = new LinkedList<Peer>();
                List<Peer> peersWhoDidntAnswered = PeerManager.getInstance().getPeerList();
                for(Message m : requestAnswers){
                    peersWhoAnswered.add(m.getDestinationPeer());
                }
                for(Peer p : peersWhoAnswered){
                    peersWhoDidntAnswered.remove(p); //removing from list of peers the ones who answered we have the ones which didn't
                }
                //now we remove these peers
                for(Peer p : peersWhoDidntAnswered){
                    mSendMessage(this.mSocket.get(), new Message(Message.MessageType.LEAVE_REQUEST, p));
                }
            }
        }
        System.out.println("Resource Request TODO");
        //TODO rest of algorithm

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
    private void mResourceRequest(EnumResourceId resourceId, Instant timestamp) {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.RESOURCE_REQUEST, PeerManager.getInstance().getOurPeer(), resourceId, timestamp));
    }

    /**
     * Handler assíncrono de eventos no grupo multicast.
     */
    class AsyncMessagesHandler extends Thread {
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
                        case GREETING_REQUEST: { // mensagem de requisição de cumprimento
                            handleGreetingRequest(messageReceived);
                            break;
                        }
                        case GREETING_RESPONSE: { // mensagem de resposta a cumprimento
                            handleGreetingResponse(messageReceived);
                            break;
                        }
                        case LEAVE_REQUEST: { // mensagem de "adeus" do par sainte
                            handleLeaveRequest(messageReceived);
                            break;
                        }
                        case RESOURCE_REQUEST: { // mensagem de requisição de recurso
                            handleResourceRequest(messageReceived);
                            break;
                        }
                        case RESOURCE_RESPONSE: { // mensagem de resposta a req. de recurso
                            handleResourceResponse(messageReceived);
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void handleResourceResponse(Message messageReceived) {
            if (!messageReceived.destinationPeer.equals(PeerManager.getInstance().getOurPeer())) // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
                return;

            System.out.println("Receive response from " + messageReceived.sourcePeer);
            requestAnswers.add(messageReceived);
        }

        private void handleResourceRequest(Message messageReceived) {
            EnumResourceId requestedResource = messageReceived.getResource();// Responde à requisição de recursos
            EnumResourceStatus requestedResourceStatus = PeerManager.getInstance().getOurPeer().getResourcesState().get(requestedResource); //verifica em que situação o estado está para este peer
            mSendMessage(mSocket.get(), new Message(Message.MessageType.RESOURCE_RESPONSE, PeerManager.getInstance().getOurPeer(), messageReceived.sourcePeer, requestedResource, requestedResourceStatus, Instant.now())); // envia mensagem de resposta a requisição
            System.out.println("RESOURCE_REQUEST to " + requestedResource + "\n");
        }

        private void handleLeaveRequest(Message messageReceived) {
            // remova o par da lista de pares online
            PeerManager.getInstance().remove(messageReceived.sourcePeer);
        }

        private void handleGreetingResponse(Message messageReceived) {
            // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
            if (!messageReceived.destinationPeer.equals(PeerManager.getInstance().getOurPeer())) return;
            PeerManager.getInstance().add(messageReceived.sourcePeer);
        }

        private void handleGreetingRequest(Message messageReceived) {
            PeerManager.getInstance().add(messageReceived.sourcePeer);
            mSendMessage(mSocket.get(), new Message(Message.MessageType.GREETING_RESPONSE, PeerManager.getInstance().getOurPeer(), messageReceived.sourcePeer)); // envia mensagem de auto-apresentação destinada ao novo par
        }
    }
}