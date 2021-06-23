package server;



import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class PostServer extends setConnection{

    public long seqNo;
    public long seqNoSendOut;
    public long seqNoExpecting;
    public long seqNoReceived;

    private DatagramChannel setConnectionChannel;
    private InetSocketAddress routerAddress;
    private int routerPort;
    private InetAddress peerAddress;
    private int peerPort;

    private ReceiveAndSend receiveAndSend;

    private String fullPostRequest;

    public long maxDataSeqNoReceived = 0; //to decide if all data packets are delivered (stored in hashmap) before FIN to disconnect

    long REQSeqNo; //to track the seq No. of the REQ, to decide the seq No. of the first data packet = REQSeqNo+1
    long minExpectingSeqNo;
    long maxExpectingSeqNo;

    public boolean REQACKDelivered = false;
    public boolean hasSentFINACK = false;

    public ConcurrentHashMap<Long, Packet> allPacketsForGet = new  ConcurrentHashMap<Long, Packet>();

    public PostServer(DatagramChannel datagramChannel, InetSocketAddress routerAddress,
                      InetAddress peerAddress, int peerPort,ReceiveAndSend receiveAndSend,
                      long seqNo,long seqNoExpecting,long seqNoReceived,long seqNoSendOut){
        this.setConnectionChannel = datagramChannel;
        this.routerAddress = routerAddress;
        this.routerPort = routerAddress.getPort();
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.receiveAndSend = receiveAndSend;
        this.seqNo = seqNo;
        this.seqNoExpecting = seqNoExpecting;
        this.seqNoReceived = seqNoReceived;
        this.seqNoSendOut = seqNoSendOut;
    }
    @Override
    public void receiveDATA(Packet newPacket){
        REQACKDelivered = true;
        if (newPacket.getType()==6) { //type = 6 DATA

            if (allPacketsForGet.containsKey(newPacket.getSequenceNumber())) { //has already been received before and put into Hashmap allPacketsForGet
                //the situation must be, server sent this pkt before, but the ACK to server is lost, so server re-send same pkt, so, re-send ACK on this pkt
                sendACK(newPacket);
            } else {
                if ( (newPacket.getSequenceNumber() < minExpectingSeqNo) || (newPacket.getSequenceNumber() > maxExpectingSeqNo)) {
                    // pkt received is not the ones in the window, do nothing = ditch this packet
                } else { //received correct data packets
                    sendACK(newPacket);
                    allPacketsForGet.put(newPacket.getSequenceNumber(), newPacket);

                    if (newPacket.getSequenceNumber()==minExpectingSeqNo) { //if received the min in window, then can silde window
                        minExpectingSeqNo+=1; //slide window
                        maxExpectingSeqNo = minExpectingSeqNo + 3;
                    } else {
                        //do nothing, wait for the minExpectingSeqNo packet
                    }
                }
            }
        }else if (newPacket.getType()==8) { //type = 8  FIN, server indicates this is the end of all data packets
            setMaxDataSeqNoReceived();
            //use maxDataSeqNoReceived to judge if have delivered all data packets before FIN
            if (newPacket.getSequenceNumber()==maxDataSeqNoReceived + 1) { //everything is ok
                System.out.println("Server receives FIN from client, seq No. "+ newPacket.getSequenceNumber() + ", type: " + newPacket.getType());

                if (!hasSentFINACK) { //if the first time receives FINACK, send back FINACK, and process data
                    sendFINACK(newPacket); //print is inside. send pkt type = 9  FIN-ACK (to ACK disconnect), don't wait for reply, don't listen anymore

                    ArrayList<Packet> allPacketsInArr = new ArrayList<>();
                    for (int i = 0; i < allPacketsForGet.size(); i++) {
                        allPacketsInArr.add(allPacketsForGet.get(REQSeqNo+1+i));
                    }
                    assemblePacket(allPacketsInArr); //setup fullPostRequest

                    // deal with the response data

                    String responseData = fullPostRequest; //receive response
                    System.out.println("---------Client Request:---------- \n" + fullPostRequest + "\n---------End of Client Request----------");
                    //System.out.println(responseData);
                    try {
                        receiveAndSend.receive(responseData);
                        receiveAndSend.process();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                } else { //if has sent FINACK before, means it was lost, only re-send FINACK again, don't process data, as has been processed before
                    sendFINACK(newPacket);
                }
            } else { //some data packets are lost, need to wait
                System.out.println("Server receives FIN packet from server, seq No. "+ seqNoReceived +", type: " + newPacket.getType());
                System.out.println("But some data packets are missing, waiting for server to re-send");
                //don't FIN-ACK, still wait for server to re-send some data packets
            }
        }
    }


    public void processRequestInfo(ReceiveAndSend receiveAndSend, Packet requestPacket){

        if (!receiveAndSend.getSucceed) { //if received a bad Post request
            //get the payload
            byte[] payloadByteArr = receiveAndSend.combineReplyToBadRequest();

            seqNoSendOut = seqNoReceived+1;
            //create a packet, use above payload, call send method in this class, to send as ACK to client, as a reply of a bad request
            Packet badReqPacket = new Packet(4, seqNoSendOut, peerAddress,peerPort, payloadByteArr); //type = 4  REQ-BAD

            try {
                setConnectionChannel.send(badReqPacket.readyToSend(),routerAddress);
                System.out.println("Server sends REQ-BAD packet to client, seq No. " + badReqPacket.getSequenceNumber() +", type: " + badReqPacket.getType());
                System.exit(0);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }else { //if received a good Post requehasSentREQACKst, make REQ-ACK to send to client
            sendREQACK(requestPacket);
            hasSentREQACK = true;
            REQSeqNo = seqNoReceived;
            minExpectingSeqNo = REQSeqNo+1;
            maxExpectingSeqNo = minExpectingSeqNo+3;
        }

    }

    public void sendREQACK(Packet requestPacket){
        String payloadStr = "";
        Packet REQACKPacket = new Packet(5, requestPacket.getSequenceNumber(), peerAddress,peerPort, payloadStr.getBytes()); //type = 5  REQ-ACK
        System.out.println("REQ-ACK to seq No. " + requestPacket.getSequenceNumber());
        try {
            send(REQACKPacket, this);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //to send ACK packets to DATA packets received, ACK packet seq No. is the same as the DATA packet it want to ack
    public void sendACK(Packet newPacket) {
        //send a ACK to ack the parameter DATA packet newPacket, so the seq No. is the same with this newpacket
        String payloadStr = "";
        byte[] byteArray = payloadStr.getBytes();
        Packet packetACK = new Packet(7, newPacket.getSequenceNumber(), peerAddress,peerPort,byteArray); //type: 7 ACK
        System.out.println("ACK seq No. " + packetACK.getSequenceNumber());
        //no need to re-send even if this packetACK is lost, so don't call send() to start thread
        try {
            this.setConnectionChannel.send(packetACK.readyToSend(),this.routerAddress);
            System.out.println("Server sends packet to client, seq No. " + packetACK.getSequenceNumber() +", type: " + packetACK.getType());
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFINACK(Packet finPacket) {
        //send a ACK to ack the parameter DATA packet newPacket, so the seq No. is the same with this newpacket
        String payloadStr = "";
        Packet packetACK = new Packet(9, finPacket.getSequenceNumber(), peerAddress,peerPort,payloadStr.getBytes()); //type: 7 ACK
        System.out.println("Server sends FIN-ACK to client, to ack seq No. " + finPacket.getSequenceNumber() + " whose type is " + finPacket.getType());
        //no need to re-send even if this packetACK is lost, so don't call send() to start thread
        try {
            this.setConnectionChannel.send(packetACK.readyToSend(),this.routerAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //to track if all data packets are delivered (stored in hashmap) before FIN to disconnect
    public void setMaxDataSeqNoReceived() {
        Set<Long> seqNoSet = allPacketsForGet.keySet();
        for (Long seqNo : seqNoSet) {
            if (seqNo > maxDataSeqNoReceived) {
                maxDataSeqNoReceived = seqNo;
            }
        }
    }

    private void assemblePacket(ArrayList<Packet> packets){
        fullPostRequest = "";
        for (Packet packet:
                packets) {
            String data = new String(packet.getPayload());
            fullPostRequest = fullPostRequest+data;
        }
    }

    public void send(Packet packet, PostServer postServer) throws IOException {
        setConnectionChannel.send(packet.readyToSend(),routerAddress);
        System.out.println("Server sends packet to client, seq No. " + packet.getSequenceNumber() +", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,postServer,packet);
        timeThread.start();
    }
}
