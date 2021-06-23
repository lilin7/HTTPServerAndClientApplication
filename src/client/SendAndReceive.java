package client;

import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;

/**
 * Send request and receive response
 */
public class SendAndReceive {

    private String host;
    private int port;
    private Holder holder;
    private SocketChannel clientSocketChannel;

    //Constructor: build connection
    public SendAndReceive(Holder holder){
        this.holder = holder;
        this.host = holder.getHost();
        this.port = holder.getPort();
/*
        try{
            //create connection
            InetSocketAddress serverAddress = new InetSocketAddress(host,port);
            clientSocketChannel = SocketChannel.open();
            clientSocketChannel.connect(serverAddress);

            if(clientSocketChannel.finishConnect()){
                System.out.println("connect successfully!");
            }else{
                System.out.println("connect failed");
                System.exit(0);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

 */
    }

    public Holder getHolder() {
        return this.holder;
    }

    public SocketChannel getClientSocketChannel() {
        return clientSocketChannel;
    }

    /**
     * Send request
     */
    public void send(){
        ByteBuffer buffer = ByteBuffer.allocate(2048);
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
        
        if(holder.getFunction().equalsIgnoreCase("get")){
        	//if (holder.isHasContentType()) {
        		request.append("Content-Type: " + holder.getContentType()).append("\r\n");
        	//}
        	if (holder.isHasContentDisposition()) {
        		request.append("Content-Disposition: " + holder.getContentDisposition()).append("\r\n");
        	}
        }

        if(holder.getFunction().equalsIgnoreCase("post")){
            if (holder.isHasContentType()) {
                request.append("Content-Type: " + holder.getContentType()).append("\r\n");
            }

            request.append("Content-Body: " + holder.getPostContentBody()).append("\r\n");
            request.append("overwrite: " + holder.isOverwrite()).append("\r\n");
        }
        request.append("\r\n");
        System.out.println();

        try{
            //send request
            buffer.put(request.toString().getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            clientSocketChannel.write(buffer);
            buffer.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * receive response
     * @return response data
     * @throws Exception
     */
    public String receive() throws Exception {

        String responseData = "";
        ByteBuffer buffer = ByteBuffer.allocate(2048);
        StringBuffer stringBuffer = new StringBuffer();
        clientSocketChannel.read(buffer);
        buffer.flip();

        while(buffer.hasRemaining()){
            stringBuffer.append((char)buffer.get());
        }
        responseData = stringBuffer.toString().trim();
        return responseData;
    }

    public String getFullContentStrToBeSent() {

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

        if(holder.getFunction().equalsIgnoreCase("get")){
            //if (holder.isHasContentType()) {
            request.append("Content-Type: " + holder.getContentType()).append("\r\n");
            //}
            if (holder.isHasContentDisposition()) {
                request.append("Content-Disposition: " + holder.getContentDisposition()).append("\r\n");
            }
        }

        if(holder.getFunction().equalsIgnoreCase("post")){
            if (holder.isHasContentType()) {
                request.append("Content-Type: " + holder.getContentType()).append("\r\n");
            }
            request.append("overwrite: " + holder.isOverwrite()).append("\r\n");
            request.append("Content-Body: " + holder.getPostContentBody()).append("\r\n");

        }
        request.append("\r\n");
        String fullContentStrToBeSent = request.toString();
        return fullContentStrToBeSent;
    }
}
