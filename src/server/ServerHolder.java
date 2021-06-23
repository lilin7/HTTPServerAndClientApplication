package server;

import java.text.SimpleDateFormat;
import java.util.Date;

public class ServerHolder {
    private String function = "";
    private String serverName = "httpFileServer";
    private String localhostAddress = "127.0.0.6";
    private int portNumber = 8080;
    private boolean V=false;
    private String rootPath = "./src/server/WorkingDirectory";

    public String getFunction() {
		return function;
	}

	public void setFunction(String function) {
		this.function = function;
	}


	public String getServerName() {
        return serverName;
    }

    public int getPortNumber() {
		return portNumber;
	}

	public void setPortNumber(int portNumber) {
		this.portNumber = portNumber;
	}

	public String getLocalhostAddress() {
		return this.localhostAddress;
	}

	public void setLocalhostAddress(String localhostAddress) {
		this.localhostAddress = localhostAddress;
	}

    public void setV(boolean v) {
        V = v;
    }

    public boolean getV(){
        return V;
    }

    public void setRootPath(String rootPath) {
        this.rootPath = rootPath;
    }

    public String getRootPath() {
        return rootPath;
    }


    public String getCurrentDateTime(){

        Date now = new Date();
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        return simpleDateFormat.format(now);

    }

    public String getOriginAndUrl(){
        return "\"origin\": \"127.0.0.6\",\r\n\"url\": \"http://localhost:"+portNumber+"\"";
    }

    public String getHostName(){
        return "\"Host\": \"localhost\"";
    }

}
