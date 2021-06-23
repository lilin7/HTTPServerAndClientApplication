package client;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.util.Scanner;

public class httpc {

    //private static SendAndReceive sendAndReceive;
    public static final int WINDOWSIZE = 4;
    public static void main(String[] args) throws Exception {
        Holder holder = new Holder();
        System.out.println("\n---------------------------------------------");
        System.out.println("Client: please input parameters");
        Scanner sc = new Scanner(System.in);
        String input = sc.nextLine();

        //in case of json data has space
        if(input.contains("-d")&&input.contains("{")&&input.contains("}")){
            String pretreatment = input.substring(input.indexOf("{"),input.indexOf("}")+1);
            String pretreatmentAfter = pretreatment.replaceAll(" ","");
            input = input.replace(pretreatment,pretreatmentAfter);
        }

        String[] parameters = input.split(" ");

        cmdParser cmdParser = new cmdParser(parameters,holder);
        //if there is error found in the input, prompt user to input again
        boolean succeedParsing = cmdParser.parameterAnalyse();
        if (!succeedParsing){
            System.out.println("Error: parse command failed.");
        } else {
            //from here is LA3

            SendAndReceive sendAndReceive = new SendAndReceive(holder);

            InetSocketAddress serverAddress = new InetSocketAddress("localhost",8007);
            InetSocketAddress routerAddress = new InetSocketAddress("localhost",3000);
            int localPort = 8008;
            setConnection setConnection = new setConnection(0,1,serverAddress,holder,routerAddress,localPort,sendAndReceive);
            setConnection.firstHandShake();
            Thread clientListenThread = new ListenThread(localPort,setConnection,setConnection.channel);
            clientListenThread.start();
        }
    }
}
