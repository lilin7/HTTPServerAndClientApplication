package server;

import com.sun.security.ntlm.Server;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.HashMap;

public class setConnection {
    public long seqNo = 1;
    public long seqNoSendOut = 1;
    public long seqNoExpecting = 0;
    public long seqNoReceived = 0;

    private HashMap<Integer,setConnection> connectionMap = new HashMap<>();
    public int timeoutTime = 1000;
    private boolean requestReceived = false; //to stop sending SYN-ACK in timerThread
    private DatagramChannel setConnectionChannel;
    private InetSocketAddress routerAddress;
    private int routerPort;

    public String requestData;

    public ReceiveAndSend receiveAndSend;
    public ServerHolder serverHolder;

    public boolean hasSentREQACK = false;

    public setConnection(){
    }

    public setConnection(DatagramChannel datagramChannel, InetSocketAddress routerAddress, ServerHolder serverHolder){
        this.setConnectionChannel = datagramChannel;
        this.routerAddress = routerAddress;
        this.routerPort = routerAddress.getPort();
        this.serverHolder = serverHolder;
    }

    //keep listening, to receive all incoming packets from client
    public void receivePacket(ByteBuffer buf) throws Exception{
        Packet newPacket = new Packet();
        newPacket.receivePacket(buf);
        seqNoReceived = newPacket.getSequenceNumber();

        if(! connectionMap.keySet().contains(newPacket.getPeerPort())){ //server never received anything from this client address
            System.out.println("Server receives packet from client, seq No. "+ seqNoReceived +", type: " + newPacket.getType());
            if(newPacket.getType()==1){ // server received SYN from server for the first handshake
                secondHandShake(newPacket);
            } else { //whatever other type is wrong, do nothing = ditch this packet
                System.out.println("Wrong type of packet, drop it, server will be listening and waiting...");
            }
        } else { //server has already received packet from this client address
            if (newPacket.getType()==3){ // type 3 = REQ
                //System.out.println(connectionMap.get(newPacket.getPeerPort()).getClass().getName());
                //if the REQ has been sent before for a POST request, in hashmap the value is PostServer
                //use postServer to re-send REQACK, and wait for DATA
                if (connectionMap.get(newPacket.getPeerPort()).getClass().getName().equalsIgnoreCase("server.PostServer")
                        && connectionMap.get(newPacket.getPeerPort()).hasSentREQACK) {
                    connectionMap.get(newPacket.getPeerPort()).sendREQACK(newPacket);
                } else { //if this is the first time server receives REQ, process REQ
                    connectionMap.get(newPacket.getPeerPort()).receiveRequest(newPacket);//use this instance of setConnection to receive this pkt
                }
            } else if(newPacket.getType()==6){//type 6 = DATA(POST)
                connectionMap.get(newPacket.getPeerPort()).receiveDATA(newPacket);
            } else if (newPacket.getType()==7) { //type 7 = ACK
                //use GetClient to ACK the Data packet with this seq No.
                connectionMap.get(newPacket.getPeerPort()).receiveACK(newPacket);//use this instance of setConnection to receive this pkt
            }else if(newPacket.getType()==8){ //type 8 = FIN
                connectionMap.get(newPacket.getPeerPort()).receiveDATA(newPacket);
            } else if (newPacket.getType()==9) { //type 9 = FIN-ACK
                connectionMap.get(newPacket.getPeerPort()).receiveFINACK(newPacket);
                //use GetClient to check if all Data packets are acked, if yes, close connection, if no, re-send the missing data packets
            } else {
                //whatever other type is wrong, do nothing = ditch this packet
            }
        }
    }

    public void receiveACK(Packet packet){

    }
    public void receiveDATA(Packet packet){

    }
    public void receiveFINACK(Packet packet){

    }
    private void secondHandShake(Packet clientFirstPacket) throws IOException {
        seqNoReceived = clientFirstPacket.getSequenceNumber();
        seqNoSendOut = seqNoReceived +1;
        String synAck2Payload = "";
        Packet packet2 = new Packet(2,seqNoSendOut,clientFirstPacket.getPeerAddress(),clientFirstPacket.getPeerPort(),synAck2Payload.getBytes());
        seqNoExpecting = seqNoSendOut+1;
        System.out.println("Server sends SYN-ACK packet to client for second hand shake");
        connectionMap.put(clientFirstPacket.getPeerPort(),this);
        this.send(packet2,this);
    }

    //only to deal with the request info from client, e.g. "get /foo Content-Type: txt"
    public void receiveRequest(Packet requestPacket){
        requestReceived = true; //to stop sending SYN-ACK in timerThread

        String payloadRequestInfo = new String(requestPacket.getPayload());
        System.out.println("Server receives packet from client, seq No. " + requestPacket.getSequenceNumber()+
                ", type: " + requestPacket.getType() +", Request Info");

        //parse request, call next method in this class, to send back content
        try {
            ReceiveAndSend receiveAndSendNew = new ReceiveAndSend(serverHolder);
            this.receiveAndSend = receiveAndSendNew;
            receiveAndSend.receive(payloadRequestInfo);
        } catch (IOException e) {
            e.printStackTrace();
        }

        //main part to process the request received, to decide if is Bad Request, or what to return to client
        boolean successProcess  = receiveAndSend.process();

        if(receiveAndSend.serverHolder.getFunction().equalsIgnoreCase("GET")){
            System.out.println(payloadRequestInfo); //only print if is get. Post print after assmebing data
            GetServer getServer = new GetServer(setConnectionChannel, routerAddress,requestPacket.getPeerAddress(),requestPacket.getPeerPort(),seqNo,seqNoExpecting,seqNoReceived,seqNoSendOut);
            //this is to replace the setConnection (value of this address) with getServer,
            // so next time in receivePacket, if type = 7 ACK or 9 FIN-ACK, can directly go to getServer class
            connectionMap.put(requestPacket.getPeerPort(),getServer);

            getServer.processRequestInfo(receiveAndSend);

        }else if(receiveAndSend.serverHolder.getFunction().equalsIgnoreCase("POST")){
            PostServer postServer = new PostServer(setConnectionChannel, routerAddress,requestPacket.getPeerAddress(),requestPacket.getPeerPort(),receiveAndSend,seqNo,seqNoExpecting,seqNoReceived,seqNoSendOut);
            connectionMap.put(requestPacket.getPeerPort(),postServer);
            postServer.processRequestInfo(receiveAndSend, requestPacket);
        }
    }

    public void sendREQACK(Packet requestPacket){

    }

    public boolean isRequestReceived(){
        return requestReceived;
    }

    // to be called by this class (secondHandShake), and class "GetServer" (real content, disconnection), "PostServer" (ACK, disconnection)
    public void send(Packet packet, setConnection setConnectionClass) throws IOException {
        setConnectionChannel.send(packet.readyToSend(),routerAddress);
        System.out.println("Server sends packet to client, seq No. " + packet.getSequenceNumber() +", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,setConnectionClass,packet);
        timeThread.start();
    }
}
