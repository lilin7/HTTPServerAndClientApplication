package server;

import java.net.InetSocketAddress;
import java.nio.channels.DatagramChannel;
import java.util.Scanner;

public class httpfs {

    //private static Parser parser;
    //private static ServerHolder serverHolder;
    //private static ReceiveAndSend receiveAndSend;



    public static void main(String[] args) throws Exception {
		ServerHolder serverHolder;
		Parser parser;
        Scanner sc = new Scanner(System.in);
    	while (true) {
            serverHolder = new ServerHolder();
	        System.out.println("------------------------");
	        System.out.println("Server: please input parameters");

	        String rawParameter = sc.nextLine();

	        //TODO: cannot handle if the directory has space
	        String[] parameters = rawParameter.split(" ");
	        parser = new Parser(parameters,serverHolder);
	        
	        boolean succeedParsing = parser.parameterAnalyse();	        
	        //if there is error found in the input, prompt user to input again		
	        if (!succeedParsing){
	        	continue;
	        } else {
                break;
			}
		}

    	//from here is LA3
    	int serverPort = 8007;
		DatagramChannel datagramChannel = DatagramChannel.open();
		datagramChannel.bind(new InetSocketAddress(serverPort));

		InetSocketAddress routerAddress = new InetSocketAddress("localhost",3000);
		setConnection setConnection = new setConnection(datagramChannel,routerAddress,serverHolder);

		Thread listenThread = new ListenThread(serverPort,setConnection,datagramChannel, serverHolder);
		listenThread.start();

		//what below is from LA2
		/*
		receiveAndSend = new ReceiveAndSend(serverHolder);

		Thread listenThread = new httpfsListenThread(receiveAndSend,serverHolder);
		listenThread.start();

		 */
    }
}
