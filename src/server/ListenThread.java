package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.channels.DatagramChannel;

public class ListenThread extends Thread{
    private setConnection setConnection;
    private DatagramChannel channel;
    private int serverPort;
    private ServerHolder serverHolder;
    public boolean isBadRequest = false;

    private ByteBuffer buf = ByteBuffer
            .allocate(Packet.MAX_LEN)
            .order(ByteOrder.BIG_ENDIAN);
    public ListenThread(int serverPort,setConnection setConnection,DatagramChannel channel)throws Exception{
        this.setConnection = setConnection;
        this.serverPort = serverPort;
        this.channel = channel;
    }
    public ListenThread(int serverPort,setConnection setConnection,DatagramChannel channel, ServerHolder serverHolder)throws Exception{
        this.setConnection = setConnection;
        this.serverPort = serverPort;
        this.channel = channel;
        this.serverHolder = serverHolder;
    }

    @Override
    public void run(){
        while (true){
            setConnection.serverHolder = this.serverHolder;

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
