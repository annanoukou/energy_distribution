import java.util.HashMap;

public class MainServer {

    private static int port = 8080;                                         //listening port of Server
    private static HashMap<String, String> credentials = new HashMap<>();   //credentials of Clients

    /**
     * Main Method
     */
    public static void main(String[] args){

        /*
         * Initialize Client's credentials
         */
        initCredentials();

        /*
         * Create new instance of Server
         */
        Server server = new Server(port, credentials);

        /*
         * Start the Server
         */
        server.start();
    }

    /**
     * Initialize Credentials of Clients
     */
    public static void initCredentials(){
        credentials.put("Client1", "Client1");
        credentials.put("Client2", "Client2");
        credentials.put("Client3", "Client3");
    }

}
