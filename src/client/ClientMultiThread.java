package client;

import java.net.InetSocketAddress;
import java.util.Scanner;

public class ClientMultiThread extends Thread {

    private int port;
    private String IPAddress;
    private String requestLine;
    private cmdParser cmdParser;
    public Holder holder;

    public ClientMultiThread(String requestLine,int port,String IPAddress){
        this.requestLine = requestLine;
        this.port = port;
        this.IPAddress = IPAddress;
    }

    @Override
    public void run(){

        holder = new Holder();

        String input = requestLine;

        //in case of json data has space
        if(input.contains("-d")&&input.contains("{")&&input.contains("}")){
            String pretreatment = input.substring(input.indexOf("{"),input.indexOf("}")+1);
            String pretreatmentAfter = pretreatment.replaceAll(" ","");
            input = input.replace(pretreatment,pretreatmentAfter);
        }

        String[] parameters = input.split(" ");

        cmdParser = new cmdParser(parameters,holder);
        //if there is error found in the input, prompt user to input again
        boolean succeedParsing = false;
        try {
            succeedParsing = cmdParser.parameterAnalyse();
        } catch (Exception e) {
            e.printStackTrace();
        }
        if (!succeedParsing){
            System.out.println("Error: parse command failed.");
        } else {
            //from here is LA3

            SendAndReceive sendAndReceive = new SendAndReceive(holder);

            InetSocketAddress serverAddress = new InetSocketAddress("localhost",8007);
            InetSocketAddress routerAddress = new InetSocketAddress("localhost",3000);
            int localPort = port;
            setConnection setConnection = new setConnection(0,1,serverAddress,holder,routerAddress,localPort,sendAndReceive);
            try {
                setConnection.firstHandShake();
                Thread clientListenThread = new ListenThread(localPort,setConnection,setConnection.channel);
                clientListenThread.start();
            } catch (Exception e) {
                e.printStackTrace();
            }


        }

    }


}
