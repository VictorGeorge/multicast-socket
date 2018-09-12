package me.maeroso;

import me.maeroso.enums.EnumResourceId;
import me.maeroso.enums.EnumResourceStatus;
import me.maeroso.protocol.Message;
import me.maeroso.protocol.Peer;

import javax.crypto.BadPaddingException;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.*;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MulticastSocket;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.*;
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
        this.mSocket.get().leaveGroup(InetAddress.getByName(Configuration.DEFAULT_HOST));
        this.mSocket.get().close();
    }

    /**
     * método que trata a liberação de um recurso.
     */
    public void resourceRelease(EnumResourceId resourceId) {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.RESOURCE_RELEASE, PeerManager.INSTANCE.getOurPeer(), resourceId));
    }

    /**
     * método que trata a requisição de um request.
     */
    public void resourceRequest(EnumResourceId resourceId) {
        // Atualiza o estado desse peer sobre esse recurso para WANTED
        PeerManager.INSTANCE.getOurPeer().getResourcesState().put(resourceId, EnumResourceStatus.WANTED);

        lastResourceRequestTimestamp.set(Instant.now());

        requestAnswers = new LinkedList<Message>();


        mResourceRequest(resourceId, lastResourceRequestTimestamp.get());// Envia uma mensagem de requisição do recurso

        while (requestAnswers.size() != PeerManager.INSTANCE.getPeerList().size()) {
            if (ChronoUnit.SECONDS.between(lastResourceRequestTimestamp.get(), Instant.now()) >= Configuration.MAXIMUM_DELTA_SEC) { //Segundos entre requisição e resposta
                List<Peer> peersWhoAnswered = new LinkedList<Peer>();
                List<Peer> peersWhoDidntAnswered = PeerManager.INSTANCE.getPeerList();
                for (Message m : requestAnswers) {
                    peersWhoAnswered.add(m.getDestinationPeer());
                }
                for (Peer p : peersWhoAnswered) {
                    peersWhoDidntAnswered.remove(p); //removing from list of peers the ones who answered we have the ones which didn't
                }
                //now we remove these peers
                for (Peer p : peersWhoDidntAnswered) {
                    mSendMessage(this.mSocket.get(), new Message(Message.MessageType.LEAVE_REQUEST, p));
                }
                break;
            }
        }
        // Excluimos todos os ausentes, agora vereficamos as respostas
        Boolean areAllReleased = requestAnswers.stream().allMatch(requestAnswers -> requestAnswers.getStatus().equals(EnumResourceStatus.RELEASED));
        if (areAllReleased) {
            // Se todos responderam released, esse peer pode pegar o recurso
            PeerManager.INSTANCE.getOurPeer().getResourcesState().put(resourceId, EnumResourceStatus.HELD);
            putOnQueue(resourceId, lastResourceRequestTimestamp.get());
            System.out.println("Changing " + resourceId + " to HELD");
        } else if (requestAnswers.stream().anyMatch(requestAnswers -> requestAnswers.getStatus().equals(EnumResourceStatus.HELD))) { //Se algum está em HELD
            System.out.println("Resource it's already HELD by a peer, put Request into queue");
            PeerManager.INSTANCE.getOurPeer().getResourcesState().put(resourceId, EnumResourceStatus.WANTED);//Se teve uma batalha de menor tempo de indicação, o que perdeu vai vir pra ca como released pq cedeu, entao tem que mudar pra Wanted de novo
            putOnQueue(resourceId, lastResourceRequestTimestamp.get());
        }
    }

    /**
     * método que constrói e envia mensagem de greeting aos pares.
     */
    private void mGreeting() {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.GREETING_REQUEST, PeerManager.INSTANCE.getOurPeer()));
    }

    /**
     * método que constrói e envia mensagem de mudança na fila de um resource.
     */
    private void mQueueAdd(EnumResourceId resourceId, Instant timestamp) {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.QUEUE_ADD, PeerManager.INSTANCE.getOurPeer(), resourceId, timestamp));
    }

    /**
     * método que constrói e envia mensagem de goodbye aos pares.
     */
    void mGoodbye() {
        if (PeerManager.INSTANCE.getPeerList().size() == 0) {
            try {
                this.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            this.releaseFromQueue(EnumResourceId.RESOURCE1);
            this.releaseFromQueue(EnumResourceId.RESOURCE2);
            mSendMessage(this.mSocket.get(), new Message(Message.MessageType.LEAVE_REQUEST, PeerManager.INSTANCE.getOurPeer()));
        }
    }

    public void releaseFromQueue(EnumResourceId resourceId) {
        Map<Instant, Peer> resourceQueue = PeerManager.INSTANCE.getResourceWanted(resourceId);
        Map.Entry<Instant, Peer> Head;
        if (!resourceQueue.isEmpty()) { //Verifica se n ta vazia
            Iterator<Map.Entry<Instant, Peer>> iterator = resourceQueue.entrySet().iterator();
            Head = iterator.next();//Pega novo cabeça da fila
            Peer ourPeer = PeerManager.INSTANCE.getOurPeer();
            if (Head.getValue().equals(ourPeer)) { //Se o cabeça da fila for a gente, muda pra HELD
                this.resourceRelease(resourceId);
            } else {
                while (iterator.hasNext()) {
                    Map.Entry<Instant, Peer> i = iterator.next();

                    // está no corpo da fila como interessado
                    if(i.getValue().equals(ourPeer)) {
                        mSendMessage(mSocket.get(), new Message(Message.MessageType.QUEUE_REMOVE, PeerManager.INSTANCE.getOurPeer(), resourceId, i.getKey()));
                    }
                }
            }
        }
    }

    /**
     * método que constrói e envia mensagem de requisição de recurso aos pares.
     */
    private void mResourceRequest(EnumResourceId resourceId, Instant timestamp) {
        mSendMessage(this.mSocket.get(), new Message(Message.MessageType.RESOURCE_REQUEST, PeerManager.INSTANCE.getOurPeer(), resourceId, timestamp, PeerManager.INSTANCE.getOurPeer().getSignature()));
    }

    /**
     * método que verifica se n teve 2 peers requisitando o recurso ao msm instante.
     */
    private void putOnQueue(EnumResourceId resourceId, Instant timestamp) {
        if (!PeerManager.INSTANCE.getResourceWanted(resourceId).containsKey(timestamp)) //Se não é duplicado, adiciona sem problemas
            PeerManager.INSTANCE.getResourceWanted(resourceId).put(timestamp, PeerManager.INSTANCE.getOurPeer()); //Coloca na fila
        else {//Se ja tem um lá
            timestamp = timestamp.plus(1, ChronoUnit.NANOS);
            PeerManager.INSTANCE.getResourceWanted(resourceId).put(timestamp, PeerManager.INSTANCE.getOurPeer()); //Coloca na fila
        }
        mQueueAdd(resourceId, lastResourceRequestTimestamp.get());//avisa outros peers para atualizarem sua fila
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

                    System.err.println("Message received: " + messageReceived);

                    // se a mensagem capturada é do própria instancia, pule
                    if (messageReceived.sourcePeer.equals(PeerManager.INSTANCE.getOurPeer()))
                        continue;

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
                        case LEAVE_RESPONSE: {
                            handleLeaveResponse(messageReceived);
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
                        case RESOURCE_RELEASE: {
                            handleResourceRelease(messageReceived);
                            break;
                        }
                        case QUEUE_ADD: {
                            handleQueueAdd(messageReceived);
                            break;
                        }
                        case QUEUE_REMOVE: {
                            handleQueueRemove(messageReceived);
                            break;
                        }
                    }
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }

        private void handleResourceResponse(Message messageReceived) {
            if (!messageReceived.destinationPeer.equals(PeerManager.INSTANCE.getOurPeer())) // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
                return;

            System.out.println("Receive response from " + messageReceived.sourcePeer);
            requestAnswers.add(messageReceived);
        }

        private void handleResourceRequest(Message messageReceived) {
            if (!verifySignature(messageReceived)) return;

            EnumResourceId requestedResource = messageReceived.getResource();// Guarda qual dos dois recursos é o request
            EnumResourceStatus requestedResourceStatus = PeerManager.INSTANCE.getOurPeer().getResourcesState().get(requestedResource); //verifica em que situação o estado está para este peer
            mSendMessage(mSocket.get(), new Message(Message.MessageType.RESOURCE_RESPONSE, PeerManager.INSTANCE.getOurPeer(), messageReceived.sourcePeer, requestedResource, requestedResourceStatus, Instant.now())); // envia mensagem de resposta a requisição
            System.out.println("RESOURCE_REQUEST to " + requestedResource + "\n");
        }

        private boolean verifySignature(Message messageReceived) {
            Peer supposedPeer = messageReceived.sourcePeer;
            Optional<Peer> first = PeerManager.INSTANCE.getPeerList().stream().filter(peer -> peer.getId().equals(supposedPeer.getId())).findFirst();
            PublicKey truePubKey = first.get().getPublicKey();
            try {
                CryptoUtils.checkSignature(truePubKey, messageReceived.signature);
                System.err.println(String.format("VALID SIGNATURE FROM %s ", messageReceived.sourcePeer.getId()));
            } catch (InvalidKeyException | IllegalBlockSizeException | BadPaddingException | NoSuchAlgorithmException | NoSuchPaddingException e) {
                System.err.println(String.format("INVALID SIGNATURE FROM %s ", messageReceived.sourcePeer.getId()));
                return false;
            }
            return true;
        }

        private void handleResourceRelease(Message messageReceived) {
            EnumResourceId requestedResource = messageReceived.getResource();// Guarda qual dos dois recursos é o release
            Map.Entry<Instant, Peer> Head = PeerManager.INSTANCE.getResourceWanted(requestedResource).entrySet().iterator().next();//Pega o cabeça da fila
            PeerManager.INSTANCE.getResourceWanted(requestedResource).remove(Head.getKey());//Remove o cabeça da fila que tinha o recurso
            if (!PeerManager.INSTANCE.getResourceWanted(requestedResource).isEmpty()) { //Verifica se n ta vazia
                Head = PeerManager.INSTANCE.getResourceWanted(requestedResource).entrySet().iterator().next();//Pega novo cabeça da fila
                if (Head.getValue().equals(PeerManager.INSTANCE.getOurPeer())) { //Se o cabeça da fila for a gente, muda pra HELD
                    PeerManager.INSTANCE.getOurPeer().getResourcesState().put(requestedResource, EnumResourceStatus.HELD);
                    System.out.println("You are now with " + requestedResource);
                }
            }
        }

        private void handleLeaveRequest(Message messageReceived) {
            // remova o par da lista de pares online
            PeerManager.INSTANCE.remove(messageReceived.sourcePeer);
            mSendMessage(mSocket.get(), new Message(Message.MessageType.LEAVE_RESPONSE, PeerManager.INSTANCE.getOurPeer(), messageReceived.sourcePeer));
        }

        private void handleLeaveResponse(Message messageReceived) {
            if (!messageReceived.destinationPeer.equals(PeerManager.INSTANCE.getOurPeer())) return;
            PeerManager.INSTANCE.getPeerList().remove(messageReceived.sourcePeer);
            if (PeerManager.INSTANCE.getPeerList().size() == 0) {
                try {
                    close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        private void handleGreetingResponse(Message messageReceived) {
            // se mensagem é destinada a esta instância adicione quem mandou a mensagem.
            if (!messageReceived.destinationPeer.equals(PeerManager.INSTANCE.getOurPeer())) return;
            PeerManager.INSTANCE.add(messageReceived.sourcePeer);
        }

        private void handleGreetingRequest(Message messageReceived) {
            PeerManager.INSTANCE.add(messageReceived.sourcePeer);
            mSendMessage(mSocket.get(), new Message(Message.MessageType.GREETING_RESPONSE, PeerManager.INSTANCE.getOurPeer(), messageReceived.sourcePeer)); // envia mensagem de auto-apresentação destinada ao novo par
        }

        private void handleQueueAdd(Message messageReceived) {
            EnumResourceId Resource = messageReceived.getResource();// Guarda sobre qual dos dois recursos é a msg
            Peer Peer = messageReceived.getSourcePeer();// Qual peer enviou a msg
            Instant time = messageReceived.getTimestamp(); //Qual timestamp
            PeerManager.INSTANCE.getResourceWanted(Resource).put(time, Peer); //adiciona a fila
        }

        private void handleQueueRemove(Message messageReceived) {
            EnumResourceId Resource = messageReceived.getResource();// Guarda sobre qual dos dois recursos é a msg
            Peer Peer = messageReceived.getSourcePeer();// Qual peer enviou a msg
            Instant time = messageReceived.getTimestamp(); //Qual timestamp
            PeerManager.INSTANCE.getResourceWanted(Resource).remove(time, Peer); //adiciona a fila
        }
    }
}