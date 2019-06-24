
public class MainClient {

    private static String serverIP = "192.168.1.37";    //IP of the Server
    private static int serverPort = 8080;               //Port of the Server
    private static String username = "Client3";         //Client's Username
    private static String password = "Client3";         //Client's Password
    private static int maxEnergyPoints = 10;            //max Energy Points that the Client can have
    private static int currentEnergyPoints = 5;         //current Energy Points that the Client has
    private static int needEnergyPoints = 4;            //Energy Points that the Client needs
    private static int waitingTime = 1;                 //Waiting time of the Client at Waiting List

    /*
     * Request:
     * 1 : Inform Server about energy status
     * 2 : Ask Server for energy points
     */
    private static int request = 2;

    /*
     * Choice
     * 1 : Client want to be supplied by two other Clients
     * 2 : Client want to be putted in WaitingList for a specific WaitingTime
     */
    private static int choice = 2;


    public static void main(String[] args){

        /*
         * Create a new Client instance
         */
        Client client = new Client(serverIP, serverPort, username, password, request, choice, waitingTime);

        /*
         * Initialize Client's Energy Status
         */
        client.initEnergy(maxEnergyPoints, currentEnergyPoints, needEnergyPoints);

        /*
         * Start the connection between the Client and the Server
         */
        client.connect();

        /*
         * The Client listen if the Server want him to be a Supplier
         */
        client.listenFromServer(5055);
    }
}
