package client;

import java.io.IOException;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;

public class setConnection {



    public long seqNo = 1;
    public long seqNoSendOut = 1;
    public long seqNoExpecting = 0;
    public long seqNoReceived = 0;

    private int type;
    private long sequenceNumber;
    public InetAddress peerAddress;
    public int peerPort;
    private byte[] payload;
    private boolean receiveSYNACK = false;
    public static int timeoutTime = 1000;
    public Holder holder;
    private InetSocketAddress routerAddress;
    public DatagramChannel channel;
    private InetSocketAddress serverAddress;
    private String status;

    public SendAndReceive sendAndReceive;

    private int localPort;
    private InetSocketAddress localAddress;


    private GetClient getClient;
    private PostClient postClient;

    public setConnection(){
    }

    public setConnection(int type, long sequenceNumber, InetSocketAddress serverAddress, Holder holder,
                         InetSocketAddress routerAddress,int localPort,SendAndReceive sendAndReceive){
        this.type = type;
        this.sequenceNumber = sequenceNumber;
        this.serverAddress = serverAddress;
        this.peerAddress = serverAddress.getAddress();
        this.peerPort = serverAddress.getPort();
        this.payload = new byte[0];
        this.holder = holder;
        this.routerAddress = routerAddress;
        this.localPort = localPort;
        this.localAddress = new InetSocketAddress(localPort);
        this.sendAndReceive = sendAndReceive;
    }

    public void firstHandShake()throws Exception{
        this.status = "connection";
        //UDP
        channel = DatagramChannel.open();
        channel.bind(localAddress);
        channel.configureBlocking(false);
        seqNoSendOut = seqNo;
        String syn1Payload = "";
        Packet packet1 = new Packet(1,seqNoSendOut,peerAddress,peerPort,syn1Payload.getBytes());
        seqNoExpecting = seqNoSendOut +1;
        System.out.println("Client sends packet type: SYN, for first hand shake");
        this.send(packet1,this);
    }

    public void send(Packet packet,setConnection sGP) throws IOException {
        channel.send(packet.readyToSend(),routerAddress);
        System.out.println("Client sends packet to server, seq No. " + packet.getSequenceNumber() + ", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,sGP,packet);
        timeThread.start();
    }

    //client receives every kinds of incoming packet
    public void receivePacket(ByteBuffer buf)throws Exception{
        Packet newPacket = new Packet();
        newPacket.receivePacket(buf); //put the reply from server into this new packet
        seqNoReceived = newPacket.getSequenceNumber();

        if(status.equals("connection") && (newPacket.getType() == 2)){ //only during handshake and received SYN-ACK
            receiveSYNACK = true;
            if (seqNoReceived == seqNoExpecting) { //received second handshake from server is the response to my first handshake
                System.out.println("Client receives packet from server, seq No. "+ seqNoReceived
                        +", type: " + newPacket.getType() + ", handshake confirmed. Sending request message.");
                sendRequestInfo(newPacket);
            } else {
                System.out.println("Client received wrong packet number. Re-sending...");
                //do nothing: actually ditch packet, keep waiting
            }

        }else if(status.equals("GET")){
            System.out.println("Client receives packet from server, seq No. "+ newPacket.getSequenceNumber()
                    +", type: " + newPacket.getType());
            getClient.newPacketFromServer(newPacket);

        }else if(status.equals("POST")){
            System.out.println("Client receives packet from server, seq No. "+ newPacket.getSequenceNumber()
                    +", type: " + newPacket.getType());
            postClient.newPacketFromServer(newPacket);
        }else {
            //do nothing-ditch pkt, timeout and re-send
        }
    }

    //after handshake connection, use this method to send request info.
    //for get, is to send the whole message, e.g. "get /foo Content-Type: txt"
    //for post, is to send message except "Content-Body", e.g. "post /foo Content-Type: txt". (Content-Body will be the payload begin from next packet)
    public void sendRequestInfo(Packet packet2)throws Exception{
        //no need to deal with seq no. here, as will dealt with in getClient or postClient
        if(holder.getFunction().equalsIgnoreCase("GET")){ // create a new instance of GetClient
            this.status = "GET";
            //call sendRequset in class GetClient, to send the whole get request e.g. "get /foo Content-Type: txt"
            getClient = new GetClient(type,sequenceNumber,serverAddress,holder,routerAddress,localPort,channel,seqNo,seqNoExpecting,seqNoReceived,seqNoSendOut);
            getClient.sendRequset();

        }else if(holder.getFunction().equalsIgnoreCase("POST")){ // create a new instance of PostClient
            this.status = "POST";

            postClient = new PostClient(type,sequenceNumber,serverAddress,holder,routerAddress,localPort,channel,seqNo,seqNoExpecting,seqNoReceived,seqNoSendOut, sendAndReceive);
            postClient.sendRequest();

        }
    }

    public boolean isReceiveSYNACK(){
        return receiveSYNACK;
    }
}
