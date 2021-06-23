package server;

import java.util.ArrayList;
import java.util.Arrays;

/**
 * to parse input from server like "httpfs -v -p ..."
 *
 */
public class Parser {

    private ArrayList<String> rawParameter;
    private ServerHolder serverHolder;

    public Parser(String[] rawParameter, ServerHolder serverHolder){
        this.rawParameter = new ArrayList<String>();
        this.rawParameter.addAll(Arrays.asList(rawParameter));
        this.serverHolder = serverHolder;
    }

    public boolean parameterAnalyse()throws Exception{

        if(!rawParameter.get(0).equals("httpfs")){
            System.out.println("wrong input, the first parameter must be httpfs");
            return false;
        }else{
            if(rawParameter.contains("-v")){
                serverHolder.setV(true);
            }
            if(rawParameter.contains("-p")){
            	//validate "-p" can't be the last parameter
            	if (rawParameter.indexOf("-p") == rawParameter.size()-1) {
            		System.out.println("-p can't be the last parameter of your input");
            		return false;
            	}

                try{
                    int port = Integer.parseInt(rawParameter.get(rawParameter.indexOf("-p")+1));
                    serverHolder.setPortNumber(port);
                }catch (NumberFormatException e){
                    System.out.println("-p must be followed by an integer");
                    return false;
                }
            }
            if(rawParameter.contains("-d")){

                if (rawParameter.indexOf("-d") == rawParameter.size()-1) {
                    System.out.println("-d can't be the last parameter of your input");
                    return false;
                }
                String newRootPath = rawParameter.get(rawParameter.indexOf("-d")+1);
                serverHolder.setRootPath(newRootPath);
            }

        }

        if(serverHolder.getV()==true){
            Debugging debugging = new Debugging(serverHolder);
            debugging.printDebugInfo();
        }
        return true;
    }
}
