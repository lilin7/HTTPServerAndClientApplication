package client;


import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.util.ArrayList;

public class PostClient extends setConnection{

    public long seqNo;
    public long seqNoSendOut;
    public long seqNoExpecting;
    public long seqNoReceived;


    private int payloadSize = 1000;

    private int type;
    private long sequenceNumber;
    public InetAddress peerAddress;
    public int peerPort;
    private byte[] payload;
    private boolean receiveSYNACK = false;
    public static int timeoutTime = 10000;
    public Holder holder;
    private InetSocketAddress routerAddress;
    private InetSocketAddress serverAddress;
    private DatagramChannel setConnectionChannel;
    private int localPort;
    private InetSocketAddress localAddress;
    public SendAndReceive sendAndReceive;

    private ArrayList<Long> ackedSeqArr = new ArrayList<>();
    private boolean hasSendFinPacket = false;

    public ArrayList<Packet> allPacketsToBeSent = new ArrayList<Packet>();
    private int packetNumber;

    long REQSeqNo; //to track the seq No. of the REQ, to decide the seq No. of the first data packet = REQSeqNo+1
    long minExpectingSeqNo;
    long maxExpectingSeqNo;

    long minSendingSeqNo;
    long maxSendingSeqNo;
    public static long minAckedSeqNo = 0; //to decide if can slide window

    public boolean hasReceivedFINACK = false;
    public static boolean hasReceivedREQACK = false;

    public PostClient (int type, long sequenceNumber, InetSocketAddress serverAddress, Holder holder,
                       InetSocketAddress routerAddress, int localPort,DatagramChannel channel,
                       long seqNo,long seqNoExpecting,long seqNoReceived,long seqNoSendOut, SendAndReceive sendAndReceive){
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
        this.setConnectionChannel = channel;
        this.seqNo = seqNo;
        this.seqNoExpecting = seqNoExpecting;
        this.seqNoReceived = seqNoReceived;
        this.seqNoSendOut = seqNoSendOut;
        this.sendAndReceive = sendAndReceive;
    }

    public void newPacketFromServer(Packet newPacket){
        //this packet is from setConnection.receivePacket, analyse the packet here

        seqNoReceived = newPacket.getSequenceNumber();
        if (newPacket.getType()==4) { //type = 4  REQ-BAD
            String responseData = new String(newPacket.getPayload());
            System.out.println("Client receives REQ-BAD packet from server, seq No. "+ seqNoReceived +", type: " + newPacket.getType());
            System.out.println(responseData);
            System.exit(0);
        } else if (newPacket.getType()==5) {//type = 5  REQ-ACK
            //only when received REQ-ACK, begin to send data
            if (!hasReceivedREQACK) {
                hasReceivedREQACK = true;
                processRequestInfo(sendAndReceive);
            } else {
                //if has received REQACK before, means has begin to send
            }

        } else if (newPacket.getType()==7){//type = 7  ACK
            //receivedDataAsReplyToREQ = true; //means type 3 REQ has been received by server, no need to keep sending
            receiveACK(newPacket);
        } else if (newPacket.getType()==9) { //type = 9 FIN-ACK
            hasReceivedFINACK = true;
            System.out.println("Client receives FIN-ACK packet from server, finish");
        }
    }

