package server;

import client.GetClient;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;

public class GetServer extends setConnection {

    public long seqNo;
    public long seqNoSendOut;
    public long seqNoExpecting;
    public long seqNoReceived;

    private int payloadSize = 1000;

    private DatagramChannel setConnectionChannel;
    private InetSocketAddress routerAddress;
    private int routerPort;
    private InetAddress peerAddress;
    private int peerPort;

    private ArrayList<Long> ackedSeqArr = new ArrayList<>();
    private boolean hasSendFinPacket = false;

    public boolean hasReceivedFinACKPacket = false;

    public ArrayList<Packet> allPacketsToBeSent = new ArrayList<Packet>();
    private int packetNumber = 0;

    long minSendingSeqNo;
    long maxSendingSeqNo;
    public static long minAckedSeqNo = 0; //to decide if can slide window

    public GetServer(){
    }

    public GetServer(DatagramChannel datagramChannel, InetSocketAddress routerAddress,
                     InetAddress peerAddress,int peerPort,
                     long seqNo,long seqNoExpecting,long seqNoReceived,long seqNoSendOut){
        this.setConnectionChannel = datagramChannel;
        this.routerAddress = routerAddress;
        this.routerPort = routerAddress.getPort();
        this.peerAddress = peerAddress;
        this.peerPort = peerPort;
        this.seqNo = seqNo;
        this.seqNoExpecting = seqNoExpecting;
        this.seqNoReceived = seqNoReceived;
        this.seqNoSendOut = seqNoSendOut;
    }

    //parse request, call next method in this class, to send back content
    //only to deal with the request info from client, e.g. "get /foo Content-Type: txt"
    public void processRequestInfo(ReceiveAndSend receiveAndSend){
        //if reply to client request is not 200, only return some error message, then only one packet is enough, don't need to slide window
        if (!receiveAndSend.getSucceed) {
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
        } else { //reply to client request is 200, may need to send more than 1 packat, call other function to slide window
            seqNoSendOut = seqNoReceived+1;
            makeDataPackets(receiveAndSend); //create many packets to be sent, store in ArrayList<Packet> allPacketsToBeSent
            slideWindowSend(); //manipulate with allPacketsToBeSent, minSendingSeqNo, maxSendingSeqNo, to send whatever need to be sent
        }
    }

    //create content need to send back, divide into many packets, send many packets, wait for ACK, slide window
    //TODO: ask Jeffery: to be adjusted, now can keep sending??????
    public void slideWindowSend() {
        minSendingSeqNo = seqNoSendOut;
        maxSendingSeqNo = minSendingSeqNo + 3;

        for (int i = 0; i<allPacketsToBeSent.size(); i++) { //search in all packets
            //if is within the window
            if ( (! (allPacketsToBeSent.get(i).getSequenceNumber() < minSendingSeqNo)) && (! (allPacketsToBeSent.get(i).getSequenceNumber() > maxSendingSeqNo))) {
                Packet packetToBeSent = allPacketsToBeSent.get(i);
                try {
                    send(packetToBeSent, this);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //called by setConnection.receivePacket()
    @Override
    public void receiveACK(Packet ACKPacket){
        System.out.println("Server receives ACK from client, to ack seq No. "+ ACKPacket.getSequenceNumber());
        int delta = 0;
        long ackedSeqNo = ACKPacket.getSequenceNumber();
        for (int i = 0; i<allPacketsToBeSent.size(); i++) {
            if (allPacketsToBeSent.get(i).getSequenceNumber()==ackedSeqNo) {
                allPacketsToBeSent.remove(i);
                ackedSeqArr.add(ackedSeqNo);
                //break;
            }
        }
        if (minSendingSeqNo == ackedSeqNo) { //check if the min seq No. in window is received
            delta+=1;
            if(ackedSeqArr.contains(ackedSeqNo+1)){//check if the min seq No. in window is received and the second window is received
                delta+=1;
                if(ackedSeqArr.contains(ackedSeqNo+2)){//check if the min seq No. in window is received and the second and third window is received
                    delta+=1;
                    if(ackedSeqArr.contains(ackedSeqNo+3)){//check if the min seq No. in window is received and the second third and firth window is received
                        delta+=1;
                    }
                }
            }
        }
        //call slideWindowSend to see if can send anything
        windowDelta(delta);
    }

    //real slide window
    public void windowDelta(int slide){
        //the top of the packet in the window delivered
        if(slide!=0){
            if(allPacketsToBeSent.size()==0&&(!hasSendFinPacket)){// send fin packet
                String finPayload = "";
                Packet finPacket = new Packet(8,seqNoSendOut+packetNumber,peerAddress,peerPort,finPayload.getBytes());
                try {
                    send(finPacket, this); //after sending FIN, need to wait for FIN-ACK, then finish
                } catch (IOException e) {
                    e.printStackTrace();
                }
                hasSendFinPacket = true;
                System.out.println("Server has sent all data packets and FIN packet, waiting for FIN-ACK");
            }else if(allPacketsToBeSent.size()!=0&&(!hasSendFinPacket)){ //send delta packet
                for (int i = 0; i <allPacketsToBeSent.size() ; i++) {
                    if ( (! (allPacketsToBeSent.get(i).getSequenceNumber() < maxSendingSeqNo+1))
                            && (! (allPacketsToBeSent.get(i).getSequenceNumber() > maxSendingSeqNo+slide))) {
                        Packet packetToBeSent = allPacketsToBeSent.get(i);
                        try {
                            send(packetToBeSent, this);
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
                }
                minSendingSeqNo = minSendingSeqNo+slide;
                maxSendingSeqNo = maxSendingSeqNo+slide;
            }
        }

    }

    public void receiveFINACK(Packet packet){
        hasReceivedFinACKPacket = true;
        System.out.println("Server receives FINACK, seq No. " + packet.getSequenceNumber() +", type: " + packet.getType() + ", finish");
    }

    // to be called by this class

    public void send(Packet packet, GetServer getServer) throws IOException {
        setConnectionChannel.send(packet.readyToSend(),routerAddress);
        System.out.println("Server sends packet to client, seq No. " + packet.getSequenceNumber() +", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,getServer,packet);
        timeThread.start();
    }

    //divide String fullContentStrToBeSent into many packets, store those packets into ArrayList<Packet> allPacketsToBeSent
    public void makeDataPackets(ReceiveAndSend receiveAndSend) {
        String fullContentStrToBeSent = receiveAndSend.getFullContentStrToBeSent();
        byte[] getFullPayload = fullContentStrToBeSent.getBytes();
        packetNumber = ((int)(getFullPayload.length)/payloadSize)+1;
        long startSeqNumber = seqNoSendOut;

        for (int i = 0; i < packetNumber ; i++) {
            byte[] smallPayload = new byte[payloadSize];
            System.arraycopy(getFullPayload,i*payloadSize,smallPayload,0,Math.min(payloadSize,(getFullPayload.length-i*payloadSize)));
            Packet newPacket = new Packet(6,startSeqNumber+i,peerAddress,peerPort,smallPayload);
            allPacketsToBeSent.add(newPacket);
        }
        System.out.println("Server created " + packetNumber + " packets to be sent");
    }
}
