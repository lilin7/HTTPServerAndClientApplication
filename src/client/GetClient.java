package client;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class GetClient extends setConnection {

    public long seqNo;
    public long seqNoSendOut;
    public long seqNoExpecting;
    public long seqNoReceived;


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
    private DatagramChannel channel;
    private int localPort;
    private InetSocketAddress localAddress;

    private String fullResponseData;
    public String header = "";
    public static boolean receivedDataAsReplyToREQ = false; //when true, stop sending REQ in timerThread

    public static long maxDataSeqNoReceived = 0; //to decide if all data packets are delivered (stored in hashmap) before FIN to disconnect

    //to store all received packets, key is sequence number, value is the received packet
    public ConcurrentHashMap<Long, Packet> allPacketsForGet = new  ConcurrentHashMap<Long, Packet>();
    long REQSeqNo; //to track the seq No. of the REQ, to decide the seq No. of the first data packet = REQSeqNo+1
    long minExpectingSeqNo;
    long maxExpectingSeqNo;

    public boolean hasSentFINACK =false;


    public GetClient(int type, long sequenceNumber, InetSocketAddress serverAddress, Holder holder,
                     InetSocketAddress routerAddress, int localPort,DatagramChannel channel,
                     long seqNo,long seqNoExpecting,long seqNoReceived,long seqNoSendOut){
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
        this.channel = channel;
        this.seqNo = seqNo;
        this.seqNoExpecting = seqNoExpecting;
        this.seqNoReceived = seqNoReceived;
        this.seqNoSendOut = seqNoSendOut;
    }

    //to be called by a successful secondhandshake
    //only to send one single requestInfo, e.g. "get /foo Content-Type: txt"
    public void sendRequset(){
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

        //if (holder.isHasContentType()) {
        request.append("Content-Type: " + holder.getContentType()).append("\r\n");
        //}
        if (holder.isHasContentDisposition()) {
            request.append("Content-Disposition: " + holder.getContentDisposition()).append("\r\n");
        }

        request.append("\r\n");

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
            System.out.println("payload:\n--------------------\n" + request);
            System.out.println("--------------------");
            send(packetRequestInfo, this); //send out the packet of type 3 REQ
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    public void send(Packet packet,GetClient sGP) throws IOException {
        this.channel.send(packet.readyToSend(),this.routerAddress);

        System.out.println("Client sends packet to server, seq No. " + packet.getSequenceNumber() + ", type: " + packet.getType());
        Thread timeThread = new timerThread(timeoutTime,sGP,packet);
        timeThread.start();
    }

    //Get request: client only possibly receive type 4 REQ-BAD, 6 ACK, 8 FIN
    public void newPacketFromServer(Packet newPacket) throws Exception {
        //this packet is from setConnection.receivePacket, analyse the packet here
        seqNoReceived = newPacket.getSequenceNumber();
        if (newPacket.getType()==4) { //type = 4  REQ-BAD
            String responseData = new String(newPacket.getPayload());
            System.out.println("Client receives REQ-BAD packet from server, seq No. "+ seqNoReceived +", type: " + newPacket.getType());
            System.out.println(responseData);
            System.exit(0);
        } else if (newPacket.getType()==8) { //type = 8  FIN, server indicates this is the end of all data packets
            receivedDataAsReplyToREQ = true; //stop sending REQ in timerThread
            setMaxDataSeqNoReceived();
            //use maxDataSeqNoReceived to judge if have delivered all data packets before FIN
            if (newPacket.getSequenceNumber()==GetClient.maxDataSeqNoReceived + 1) { //everything is ok
                System.out.println("Client receives FIN packet from server, seq No. "+ seqNoReceived +", type: " + newPacket.getType());

                if (hasSentFINACK == false) {
                    sendFINACK(newPacket); //print is inside. send pkt type = 9  FIN-ACK (to ACK disconnect), don't wait for reply, don't listen anymore
                    hasSentFINACK = true;
                    ArrayList<Packet> allPacketsInArr = new ArrayList<>();
                    for (int i = 0; i < allPacketsForGet.size(); i++) {
                        allPacketsInArr.add(allPacketsForGet.get(REQSeqNo+1+i));
                    }
                    assemblePacket(allPacketsInArr);

                    // deal with the response data

                    String responseData = fullResponseData; //receive response


                    String[] headAndData = responseData.split("\r\n\r\n");
                    System.out.println("----------response data received from server is:------------");
                    System.out.println(responseData);
                    System.out.println("----------end of response data received from server---------");


                    String[] splitHeadResponseData = headAndData[0].split("\r\n");
                    String defaultFilePath = "./src/client/DownloadDirectory/";
                    String defaultFileName = "./src/client/DownloadDirectory/newfile";
                    String defaultFileType = ".txt";
                    String fullFileName = defaultFileName+defaultFileType;
                    String findType = "";

                    for(String everyLine:splitHeadResponseData){
                        if(everyLine.contains("Content-Type:")){
                            String[] splitContentType = everyLine.split(" ");
                            if (splitContentType.length>1) {
                                if (!splitContentType[1].isEmpty()) {
                                    if(splitContentType[1].contains("/")){
                                        findType = "." + splitContentType[1].split("/")[0];
                                        defaultFileType = "." + splitContentType[1].split("/")[0];
                                        fullFileName = defaultFileName + defaultFileType;
                                    } else{
                                        findType = "." + splitContentType[1];
                                        defaultFileType = "." + splitContentType[1];
                                        fullFileName = defaultFileName + defaultFileType;
                                    }

                                }
                            }
                        }
                    }


                    for(String everyLine:splitHeadResponseData){
                        if(everyLine.contains("Content-Disposition")){
                            String[] splitContentDisposition = everyLine.split(" ");
                            if(splitContentDisposition.length==1){
                                break;
                            }
                            if(splitContentDisposition[1].equals("inline")){
                                System.out.println("------------content-disposition is inline-------------");
                                System.out.println(headAndData[1]);
                            } else{
                                if(splitContentDisposition[1].contains("filename")){
                                    String fileNameWithQuotation = splitContentDisposition[1].substring(splitContentDisposition[1].indexOf("filename")+9,splitContentDisposition[1].length());
                                    String fileName = fileNameWithQuotation.replaceAll("\"","");
                                    if(!fileName.contains(".")){
                                        if(!findType.isEmpty()){
                                            fileName = fileName+findType;
                                        }
                                    }
                                    writeToFile(headAndData[1],defaultFilePath + fileName);
                                    System.out.println("------------content-disposition is attachment + filename-------------");
                                }else{
                                    System.out.println("------------content-disposition is attachment-------------");
                                    writeToFile(headAndData[1],fullFileName);
                                }
                            }
                        }
                    }

                    //for -o option
                    if (holder.hasO) {
                        writeToFile(headAndData[1],defaultFilePath + holder.fileNameForO);
                    }
                } else {
                    //has sent FIN-ACK and printed get content already, only send FIN-ACK to let server not to send FIN anymore
                    sendFINACK(newPacket); //print is inside. send pkt type = 9  FIN-ACK (to ACK disconnect), don't wait for reply, don't listen anymore
                }
            } else { //some data packets are lost, need to wait
                System.out.println("Client receives FIN packet from server, seq No. "+ seqNoReceived +", type: " + newPacket.getType());
                System.out.println("But some data packets are missing, waiting for server to re-send");
                //don't FIN-ACK, still wait for server to re-send some data packets
            }
        } else if (newPacket.getType()==6) { //type = 6 DATA
            receivedDataAsReplyToREQ = true; //stop sending REQ in timerThread

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
        }
    }

    //to send ACK packets to DATA packets received, ACK packet seq No. is the same as the DATA packet it want to ack
    public void sendACK(Packet newPacket) {
        //send a ACK to ack the parameter DATA packet newPacket, so the seq No. is the same with this newPacket
        String payloadStr = "";
        Packet packetACK = new Packet(7, newPacket.getSequenceNumber(), peerAddress,peerPort,payloadStr.getBytes()); //type: 7 ACK
        System.out.println("Client sends ACK to server, to ack seq No. " + packetACK.getSequenceNumber());
        //no need to re-send even if this packetACK is lost, so don't call send() to start thread
        try {
            this.channel.send(packetACK.readyToSend(),this.routerAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendFINACK(Packet finPacket) {
        //send a ACK to ack the parameter DATA packet newPacket, so the seq No. is the same with this newpacket
        String payloadStr = "";
        Packet packetACK = new Packet(9, finPacket.getSequenceNumber(), peerAddress,peerPort,payloadStr.getBytes()); //type: 9 FIN-ACK
        System.out.println("Client sends FIN-ACK to server, to ack seq No. " + finPacket.getSequenceNumber() + " whose type is " + finPacket.getType());
        //no need to re-send even if this packetACK is lost, so don't call send() to start thread
        try {
            this.channel.send(packetACK.readyToSend(),this.routerAddress);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    //to track if all data packets are delivered (stored in hashmap) before FIN to disconnect
    public void setMaxDataSeqNoReceived() {
        Set<Long> seqNoSet = allPacketsForGet.keySet();
        for (Long seqNo : seqNoSet) {
            if (seqNo > GetClient.maxDataSeqNoReceived) {
                GetClient.maxDataSeqNoReceived = seqNo;
            }
        }
    }

    //process all packets in ArrayList, to create a String with all data
    private void assemblePacket(ArrayList<Packet> packets){
        fullResponseData = "";
        for (Packet packet: packets) {
            String data = new String(packet.getPayload());
            fullResponseData = fullResponseData+data;
        }
    }

    public String getFullResponseData() {
        return fullResponseData;
    }

    /**
     * for option -o: write data to file path
     * @param data
     * @param path
     * @throws Exception
     */

    private static void writeToFile(String data, String path)throws Exception{
        File outputFile = new File(path);
        FileWriter writer = null;
        try {
            if (!outputFile.exists()) { //if file doesn't exist, create file
                outputFile.createNewFile();
            }
            writer = new FileWriter(outputFile);
            writer.append(data);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != writer)
                writer.close();
        }
    }


}
