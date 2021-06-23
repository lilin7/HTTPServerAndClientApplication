package server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class Debugging {

    ServerHolder serverHolder;

    public Debugging(ServerHolder serverHolder){
        this.serverHolder = serverHolder;
    }

    public void printDebugInfo(){

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

        System.out.println("server name: "+ serverHolder.getServerName());
        System.out.println("server address: "+serverHolder.getLocalhostAddress());
        System.out.println("listen port number: "+serverHolder.getPortNumber());
        System.out.println("root path is: "+serverHolder.getRootPath());
        System.out.println("Time: "+simpleDateFormat.format(now));
        System.out.println("--------------");

    }
}