    public void sendRequest(){
        ByteBuffer buffer = ByteBuffer.allocate(1013);

        //form request based on user input
        StringBuilder request = new StringBuilder();
        String requestLine = "";
        //if start with "cd " or ".." or "x:/", don't add / in front
        if ((holder.getFileName().length()>2 && holder.getFileName().substring(0,3).equalsIgnoreCase("cd "))
                || (holder.getFileName().length()>1 && holder.getFileName().substring(0,2).equals(".."))
                || (holder.getFileName().length()>2 && holder.getFileName().substring(1,3).equals(":/"))) {
            requestLine = holder.getFunction().toUpperCase()+" "+holder.getFileName() + " "+holder.getURL()+" HTTP/1.0\r\nConnection: Keep-Alive\r\n";
        } else { //normal situations, add / infront of file name
            requestLine = holder.getFunction().toUpperCase()+" "+"/"+holder.getFileName() + " "+holder.getURL()+" HTTP/1.0\r\nConnection: Keep-Alive\r\n";
        }

        request.append(requestLine);

        String headerLine = "Host: "+holder.getHost()+"\r\n";
        request.append(headerLine);

        if (holder.isHasContentType()) {
            request.append("Content-Type: " + holder.getContentType()).append("\r\n");
        }
        request.append("overwrite: " + holder.isOverwrite()).append("\r\n");

        //don't put the content here, this is only to send a post request, data will be from next packet
        if (!holder.getPostContentBody().equalsIgnoreCase("")) {
            request.append("Content-Body: ").append("\r\n");
        }

        request.append("\r\n");
        System.out.println();

        //TODO: Jeffery: this buffer does nothing????
        buffer.put(request.toString().getBytes());
        //transfer ByteBuffer buffer to byte[] byteArray, to be wrapped into packet
        byte[] byteArray = request.toString().getBytes();

        seqNoSendOut = seqNoReceived + 1;
        seqNoExpecting = seqNoSendOut + 1;
        Packet packetRequestInfo = new Packet(3, seqNoSendOut, peerAddress,peerPort,byteArray); //type=3: REQ
        REQSeqNo = packetRequestInfo.getSequenceNumber(); //to track the seq No. of the REQ, to decide the seq No. of the first data packet = REQSeqNo+1
        minExpectingSeqNo = REQSeqNo +1; //min number in window
        maxExpectingSeqNo = minExpectingSeqNo + 3; //max number in window

        buffer.clear();

        try {
            System.out.println("Sending request message REQ");
            //System.out.println("payload:\n--------------------\n" + request);
            //System.out.println("--------------------");
            send(packetRequestInfo, this); //send out the packet of type 3 REQ
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void processRequestInfo(SendAndReceive sendAndReceive){
        seqNoSendOut = REQSeqNo+1;
        makeDataPackets(sendAndReceive); //create many packets to be sent, store in ArrayList<Packet> allPacketsToBeSent
        slideWindowSend(); //manipulate with allPacketsToBeSent,
        // , maxSendingSeqNo, to send whatever need to be sent
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

    public void receiveACK(Packet ACKPacket){
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
                Packet finPacket = new Packet(8,seqNoSendOut+packetNumber,
                        peerAddress,peerPort,finPayload.getBytes());
                try {
                    setConnectionChannel.send(finPacket.readyToSend(),routerAddress);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                hasSendFinPacket = true;
                System.out.println("Client sends FIN packet to server, seq No. " + finPacket.getSequenceNumber());
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

    //divide String fullContentStrToBeSent into many packets, store those packets into ArrayList<Packet> allPacketsToBeSent
    public void makeDataPackets(SendAndReceive sendAndReceive) {
        String fullContentStrToBeSent = sendAndReceive.getFullContentStrToBeSent();
        byte[] getFullPayload = fullContentStrToBeSent.getBytes();
        packetNumber = ((int)(getFullPayload.length)/payloadSize)+1;
        long startSeqNumber = seqNoSendOut;

        for (int i = 0; i < packetNumber ; i++) {
            byte[] smallPayload = new byte[payloadSize];
            System.arraycopy(getFullPayload,i*payloadSize,smallPayload,0,Math.min(payloadSize,(getFullPayload.length-i*payloadSize)));
            Packet newPacket = new Packet(6,startSeqNumber+i,peerAddress,peerPort,smallPayload);
            allPacketsToBeSent.add(newPacket);
        }
    }

    public void send(Packet packet,PostClient sGP) throws IOException {
        this.setConnectionChannel.send(packet.readyToSend(),this.routerAddress);

        System.out.println("Client sends packet to client, seq No. " + packet.getSequenceNumber() +", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,sGP,packet);
        timeThread.start();
    }

    public void sendSlideWindow(){
        //todo:add send with slide window
    }
}
