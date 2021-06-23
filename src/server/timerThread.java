package server;

import java.io.IOException;

public class timerThread extends Thread {

    private int timeout=1000;

    private boolean isConnect = false;
    private boolean isGet = false;
    private boolean isPost = false;
    private setConnection setConnection;
    private GetServer getServer;
    private PostServer postServer;

    private Packet packet;
    public timerThread(int timeout, setConnection setConnection, Packet packet){
        this.timeout = timeout;
        this.setConnection = setConnection;
        this.packet = packet;
        isConnect = true;
    }
    public timerThread(int timeout, GetServer getServer, Packet packet){
        this.timeout = timeout;
        this.getServer = getServer;
        this.packet = packet;
        isGet = true;
    }
    public timerThread(int timeout, PostServer postServer, Packet packet){
        this.timeout = timeout;
        this.postServer = postServer;
        this.packet = packet;
        isPost = true;
    }
    @Override
    public void run(){

        try {
            sleep(timeout);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        if(isConnect){
            if(!setConnection.isRequestReceived()){
                try {
                    setConnection.send(packet,setConnection);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if(isGet){
            //because a packet will be removed from allPacketsToBeSent once it is acked
            //so if a packet is still in allPacketsToBeSent, means it is not acked, and need to be re-send
            if (getServer.allPacketsToBeSent.contains(packet) || !getServer.hasReceivedFinACKPacket) {
                try {
                    getServer.send(packet,getServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }else if(isPost){
            if (!postServer.REQACKDelivered) {
                try {
                    postServer.send(packet,postServer);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            //todo: something need to be add here to satisfy the post


        }

    }
}
