package client;
import java.util.HashMap;

/**
 * A holder class, holder object is to hold and set all status of the cURL input
 */
public class Holder {

    private String function;
    private boolean getV = false;
    private boolean postV = false;
    private boolean getH = false;
    private boolean postH = false;
    public boolean hascd = false;

    //for bonus funtion: -o
    private boolean getO = false;
    private String outputFileforO;

    private HashMap<String,String> headerMap = new HashMap<String,String>();
    private boolean postDataOrFile = false;
    private boolean postD = false;
    private boolean postF = false;
    private String data = "";
    private String file = "";
    private String URL;
    private String host;
    private int port;
    private String httpOrhttps = "";

    //-------begin for asg 2---------------
    private String fileName; //like "foo" or nothing
	private boolean hasContentType = false; //only for GET
    private String contentType = "";   //the content type of file, like "json/txt", //only for GET
    private boolean hasContentDisposition; //only for GET
    private String contentDisposition; //only for GET
    private String postContentBody = "";
    private boolean overwrite = true; //default overwrite

    public boolean hasO = false;
    public String fileNameForO = "";

	public String getFileName() {
		return fileName;
	}

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}

	public boolean isHasContentType() {
		return hasContentType;
	}

	public void setHasContentType(boolean hasContentType) {
		this.hasContentType = hasContentType;
	}

	public boolean isHasContentDisposition() {
		return hasContentDisposition;
	}

	public void setHasContentDisposition(boolean hasContentDisposition) {
		this.hasContentDisposition = hasContentDisposition;
	}

	public String getContentType() {
		return contentType;
	}

	public void setContentType(String contentType) {
		this.contentType = contentType;
	}

	public String getContentDisposition() {
		return contentDisposition;
	}

	public void setContentDisposition(String contentDisposition) {
		this.contentDisposition = contentDisposition;
	}
	public String getPostContentBody() {
		return postContentBody;
	}

	public void setPostContentBody(String postContentBody) {
		this.postContentBody = postContentBody;
	}


    public boolean isOverwrite() {
        return overwrite;
    }

    public void setOverwrite(boolean overwrite) {
        this.overwrite = overwrite;
    }
	//-------end for asg 2---------------
    
    public void setFunction(String function) {
        this.function = function;
    }

    public String getFunction() {
        return function;
    }

    public void setGetV(boolean getV) {
        this.getV = getV;
    }

    public boolean isGetV() {
        return getV;
    }

    public void setPostV(boolean postV) {
        this.postV = postV;
    }

    public boolean isPostV() {
        return postV;
    }

    public void setGetH(boolean getH) {
        this.getH = getH;
    }

    public boolean isGetH() {
        return getH;
    }

    public void setPostH(boolean postH) {
        this.postH = postH;
    }

    public boolean isPostH() {
        return postH;
    }

    public void setGetO(boolean getO) {
        this.getO = getO;
    }

    public boolean isGetO() {
        return getO;
    }

    public void setOutputFileforO(String s) {
        this.outputFileforO = s;
    }

    public String getOutputFileforO() {
        return this.outputFileforO;
    }

    public void setHeaderMap(String headerKey, String headerValue) {
        this.headerMap.put(headerKey,headerValue);
    }

    public HashMap<String, String> getHeaderMap() {
        return headerMap;
    }

    public void setPostDataOrFile(boolean postDataOrFile) {
        this.postDataOrFile = postDataOrFile;
    }

    public boolean isPostDataOrFile() {
        return postDataOrFile;
    }

    public void setPostD(boolean postD) {
        this.postD = postD;
    }

    public boolean isPostD() {
        return postD;
    }

    public void setPostF(boolean postF) {
        this.postF = postF;
    }

    public boolean isPostF() {
        return postF;
    }

    public void setData(String data) {
        this.data = this.data + data;
    }

    public String getData() {
        return data;
    }

    public void setFile(String file) {
        this.file = this.file + file;
    }

    public String getFile() {
        return file;
    }

    public void setURL(String URL) {
        this.URL = URL;
    }

    public String getURL() {
        return URL;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public String getHost() {
        return host;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public int getPort() {
        return port;
    }

    public String getHttpOrhttps() {
        return httpOrhttps;
    }
    public void setHttpOrhttps(String httpOrhttps) {
        this.httpOrhttps=httpOrhttps;
    }
}
