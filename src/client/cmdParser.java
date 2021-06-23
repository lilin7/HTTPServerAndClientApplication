package client;

//import server.ServerHolder;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Arrays;

/**
 * Parse input to set the status of holder
 */
public class cmdParser {

    private ArrayList<String> parameters;
    private Holder holder;
    private Help help = new Help();

   //ServerHolder serverHolder = new server.ServerHolder();
    
    /**
     * Constructor
     * @param args, input array
     * @param holder
     */
    public cmdParser(String[] args, Holder holder){
        this.parameters = new ArrayList<String>();
        this.parameters.addAll(Arrays.asList(args));
        this.holder = holder;
    }

    /**
     * Analyse input to know how to organize response to user
     * @return boolean, if parse succeed or not
     * @throws Exception
     */
    public boolean parameterAnalyse() throws Exception {
    	if (parameters.size()<2) {
    		System.out.println("Error input. Parameter size should be at least 2.");
    		return false;
    	}
    
    	switch (parameters.get(0).toLowerCase()) {
    		case "get":
    			holder.setFunction("get");
    			String file = parameters.get(1);
    			if (file.equals("/")) {
					holder.setFileName("");
    			} else {
    				if (file.indexOf("/")!=0) {
    					if (file.length()>1 && file.substring(0,2).equals("..")){ //e.g. GET ../.., only this situation can start with not /
							holder.setFileName(file);
						} else if (file.equalsIgnoreCase("cd")) {
							//do nothing, below there is part of if (parameters.contains("cd"))
						} else if (file.length()>2 && file.substring(1,3).equals(":/")) { // c:/...
							holder.setFileName(file);
						}
    					else {
							System.out.println("Error input.");
							return false;
						}
    				} else {
    					holder.setFileName(file.substring(1));
    				}
    			}

				if (parameters.contains("-o")) {
					int indexOfO = parameters.indexOf("-o");
					if (indexOfO==parameters.size()-1) {
						System.out.println("Error input. 'Content-Type:' must be followed by the content type");
						return false;
					}
					holder.hasO = true;
					holder.fileNameForO = parameters.get(indexOfO+1);
				}
    			
    			if (parameters.contains("Content-Type:")) {
    				int indexOfContentType = parameters.indexOf("Content-Type:");
    				if (indexOfContentType==parameters.size()-1) {
    					System.out.println("Error input. 'Content-Type:' must be followed by the content type");
    					return false;
    				}

					String[] contentTypeArr = parameters.get(indexOfContentType+1).trim().toLowerCase().split("/");
					ArrayList<String> contentTypeAL = new ArrayList<String>(Arrays.asList(contentTypeArr));

    				String fullFileName = holder.getFileName();
    				if (fullFileName.lastIndexOf(".")!= -1) { //has "." in file name
    					int indexOfLastDot = fullFileName.lastIndexOf(".");
    					String extention = fullFileName.substring(indexOfLastDot+1).trim().toLowerCase();

    					if (!contentTypeAL.contains(extention)){
							System.out.println("Error input. File extention and Content-Type are contradiction.");
							return false;
						}
					}

    				holder.setHasContentType(true);
    				holder.setContentType(parameters.get(indexOfContentType+1));
    			}
    			
    			if (parameters.contains("Content-Disposition:")) {
    				int indexOfContentDisposition = parameters.indexOf("Content-Disposition:");
    				if (indexOfContentDisposition==parameters.size()-1) {
    					System.out.println("Error input. 'Content-Disposition:' must be followed by the content disposition");
    					return false;
    				}
    				holder.setHasContentDisposition(true);
    				holder.setContentDisposition(parameters.get(indexOfContentDisposition+1));
    			}

				if (parameters.contains("cd")) {
					holder.hascd = true;
					int indexOfcd = parameters.indexOf("cd");
					if (indexOfcd==parameters.size()-1) {
						System.out.println("Error input.");
						return false;
					}
					String directoryChangeTo = parameters.get(indexOfcd+1);
					String cdAndDirectoryChangeTo = "cd "+directoryChangeTo;
					holder.setFileName(cdAndDirectoryChangeTo);
				}
    					
    			break;
    		case "post":
    			holder.setFunction("post");

    			String postfile = parameters.get(1); //like "/" or "/foo"
    			if (postfile.equals("/")) {
    				System.out.println("Error input. Please indicate you want to post to which file.");
					return false;
    			} else {   				
    				if (postfile.indexOf("/")!=0) {
						if (postfile.length()>1 && postfile.substring(0,2).equals("..")){ //e.g. GET ../.., only this situation can start with not /
							holder.setFileName(postfile);
						} else if (postfile.equalsIgnoreCase("cd")) {
							//do nothing, below there is part of if (parameters.contains("cd"))
						} else if (postfile.length()>2 && postfile.substring(1,3).equals(":/")) { // c:/...
							holder.setFileName(postfile);
						} else {
							System.out.println("Error input.");
							return false;
						}
    				} else {
    					holder.setFileName(postfile.substring(1)); //filename = "foo"
    				}
    			}

				if (parameters.contains("Content-Type:")) {
					int indexOfContentType = parameters.indexOf("Content-Type:");
					if (indexOfContentType==parameters.size()-1) {
						System.out.println("Error input. 'Content-Type:' must be followed by the content type");
						return false;
					}

					String[] contentTypeArr = parameters.get(indexOfContentType+1).trim().toLowerCase().split("/");
					ArrayList<String> contentTypeAL = new ArrayList<String>(Arrays.asList(contentTypeArr));

					String fullFileName = holder.getFileName();

					/* if want to be able to post bar.json.txt, comment this off
					if (fullFileName.lastIndexOf(".")!= -1) { //has "." in file name
						int indexOfLastDot = fullFileName.lastIndexOf(".");
						String extention = fullFileName.substring(indexOfLastDot+1).trim().toLowerCase();


						if (!contentTypeAL.contains(extention)){
							System.out.println("Error input. File extention and Content-Type are contradiction.");
							return false;
						}
					}*/

					holder.setHasContentType(true);
					holder.setContentType(parameters.get(indexOfContentType+1));
				}

				if (parameters.contains("Content-Body:")) {
					int indexOfContentBody = parameters.indexOf("Content-Body:");
					if (indexOfContentBody==parameters.size()-1) {
						holder.setPostContentBody("");
					} else {
						//-----------------------------------TODO: if there is space in Content-Body, but is the last parameter-----
						StringBuffer sb = new StringBuffer();
						for (int i = indexOfContentBody+1; i<parameters.size(); i++){
							sb.append(parameters.get(i));
							sb.append(" ");
						}
						String body = sb.toString().trim();
						holder.setPostContentBody(body);
						//-------------------------
						//----------otherwise use below code---------
						//holder.setPostContentBody(parameters.get(indexOfContentBody+1));
					}
				}

    			//----------begin of parsing overwrite---------------------
    			String overwriteBooleanString = "";
    			for (String s : parameters){
    				if (s.length()>8) {
    					String first8Substring = s.substring(0, 9);
    					if (first8Substring.equalsIgnoreCase("overwrite")) { //find overwrite string
							if (s.length()==14) { //case "overwrite=true"
								overwriteBooleanString = s.substring(s.length()-4);
								if (!overwriteBooleanString.equalsIgnoreCase("true")) {
									System.out.println("Error input. Overwrite value should be true or false");
									return false;
								} else {
									holder.setOverwrite(true);
								}
							} else if (s.length()==15) {//case "overwrite=false"
								overwriteBooleanString = s.substring(s.length()-5);
								if (!overwriteBooleanString.equalsIgnoreCase("false")) {
									System.out.println("Error input. Overwrite value should be true or false");
									return false;
								} else {
									holder.setOverwrite(false);
								}
							} else { //other case for overwrite
								System.out.println("Error input. Overwrite value should be true or false, write in the format of 'overwrite=true' or 'overwrite=false'");
								return false;
							}
						}
					}
				}
				//----------end of parsing overwrite---------------------

				if (parameters.contains("cd")) {
					holder.hascd = true;
					int indexOfcd = parameters.indexOf("cd");
					if (indexOfcd==parameters.size()-1) {
						System.out.println("Error input.");
						return false;
					}
					String directoryChangeTo = parameters.get(indexOfcd+1);
					String cdAndDirectoryChangeTo = "cd "+directoryChangeTo;
					holder.setFileName(cdAndDirectoryChangeTo);
				}

    			break;
    		default:
    			System.out.println("Error input. First word must be GET or POST.");
    			return false;
    	}

    	//TODO: PORT NUMBER!!! if automatically connected to server, un-comment below 2 lines
        //holder.setHost(serverHolder.getLocalhostAddress());
        //holder.setPort(serverHolder.getPortNumber());
		holder.setHost("127.0.0.6");
		holder.setPort(8080);
  
    	holder.setURL(holder.getHost() +":"+ holder.getPort());
    	System.out.println("Host address is: " + holder.getHost());
    	System.out.println("Port is: " + holder.getPort());
    	
        return true;
    }


    /**
     * for -f option in get, read input from file
     * @param path
     * @return String
     * @throws Exception
     */
    private String readFile(String path)throws Exception{
        File file = new File(path);
        FileReader reader = new FileReader(file);
        BufferedReader bReader = new BufferedReader(reader);
        StringBuilder sb = new StringBuilder();
        String data = "";
        while ((data =bReader.readLine()) != null) {
            sb.append(data);
        }
        bReader.close();
        data= sb.toString();
        return data;
    } 
}
