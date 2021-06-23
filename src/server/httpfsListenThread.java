package server;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;

public class httpfsListenThread extends Thread {

    private ReceiveAndSend receiveAndSend;
    private ServerHolder serverHolder;
    private int portNumber;
    private ServerSocketChannel serverSocketChannel;
    private SocketChannel clientSocketChannel;

    public httpfsListenThread(ReceiveAndSend receiveAndSend, ServerHolder serverHolder) throws IOException {
        this.receiveAndSend = receiveAndSend;
        this.serverHolder = serverHolder;
        this.portNumber = serverHolder.getPortNumber();

        System.out.println("Server is ready to listen");
        InetSocketAddress address = new InetSocketAddress(portNumber);
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.socket().bind(address);
    }

    @Override
    public void run(){
        while(true){
            try {
                clientSocketChannel = serverSocketChannel.accept();
            } catch (IOException e) {
                e.printStackTrace();
            }
            receiveAndSend.setClientSocketChannel(clientSocketChannel);
            ByteBuffer buffer = ByteBuffer.allocate(2048);
            StringBuffer stringBuffer = new StringBuffer();
            try {
                clientSocketChannel.read(buffer);
            } catch (IOException e) {
                e.printStackTrace();
            }
            buffer.flip();

            while(buffer.hasRemaining()){
                stringBuffer.append((char)buffer.get());
            }
            String data = stringBuffer.toString().trim();
            buffer.clear();
            try {
                receiveAndSend.receive(data);
                boolean successProcess  = receiveAndSend.process();
                receiveAndSend.send(clientSocketChannel);

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
