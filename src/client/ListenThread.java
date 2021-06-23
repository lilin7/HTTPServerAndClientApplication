package client;

import java.io.IOException;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class ListenThread extends Thread {
    private setConnection setConnection;
    private DatagramChannel channel;
    private int clientPort;
    private ByteBuffer buf = ByteBuffer
            .allocate(Packet.MAX_LEN);
    public ListenThread(int clientPort,setConnection setConnection,DatagramChannel channel)throws Exception{
        this.setConnection = setConnection;
        this.clientPort = clientPort;
        this.channel = channel;
    }

    @Override
    public void run(){
        while (true){
            buf.clear();

            try {
                SocketAddress router = channel.receive(buf);
                buf.flip();
                if(buf.limit()!=0){
                    setConnection.receivePacket(buf);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
