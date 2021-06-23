package server;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

public class ReceiveAndSend {

    public ServerHolder serverHolder;

    private ServerSocketChannel serverSocketChannel;
    private SocketChannel clientSocketChannel;
    private String requestData = "";
    private String localhostAddress;
    private int portNumber;

    private String responseStat = "";
    private String contentType = "";
    private ArrayList<String> contentTypeAL = new ArrayList<String>();
    private String contentDisposition = "";
    private String contentBody = "";
    private boolean overwrite = true;
    private String replyData = "";
    private ArrayList<String> fileAndFolderNamesAL = new ArrayList<String>();
    public boolean getSucceed = false;

    String[] requestDataArray;
    String[] firstLineArray;
   	
	public ReceiveAndSend() {
		this.serverHolder = serverHolder;
        this.localhostAddress = serverHolder.getLocalhostAddress();
        this.portNumber = serverHolder.getPortNumber();
	}

	public ReceiveAndSend(ServerHolder serverHolder) throws IOException {
        this.serverHolder = serverHolder;
        this.localhostAddress = serverHolder.getLocalhostAddress();
        //System.out.println("Server host address is: "+ localhostAddress);
        this.portNumber = serverHolder.getPortNumber();
        //System.out.println("Port is: "+ portNumber);
    }

    public void setClientSocketChannel(SocketChannel clientSocketChannel) {
        this.clientSocketChannel = clientSocketChannel;
    }

    public void receive(String data) throws IOException {
        requestData = data;

        //requestData: data received from client's request, can be the parameter of process()
        //System.out.println("---------Client Request:---------- \n" + requestData + "\n---------End of Client Request----------");
    }

