package client;

/**
 * Help class, to be called when user input help requests
 */
public class Help {
    public void printHttpcHelp(){
        System.out.println("httpc is a curl-like application but supports HTTP protocol only");
        System.out.println("Usage:");
        System.out.println("    httpc command [arguments]");
        System.out.println("The commands are:");
        System.out.println("    get   executes a HTTP GET request and prints the response.");
        System.out.println("    post  executes a HTTP POST request and prints the response.");
        System.out.println("    help  prints this screen.");
        System.out.println("Use \"httpc help [command]\" for more information about a command.");
    }

    public void printGetHelp(){
        System.out.println("usage: httpc get [-v] [-h key:value] URL");
        System.out.println("Get executes a HTTP GET request for a given URL.");
        System.out.println("    -v            Prints the detail of the response such as protocol, status, and headers.");
        System.out.println("    -h key:value  Associates headers to HTTP Request with the format 'key:value'.");
    }

    public void printPostHelp(){
        System.out.println("usage: httpc post [-v] [-h key:value] [-d inline-data] [-f file] URL");
        System.out.println("Post executes a HTTP POST request for a given URL with inline data or from file.");
        System.out.println("    -v            Prints the detail of the response such as protocol, status, and headers.");
        System.out.println("    -h key:value  Associates headers to HTTP Request with the format 'key:value'.");
        System.out.println("    -d string     Associates an inline data to the body HTTP POST request.");
        System.out.println("    -f file       Associates the content of a file to the body HTTP POST request.");
        System.out.println("Either [-d] or [-f] can be used but not both.");
    }

    public void errorInput(){
        System.out.println("Invalid input.");
    }

}
