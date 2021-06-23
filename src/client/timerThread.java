package client;


import java.io.IOException;

public class timerThread extends Thread {

    private int timeout=1000;

    private boolean isConnect = false;
    private boolean isGet = false;
    private boolean isPost = false;
    private setConnection setConnection;
    private GetClient getClient;
    private PostClient postClient;

    private Packet packet;

    //constructor if "setConnection"
    //who called timeThread, construct which timeThread
    public timerThread(int timeout,setConnection setConnection,Packet packet){
        this.timeout = timeout;
        this.setConnection = setConnection;
        this.packet = packet;
        isConnect = true;
    }

    //constructor if "GetClient"
    //who called timeThread, construct which timeThread
    public timerThread(int timeout,GetClient getClient,Packet packet){
        this.timeout = timeout;
        this.getClient = getClient;
        this.packet = packet;
        isGet = true;
    }
    //constructor if "PostClient"
    //who called timeThread, construct which timeThread
    public timerThread(int timeout,PostClient postClient,Packet packet){
        this.timeout = timeout;
        this.postClient = postClient;
        this.packet = packet;
        isPost = true;
    }


    //to be used in GetClient.newPacketFromServer
    public timerThread(int timeout,GetClient getClient,Packet packet, boolean occupied){
        this.timeout = timeout;
        this.getClient = getClient;
        this.packet = packet;
        isGet = true;
        occupied = false;
    }


    @Override
    public void run(){

        try {
            this.sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(isConnect){ //if timeThread is called by "setConnection":
            if(!setConnection.isReceiveSYNACK()){ //if fail handshaking, resend this packet
                try {
                    setConnection.send(packet,setConnection);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if(isGet){ //if timeThread is called by "GetClient":
            if(!GetClient.receivedDataAsReplyToREQ) {
                try {
                    getClient.send(packet, getClient); //send the getClient to server
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }


        }else if(isPost){ //if timeThread is called by "PostClient":
            if(!PostClient.hasReceivedREQACK) {
                try {
                    postClient.send(packet, postClient); //send the getClient to server
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }else if (postClient.allPacketsToBeSent.contains(packet)) {
                try {
                    postClient.send(packet,postClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            } else if (!postClient.hasReceivedFINACK) { //if has not received FINACK from server, keep sending FIN
                try {
                    postClient.send(packet,postClient);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

        } else {
            System.out.println("Error when calling client.timerThread");
        }

        //re-initialize
        isConnect = false;
        isGet = false;
        isPost = false;
    }
}