    // parse the requestData received from client
    // check syntax, logic, privilege, validation, error handling
    public boolean process(){
        String rootPath = serverHolder.getRootPath();
        String fileName = ""; //file name like "bar"

        requestDataArray = requestData.split("\r\n"); //String[]
    	firstLineArray = requestDataArray[0].split(" "); //String[]

    	//clean data from previous run
        responseStat = "";
        contentType = "";
        contentTypeAL = new ArrayList<String>();
        contentDisposition = "";
        contentBody = "";
        overwrite = true;
        replyData = "";
        fileAndFolderNamesAL = new ArrayList<String>();

        //if(!securityCheck(firstLineArray[1])){
        if (false){
            responseStat = "HTTP/1.0 403 FORBIDDEN";
            replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
            return false;
        }else{
            //process the file name
            String file = firstLineArray[1];
            if (file.equals("/")) {
                fileName = "";
            } else {
                if (file.indexOf("/")!=0) { // if / is not the first char
                    //e.g. GET /..  or GET /../.. or GET .....
                    if (file.length()==2) {
                        if (file.equals("..") || file.substring(0,2).equalsIgnoreCase("cd")){ //e.g. GET ..  or POST ..
                            responseStat = "HTTP/1.0 403 FORBIDDEN";
                            replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                            return false;
                        } else {
                            responseStat = "HTTP/1.0 400 BAD REQUEST";
                            replyData = "HTTP ERROR 400\nError input.";
                            return false;
                        }
                    } else if ((file.length()>2)){ // if GE
                        if (file.substring(0,3).equals("../") || file.substring(1,3).equals(":/")) {
                            responseStat = "HTTP/1.0 403 FORBIDDEN";
                            replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                            return false;
                        }
                        else {
                            responseStat = "HTTP/1.0 400 BAD REQUEST";
                            replyData = "HTTP ERROR 400\nError input.";
                            return false;
                        }
                    } else {
                        responseStat = "HTTP/1.0 400 BAD REQUEST";
                        replyData = "HTTP ERROR 400\nError input.";
                        return false;
                    }
                } else { //length is at least 1, because above validation
                    //security check, the input path like "server/WorkingDirectory/foo.txt"
                    String inputFileString = file.substring(1); //get file name like "server/WorkingDirectory/foo.txt"
                    String[] inputFileStringArr = inputFileString.split("/");

                    //from now on, length is at least 1, because above validation

                    if (inputFileStringArr.length==1) { //from now on, length is at least 1, because above validation
                        if (inputFileStringArr[0].equals("WorkingDirectory")){
                            //e.g. /WorkingDirectory
                            fileName = ""; //list under current folder
                        } else {
                            //e.g. /foo.txt, so path no change, fileName = "foo.txt"
                            fileName = inputFileStringArr[0];
                        }
                    } else if (inputFileStringArr.length==2) {
                        if (inputFileStringArr[0].equals("WorkingDirectory")) {
                            if (folderExists(rootPath, inputFileStringArr[1])) { //if it's a folder name, e.g. /WorkingDirectory/SubDirectory
                                rootPath = serverHolder.getRootPath() + "/"+inputFileStringArr[1];
                                fileName = "";
                            } else { //e.g. /WorkingDirectory/foo.txt, so path no change, fileName = "foo.txt"
                                fileName = inputFileStringArr[1]; //get file name like "foo"
                            }
                        } else if (inputFileStringArr[0].equals("server")) {
                            if (inputFileStringArr[1].equals("WorkingDirectory")) {
                                fileName = ""; //list file names in current directory
                            } else {
                                responseStat = "HTTP/1.0 403 FORBIDDEN";
                                replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                return false;
                            }
                        } else {
                            responseStat = "HTTP/1.0 403 FORBIDDEN";
                            replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                            return false;
                        }
                    } else if (inputFileStringArr.length==3) {
                        if (inputFileStringArr[0].equals("src")) {
                            if ( inputFileStringArr[1].equals("server")) {
                                if (inputFileStringArr[2].equals("WorkingDirectory")) {
                                    fileName = ""; //list file names in current directory
                                } else {
                                    responseStat = "HTTP/1.0 403 FORBIDDEN";
                                    replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                    return false;
                                }
                            }
                        } else if (inputFileStringArr[0].equals("server")) {
                            if ( inputFileStringArr[1].equals("WorkingDirectory")) {
                                // situation like input is /server/WorkingDirectory/foo.txt
                                // if start with server, then next must be WorkingDirectory
                                // so path no change, fileName = "foo.txt"
                                fileName = inputFileStringArr[2]; //get file name like "foo"
                            } else {
                                responseStat = "HTTP/1.0 403 FORBIDDEN";
                                replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                return false;
                            }
                        } else if (inputFileStringArr[0].equals("WorkingDirectory")) {
                            // situation like input is /WorkingDirectory/SubDirectory/foo.txt
                            // if start with WorkingDirectory, then check if next is a folder in current path
                            if (folderExists(rootPath, inputFileStringArr[1])) {
                                // if the next param is a subfolder of WorkingDirectory, set path to /WorkingDirectory/SubDirectory
                                rootPath = serverHolder.getRootPath() + "/"+inputFileStringArr[1];
                                fileName = inputFileStringArr[2]; //get file name like "foo"
                            } else {
                                responseStat = "HTTP/1.0 403 FORBIDDEN";
                                replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                return false;
                            }
                        } else {
                            responseStat = "HTTP/1.0 403 FORBIDDEN";
                            replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                            return false;
                        }
                    } else if (inputFileStringArr.length==4) {
                        if (inputFileStringArr[0].equals("src")) {
                            if (inputFileStringArr[1].equals("server")) {
                                if (inputFileStringArr[2].equals("WorkingDirectory")) {
                                    fileName = inputFileStringArr[3]; //get file name like "foo"
                                } else {
                                    responseStat = "HTTP/1.0 403 FORBIDDEN";
                                    replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                    return false;
                                }
                            }
                        } else if (inputFileStringArr[0].equals("server")) {
                            if (inputFileStringArr[1].equals("WorkingDirectory")) {
                                if (folderExists(rootPath, inputFileStringArr[2])) {
                                    rootPath = serverHolder.getRootPath() + "/" + inputFileStringArr[2];
                                    fileName = inputFileStringArr[3]; //get file name like "foo"
                                }else {
                                    responseStat = "HTTP/1.0 403 FORBIDDEN";
                                    replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                    return false;
                                }
                            } else {
                                responseStat = "HTTP/1.0 403 FORBIDDEN";
                                replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                                return false;
                            }
                        } //TODO: if need to consider there are 2 layers of subfolder in WorkingDirectory, continue here to write else if
                    } else {
                        responseStat = "HTTP/1.0 403 FORBIDDEN";
                        replyData = "HTTP ERROR 403\nRequest forbidden for security reasons. The requested file is located outside the working directory.";
                        return false;
                    }
                }
            }

            setContentType(requestDataArray); //set ArrayList<String> of Content-Type, for both GET and POST
            setContentDisposition(requestDataArray);

            if (firstLineArray[0].equalsIgnoreCase("get")) {
                serverHolder.setFunction("get");

                fileAndFolderNamesAL = scanFiles(rootPath); //all files and folders in the directory
                ArrayList<String> filesOfWantedTypeAL = new ArrayList<String>();

                if (fileName.equals("")) { //situation like "GET /", if only "/" return a list of the current files in the data directory
                    //if Content-Type is in input, filter only the wanted content type
                    if (!contentType.equals("")) {
                        filesOfWantedTypeAL = filterFileType(fileAndFolderNamesAL, contentTypeAL);
                    } else {
                        filesOfWantedTypeAL = fileAndFolderNamesAL;
                    }

                    StringBuffer sb = new StringBuffer();
                    sb.append("List current files in the data directory as below: \n");
                    for (String name : filesOfWantedTypeAL) {
                        sb.append(name +"\n");
                    }
                    replyData = sb.toString().trim();
                    getSucceed = true;
                    return true;

                } else { // situation like GET /foo
                    //TODO: write bonus part here (Content-Type, Content-Disposition)

                    String completeFileName = "";
                    String targetFileName = ""; //the final want-to-be-printed file name
                    ArrayList<String> allTargetFileNames = new ArrayList<String>(); //if there are multiple files if type not defined

                    if (fileName.indexOf(".") != -1) { //input is foo.txt
                        int indexOfLastDot = fileName.lastIndexOf(".");
                        String extention = fileName.substring(indexOfLastDot+1).toLowerCase();

                        if (contentType.equals("")) { //if input is foo.txt and without defining Content-Type, use the foo.txt
                            targetFileName = fileName;
                        } else { //if input both write foo.txt and also define Content-Type
                            if (contentTypeAL.contains(extention)) { //no contradiction, e.g. if foo.txt and Content-Type: txt/json
                                targetFileName = fileName;
                            } else { //this is handled in client already. contradiction, e.g. if foo.txt and Content-Type: xml/json
                                responseStat = "HTTP/1.0 400 BAD REQUEST";
                                replyData = "HTTP ERROR 400\nError input. File extention and Content-Type are contradiction.";
                                return false;
                            }
                        }
                    } else { //input is foo, no extention follows
                        if (! contentType.equals("")) { //defined Content-Type
                            filesOfWantedTypeAL = filterFileType(fileAndFolderNamesAL, contentTypeAL);
                            for (String type : contentTypeAL) {
                                completeFileName = fileName+"."+type;
                                if (filesOfWantedTypeAL.contains(completeFileName)) { //if file exists
                                    allTargetFileNames.add(completeFileName);
                                } else { //if file doesn't exit, check next requested type
                                    continue;
                                }
                            }
                            if (allTargetFileNames.size()==0) { //can't find targe file name
                                responseStat = "HTTP/1.0 404 NOT FOUND";
                                replyData = "HTTP ERROR 404\nFile doesn't exist!";
                                return false;
                            }

                        } else { //input is foo, no extention, and not define Content-Type
                            fileAndFolderNamesAL = scanFiles(rootPath); //all files and folders in the directory
                            for (String eachFile : fileAndFolderNamesAL) {
                                String pureName;
                                int indexOfLastDot = eachFile.lastIndexOf(".");
                                if (indexOfLastDot!=-1) {
                                    pureName = eachFile.substring(0, indexOfLastDot);
                                } else {
                                    pureName = eachFile;
                                }

                                if (pureName.equals(fileName)) { //the one in the folder, is the one looking for
                                    allTargetFileNames.add(eachFile);
                                }
                            }
                        }
                    }

                    if (allTargetFileNames.size()==0) { //if only one target file, never touched this ArrayList
                        //check if file exist in folder
                        String fullPath = rootPath+"/"+targetFileName;
                        File tempFile = new File(fullPath);
                        if (tempFile.exists() && !tempFile.isDirectory()) { //if file exists already or is it a folder
                            responseStat = "HTTP/1.0 200 OK";
                            getSucceed = true; //identifier: going to send many packets from next pkt
                            replyData = readFile(fullPath); //set data ready to send back to client
                        } else { //if file doesn't exist, return status code HTTP ERROR 404
                            responseStat = "HTTP/1.0 404 NOT FOUND";
                            replyData = "HTTP ERROR 404\nFile doesn't exist!";
                        }
                    } else {
                        StringBuffer replySB = new StringBuffer();
                        boolean allAreDirectory = true;

                        for (String eachTargetFileName : allTargetFileNames) {
                            String fullPath = rootPath + "/" + eachTargetFileName;
                            File tempFile = new File(fullPath);

                            if (!tempFile.exists()) {
                                continue;
                            }
                            if (tempFile.isDirectory()) { //if file doesn't exist or is directory, return status code HTTP ERROR 404
                                continue;
                            } else {
                                allAreDirectory = false; //even if there is one file, this becomes false
                                String contentOfFile = readFile(fullPath); //set data ready to send back to client
                                replySB.append("Content of file " + eachTargetFileName + ":\n");
                                replySB.append(contentOfFile);
                                replySB.append("\n");
                            }
                        }

                        if (allAreDirectory) {
                            responseStat = "HTTP/1.0 404 NOT FOUND";
                            replyData = "HTTP ERROR 404\nFile doesn't exist!";
                            return false;
                        } else {
                            responseStat = "HTTP/1.0 200 OK";
                            getSucceed = true; //identifier: going to send many packets from next pkt
                            replyData = replySB.toString();
                            return true;
                        }
                    }
                }
            } else if (firstLineArray[0].equalsIgnoreCase("post")) {
                serverHolder.setFunction("post");
                setContentBody(requestDataArray); //setup what to be posted, setup overwrite value

                boolean successSetOverwrite = setOverwrite(requestDataArray);
                if(!successSetOverwrite) {
                    return false; //failed process overwrite
                }

                //e.g. POST /    POST /SubDirectory
                if (fileName.equals("")) {
                    responseStat = "HTTP/1.0 400 BAD REQUEST";
                    replyData = "HTTP ERROR 400\nError input. Must define the file name to be posted to.";
                    return false;
                }

                if (!contentType.equals("")){ //if defined Content-Type in input
                    String totalFileName = "";
                    if (contentTypeAL.size()==0){
                        responseStat = "HTTP/1.0 400 BAD REQUEST";
                        replyData = "HTTP ERROR 400\nError input. Re-enter Content-Type.";
                        return false;
                    } else if (contentTypeAL.size()>1) { //e.g. json/txt -> [json][txt]
                        responseStat = "HTTP/1.0 400 BAD REQUEST";
                        replyData = "HTTP ERROR 400\nError input. Only one Content-Type could be defined when post.";
                        return false;
                    } else { //only one Content-Type is in input
                        totalFileName = fileName + "." + contentTypeAL.get(0);
                    }
                    fileName = totalFileName;
                }

                //check if file exist in folder
                String fullPath = rootPath+"/"+fileName;
                File tempFile = new File(fullPath);
                if (tempFile.exists() && !tempFile.isDirectory()) { //if file exists already
                    if (overwrite) {
                        writeToFile(contentBody, fullPath, true);
                        responseStat = "HTTP/1.0 200 OK";
                        replyData = "Post succeeds. File exists already, overwrite content.";
                        getSucceed = true;
                    } else if (!overwrite) {
                        writeToFile(contentBody, fullPath, false);
                        responseStat = "HTTP/1.0 200 OK";
                        replyData = "Post succeeds. File exists already, append to original content.";
                        getSucceed = true;
                    }

                } else { //if file doesn't exist, create file and write contentBody into it
                    try {
                        tempFile.createNewFile();
                        writeToFile(contentBody, fullPath, overwrite);
                        responseStat = "HTTP/1.0 200 OK";
                        replyData = "Post succeeds. File doesn't exist, created file and write to it.";
                        getSucceed = true;
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            } else {
                responseStat = "HTTP/1.0 400 BAD REQUEST";
                replyData = "HTTP ERROR 400\nError input. First parameter must be 'GET' or 'POST'";
                return false;
            }
            getSucceed = true;
            return true;
        }
    }

    public byte[] combineReplyToBadRequest() {
        StringBuilder sendBody = new StringBuilder();
        sendBody.append(replyData).append("\r\n");
        sendBody.append("\r\n");

        StringBuilder sendHeader = new StringBuilder();
        sendHeader.append(responseStat).append("\r\n");

        sendHeader.append("Date: ").append(serverHolder.getCurrentDateTime()).append("\r\n");
        sendHeader.append("Server: ").append(serverHolder.getServerName()).append("\r\n");
        sendHeader.append("Content-Length: ").append(sendBody.toString().length()).append("\r\n");
        sendHeader.append("Content-Disposition: ").append(contentDisposition).append("\r\n");
        sendHeader.append("Content-Type: ").append(contentType).append("\r\n");

        if (serverHolder.getFunction().equalsIgnoreCase("post")) {
            sendHeader.append("Content-Body: ").append(contentBody).append("\r\n");
        }
        sendHeader.append("Connection: Keep-alive").append("\r\n\r\n");

        ByteBuffer buffer = ByteBuffer.allocate(1013);
        String response = sendHeader.append(sendBody).toString();
        System.out.println("---------Server Response:---------- \n" + response + "\n---------End of Server Response----------");

        byte[] byteArr = response.getBytes(StandardCharsets.UTF_8);

	    return byteArr;
    }

    public String getFullContentStrToBeSent() {

        StringBuilder sendBody = new StringBuilder();
        sendBody.append(replyData).append("\r\n");
        sendBody.append("\r\n");

        StringBuilder sendHeader = new StringBuilder();
        sendHeader.append(responseStat).append("\r\n");

        sendHeader.append("Date: ").append(serverHolder.getCurrentDateTime()).append("\r\n");
        sendHeader.append("Server: ").append(serverHolder.getServerName()).append("\r\n");
        sendHeader.append("Content-Length: ").append(sendBody.toString().length()).append("\r\n");
        sendHeader.append("Content-Disposition: ").append(contentDisposition).append("\r\n");

        //if (!contentType.equalsIgnoreCase("")) { //only print Content-Type if defined
        sendHeader.append("Content-Type: ").append(contentType).append("\r\n");
        //}

        if (serverHolder.getFunction().equalsIgnoreCase("post")) {
            sendHeader.append("Content-Body: ").append(contentBody).append("\r\n");
        }
        sendHeader.append("Connection: Keep-alive").append("\r\n\r\n");

        ByteBuffer buffer = ByteBuffer.allocate(2048);
        String response = sendHeader.append(sendBody).toString();

        return response;
    }


    //for "GET", send back what client should get
    public void send(SocketChannel client){
        //TODO: edit replyData (add headers etc.) to send back to client, both for "GET" and "send"

        //post also need to send response
        //the code below is just a test in order to form a well loop to client, if not there would be mistake

        StringBuilder sendBody = new StringBuilder();
        sendBody.append(replyData).append("\r\n");
        sendBody.append("\r\n");

        StringBuilder sendHeader = new StringBuilder();
        sendHeader.append(responseStat).append("\r\n");

        sendHeader.append("Date: ").append(serverHolder.getCurrentDateTime()).append("\r\n");
        sendHeader.append("Server: ").append(serverHolder.getServerName()).append("\r\n");
        sendHeader.append("Content-Length: ").append(sendBody.toString().length()).append("\r\n");
        sendHeader.append("Content-Disposition: ").append(contentDisposition).append("\r\n");

        //if (!contentType.equalsIgnoreCase("")) { //only print Content-Type if defined
            sendHeader.append("Content-Type: ").append(contentType).append("\r\n");
        //}

        if (serverHolder.getFunction().equalsIgnoreCase("post")) {
            sendHeader.append("Content-Body: ").append(contentBody).append("\r\n");
        }
        sendHeader.append("Connection: Keep-alive").append("\r\n\r\n");

        ByteBuffer buffer = ByteBuffer.allocate(2048);
        String response = sendHeader.append(sendBody).toString();
        //System.out.println("---------Server Response:---------- \n" + response + "\n---------End of Server Response----------");
        try{
            buffer.put(response.getBytes(StandardCharsets.UTF_8));
            buffer.flip();
            client.write(buffer);
            buffer.clear();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static ArrayList<String> contentTypeStringArrayToArrayList(String contentTypeStr) {
        String[] filePathAndNameArray = contentTypeStr.trim().toLowerCase().split("/");
        ArrayList<String> filePathAndNameAL = new ArrayList<String>(Arrays.asList(filePathAndNameArray));
        return filePathAndNameAL;
    }

    /**
     * parse "src/server/WorkingDirectory/foo" to an ArrayList of 4 elements
     * @param filePathAndName
     * @return
     */
    private static ArrayList<String> parseFilePathAndName(String filePathAndName){
        String[] filePathAndNameArray = filePathAndName.trim().split("/");
        ArrayList<String> filePathAndNameAL = new ArrayList<String>(Arrays.asList(filePathAndNameArray));
        return filePathAndNameAL;
    }

    private static ArrayList<String> scanFiles(String path) {
        List<String> fileNames = new ArrayList<String>();
        LinkedList<File> list = new LinkedList<File>();
        File dir = new File(path);
        File[] file = dir.listFiles();

        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory() || file[i].exists()) {
                list.add(file[i]);
            }
            fileNames.add(file[i].getName());
        }
        ArrayList<String> fileAndFolderNamesAL = (ArrayList)fileNames;
        return fileAndFolderNamesAL;
    }

    //Below method is to return all files and sub-folders and so on inside all layers of sub-folders, not used
    private static ArrayList<String> scanFilesMultiLayers(String path) {
        List<String> fileNames = new ArrayList<String>();
        LinkedList<File> list = new LinkedList<File>();
        File dir = new File(path);
        File[] file = dir.listFiles();

        for (int i = 0; i < file.length; i++) {
            if (file[i].isDirectory()) {
                list.add(file[i]);
            }
            fileNames.add(file[i].getName());
        }

        while (!list.isEmpty()) {
            File tmp = list.removeFirst(); //pop from list
            // if get a folder
            if (tmp.isDirectory()) {
                // list files under this folder
                file = tmp.listFiles();
                if (file == null) {// empty folder
                    continue;
                }
                // traverse
                for (int i = 0; i < file.length; ++i) {
                    if (file[i].isDirectory()) {
                        // if get folder, add to list
                        list.add(file[i]);
                    }
                    fileNames.add(file[i].getName());
                }
            }
        }

        ArrayList<String> fileAndFolderNamesAL = (ArrayList)fileNames;

        return fileAndFolderNamesAL;
    }

    private static String readFile(String path){

        File file = new File(path);
        FileReader reader = null;
        try {
            reader = new FileReader(file);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        BufferedReader bReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String data = "";
        try {
            while (true) {
                if (!((data =bReader.readLine()) != null)) break;
                sb.append(data).append("\n");
            }
            bReader.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        data= sb.toString();
        return data;
    }

    private boolean securityCheck(String clientDir){
        String[] rootPathDirArr = serverHolder.getRootPath().split("/");
        String lastDir = rootPathDirArr[rootPathDirArr.length-1];

	    if(!clientDir.contains(lastDir)){
	        if(clientDir.equals("/null")){
	            return true;
            }else if(!clientDir.substring(1,clientDir.length()).contains("/")){
	            return true;
            }else {
	            return false;
            }
        }else{
	        if(clientDir.indexOf(lastDir)==1){
	            return true;
            }else{
	            String lastDirFlip = new StringBuilder(lastDir).reverse().toString();
                String clientDirFlip = new StringBuilder(clientDir).reverse().toString();
                String clientPathFlip = clientDirFlip.substring(clientDirFlip.indexOf(lastDirFlip),clientDirFlip.length());
                String serverWorkingDirFlip = new StringBuilder(serverHolder.getRootPath()).reverse().toString();
                String serverWorkingSubDirFlip = serverWorkingDirFlip.substring(0,clientPathFlip.length());

                if(clientPathFlip.equals(serverWorkingSubDirFlip)){
                    return true;
                }else{
                    return false;
                }
            }
        }
    }

    //for both GET and POST: setup contentType: what type of file should be get or post
    public void setContentType(String[] requestDataArray){
        for (String line : requestDataArray) {
            String[] lineArray = line.split(" ");
            if (lineArray[0].equalsIgnoreCase("Content-Type:")) {
                if (line.length() > 14) {
                    contentType = lineArray[1].toLowerCase();
                    contentTypeAL = contentTypeStringArrayToArrayList(contentType); //elements are: json, xml, ...
                    break;
                }
            }
        }
        return; //if doesn't find "Content-Type:" in input, contentType is empty string as initialized
    }

    //for GET only??? setup contentDisposition
    //TODO: check if Content-Disposition is for GET only, Lin checks so
    public void setContentDisposition(String[] requestDataArray){
        for (String line : requestDataArray) {
            String[] lineArray = line.split(" ");
            if (lineArray[0].equalsIgnoreCase("Content-Disposition:")) {
                if (line.length() > 21) {
                    contentDisposition = lineArray[1].toLowerCase();
                    break;
                }
            }
        }
        return; //if doesn't find "Content-Disposition:" in input, contentDisposition is empty string as initialized
    }

    //for POST only: setup contentBody: what to be posted
    public void setContentBody(String[] requestDataArray){
        for (String line : requestDataArray) {
            String[] lineArray = line.split(" ");

            if (lineArray[0].equalsIgnoreCase("Content-Body:")) {
                if (line.length() > 14) {
                    this.contentBody = line.substring(14); //start from after the first space, count whatever as content need to post to file
                    break;
                } else {
                    this.contentBody = "";
                    break;
                }
            }
        }
        return; //if doesn't find "Content-Body:" in input, contentBody is empty string as initialized
    }

    public void setContentBodyItself(String cb) {
        contentBody = cb;
    }

    //only for POST
    public boolean setOverwrite(String[] requestDataArray){
        for (String line : requestDataArray) {
            String[] lineArray = line.split(" ");

            //for POST only: setup overwrite value
            if (lineArray[0].equalsIgnoreCase("overwrite:")) {
                String overwriteValueString = "";
                if (!(line.length() >10)) {
                    System.out.println("Error input. There must be a value of overwrite.");
                    return false;
                } else {
                    overwriteValueString = lineArray[1].toLowerCase(); //get "true" or "false"
                }

                if (overwriteValueString.equalsIgnoreCase("true")) {
                    overwrite = true;
                    return true;
                } else if (overwriteValueString.equalsIgnoreCase("false")) {
                    overwrite = false;
                    return true;
                } else {
                    System.out.println("Error input. There value of overwrite must be true or false.");
                    return false;
                }
            }
        }
        return true;
    }

    //among all files, get the files of requested type
    private ArrayList<String> filterFileType(ArrayList<String> fileAndFolderNamesAL, ArrayList<String> contentTypeAL) {
        ArrayList<String> filesOfWantedTypeAL = new ArrayList<String>();
        for (String wantedType : contentTypeAL) {
            for (String fileAndFolderName : fileAndFolderNamesAL) {
                if ( fileAndFolderName.indexOf(".") == -1 ) { //file name doesn't contain ".", skip, check next file
                    continue;
                } else { //file name contains "."
                    int indexOfLastDot = fileAndFolderName.lastIndexOf(".");
                    String extention = fileAndFolderName.substring(indexOfLastDot+1);

                    if (wantedType.equalsIgnoreCase(extention)) { //if file extention is same as wanted type, count it
                        filesOfWantedTypeAL.add(fileAndFolderName);
                    } else { //if file extention is not wanted type, skip, check next file
                        continue;
                    }
                }
            }
        }
        return filesOfWantedTypeAL;
    }


    private boolean fileExists (String path, String fileName){
        File file=new File(path+"/"+fileName);
        return (file.exists()? true:false);
    }

    private boolean folderExists (String path, String folderName){
        File file=new File(path+"/"+folderName);
        return (file.isDirectory()? true:false);
    }

    private boolean fileOrFolderExists (String path, String fileOrFolderName){
        File file=new File(path+"/"+fileOrFolderName);
        if (file.exists() || file.isDirectory()) {
            return true;
        } else {
            return false;
        }
    }





    /**
     * To be used by POST to write to file
     * @param data
     * @param path
     * @param overwrite
     */
    private static void writeToFile(String data, String path, boolean overwrite){
        File outputFile = new File(path);
        FileWriter writer = null;
        try {
            if (!outputFile.exists()) { //if file doesn't exist, create file
                outputFile.createNewFile();
            }
            writer = new FileWriter(outputFile, !overwrite); //true for append, false for overwrite
            writer.append(data);
            writer.flush();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != writer) {
                try {
                    writer.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
