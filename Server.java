import java.io.*;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;

public class Server {
    private int port; //port of Server
    private ServerSocket serverSocket = null;               //Socket of Server
    private Socket clientSocket = null;                     //Socket of client
    private ObjectInputStream in = null;
    private ObjectOutputStream out = null;

    private HashMap<String, String> creds = new HashMap<>();                            //credentials of Clients
    private static List<EnergyStatus> energyClientsInformation = new ArrayList<>();     //Client's Information
    private List<EnergyStatus> waitingClients = new ArrayList<>();                      //Waiting List of Client's
    private List<EnergyStatus> energySuppliers = new ArrayList<>();                     //List with energy Suppliers found by the Server

    private List<Thread> threads = new ArrayList<>();                                   //Threads of each Client

    private List<EnergyStatus> possibleEnergySuppliers = new ArrayList<>();             //Contains the Clients in descending order via the metric (max{b-s,0} * t)

    private EnergyStatus energyStatustemp;                                              //Instance of EnergyStatus that helps identify if a Client has already connected to Server


    /**
     * Constructor of Server
     * Initialize Server's port and credentials of Clients
     * @param port  Server port
     * @param creds Client's credentials
     */
    public Server(int port, HashMap<String, String> creds){
        this.port = port;
        this.creds = creds;
    }

    /**
     * Start the Server
     */
    public void start(){
        System.out.println("Server is Listening\n");
        try{

            /*
             * Server start to listen to port
             */
            serverSocket = new ServerSocket(port);

            /*
             * For every connection
             */
            while(true){
                clientSocket = null;
                try{

                    /*
                     * Accept new Client
                     */
                    clientSocket = serverSocket.accept();

                    /*
                     * Initialize input and output streams
                     */
                    in = new ObjectInputStream(clientSocket.getInputStream());
                    out = new ObjectOutputStream(clientSocket.getOutputStream());
                    System.out.println("\nNew Client Just Connected");

                    /*
                     * Get from Client his credentials, their max and current Energy status and the Request that they want
                     */
                    String clientIP = in.readUTF();
                    String username = in.readUTF();
                    String password = in.readUTF();
                    int maxEnergyPoints = in.readInt();
                    int currentEnergyPoints = in.readInt();
                    int request = in.readInt();

                    System.out.println("\nClientIP: " + clientIP);
                    System.out.println("Client Username: " + username);
                    System.out.println("Client Max Energy Points: " + maxEnergyPoints);
                    System.out.println("Client Current Energy Points: " + currentEnergyPoints);

                    /*
                     * Authenticate the Client
                     */
                    if(authenticateClient(username, password)){
                        System.out.println("Client authenticated");

                        /*
                         * Send to Client if he is valid
                         */
                        out.writeBoolean(authenticateClient(username, password));
                        out.flush();

                        /*
                         * If energyClient has been initialized before found = true
                         * Else found = false
                         */
                        boolean found = false;

                        /*
                         * For every energyClients that has been initialized
                         */
                        for(EnergyStatus energyStatus : energyClientsInformation){

                            /*
                             * If energyClient is already initialized do nothing
                             */
                            if(energyStatus.getUsername().equals(username)){
                                energyStatustemp = energyStatus;
                                found = true;
                            }
                        }

                        /*
                         * If energyClient has been not initialized add him to energyClientsInformation List
                         */
                        if(!found){
                            energyStatustemp = new EnergyStatus(maxEnergyPoints, currentEnergyPoints, 0, clientIP, username);
                            energyClientsInformation.add(energyStatustemp);
                            findEnergySupplierAfterUpdate();
                        }

                        /*
                         * Create new Thread so the Client can stay connected
                         */
                        Thread t = new ClientHandler(clientSocket, in, out, energyStatustemp, this, request);
                        threads.add(t);
                        t.start();
                    }else{
                        System.out.println("Problem with authentication");

                        /*
                         * Send Client 0 so he is not authenticated
                         */
                        out.writeBoolean(authenticateClient(username, password));
                        out.flush();
                    }
                }catch(Exception e){

                    e.printStackTrace();
                }
            }
        } catch (IOException e) {

            e.printStackTrace();
        }
    }

    /**
     * Authenticate Client from username and password
     * @param username of Client
     * @param password of CLient
     * @return
     */
    private boolean authenticateClient(String username, String password){
        return creds.containsKey(username) && creds.get(username).equals(password);
    }

    /**
     * Update Client's Information
     * Synchronized Method so Threads can not access the energyClientsInformation List at the same time
     * @param username  Username of the Client
     * @param newCurrentEnergyPoints    Energy Points at a certain time
     */
    public synchronized void updateEnergyClientInformation(String username, int newCurrentEnergyPoints){

        /*
         * For every connected Client to Server
         */
        for(EnergyStatus energyStatus : energyClientsInformation){

            /*
             * Find the Client and check if his energy Points has changed
             */
            if(energyStatus.getUsername().equals(username) && !(energyStatus.getCurrentEnergyPoints() == newCurrentEnergyPoints)){
                System.out.println("Client updated");

                /*
                 * Change the energy Points
                 */
                energyStatus.setCurrentEnergyPoints(newCurrentEnergyPoints);
            }
        }
    }

    /**
     * Find Energy Suppliers after every update of the EnergyClientsInformation List
     */
    public synchronized void findEnergySupplierAfterUpdate(){

        /*
         * Clear energySuppliers List so as to find the new Energy Suppliers
         */
        energySuppliers.clear();

        /*
         * energyStatusToRemove contains all the Energy Clients that have to be removed from the WaitingList
         */
        List<EnergyStatus> energyStatusToRemove = new ArrayList<>();

        /*
         * If Energy Suppliers has not been found then found = false
         * Else found = true
         */
        boolean found = false;

        /*
         * For every Energy Client who is inside Waiting List
         */
        for(EnergyStatus energyStatus : waitingClients){

            /*
             * Energy Points that have been obtained by taking the i-th first Suppliers
             */
            int energyPointsCounter = 0;

            /*
             * For each possible Energy Supplier
             */
            for(EnergyStatus possibleSupplier : possibleEnergySuppliers){

                /*
                 * If the Supplier is not waiting for Energy
                 * AND the Energy Points that the Client needs have not been collected
                 * AND the Energy Points that have been obtained are less than the Energy Points that the Client needs
                 * AND the Supplier is not the same as the Client
                 */
                if(!waitingClients.contains(possibleSupplier) && !found && energyPointsCounter < energyStatus.getNeedEnergyPoints() && !possibleSupplier.getUsername().equals(energyStatus.getUsername())){

                    /*
                     * Increase the Energy Points that have been obtained by the Energy Points of the current Supplier
                     */
                    energyPointsCounter += possibleSupplier.getCurrentEnergyPoints();

                    /*
                     * Add Supplier inside the energySuppliers List
                     */
                    energySuppliers.add(possibleSupplier);

                    /*
                     * If the Energy Points have been obtained
                     */
                    if(energyPointsCounter >= energyStatus.getNeedEnergyPoints()){

                        /*
                         * Suppliers have been found
                         */
                        found = true;
                        System.out.println("Server FOUND " + energySuppliers.size() + " Suppliers");
                        break;
                    }
                }
            }

            /*
             * If Server found Energy Suppliers for the Client
             */
            if(found){

                /*
                 * For each Supplier
                 */
                for(EnergyStatus energySupplier : energySuppliers){

                    /*
                     *  The amount of Energy Points that each Supplier will have after the transaction
                     */
                    int newEnergyPoints = energyPointsCounter - energySupplier.getCurrentEnergyPoints();

                    /*
                     * Reduce the Energy Points that have been obtained by the Energy Points of each Supplier
                     */
                    energyPointsCounter -= energySupplier.getCurrentEnergyPoints();

                    /*
                     * If the Energy Points that remains are positive means that the Energy Supplier has given all of his Energy
                     */
                    if(newEnergyPoints >= 0){

                        /*
                         * Energy Points that the Supplier will have is 0
                         */
                        newEnergyPoints = 0;
                    }else{

                        /*
                         * Energy Points that the Supplier will have is the absolute of the Energy Points that have been remained
                         */
                        newEnergyPoints = Math.abs(newEnergyPoints);
                    }

                    /*
                     * Update the Energy Points of the Suppliers
                     */
                    updateEnergyClientInformation(energySupplier.getUsername(), newEnergyPoints);
                }

                /*
                 * Update the Energy Points of the Client
                 */
                updateEnergyClientInformation(energyStatus.getUsername(), energyStatus.getNeedEnergyPoints() + energyStatus.getCurrentEnergyPoints());
            }

            /*
             * If the Server could not found Energy Supplier
             */
            if(!found){

                /*
                 * Reduce Waiting Time
                 */
                energyStatus.setWaitingTime(energyStatus.getWaitingTime()-1);

                /*
                 * If the Waiting Time is 0 add Client to energyStatusToRemove
                 */
                if(energyStatus.getWaitingTime() == 0){
                    System.out.println("Clients to remove");
                    energyStatusToRemove.add(energyStatus);
                }
            }
        }

        /*
         * If Client's Waiting Time is 0 or Server have found Energy Suppliers for this Client
         * Remove Client from Waiting List
         */
        for(EnergyStatus energyStatus : energyStatusToRemove){
            System.out.println("Remove " + energyStatus.getUsername() + " from Waiting List");
            waitingClients.remove(energyStatus);
        }

        /*
         * For every Energy Client increase t value
         */
        for(EnergyStatus energyStatus : energyClientsInformation){
            energyStatus.setT(energyStatus.getT() + 1);
        }

        /*
         * For every Supplier the t value must be 0
         */
        for(EnergyStatus energyStatus : energySuppliers){
            energyStatus.setT(0);
        }
    }


    /**
     * Find Energy Supplier based on the choice that the Client gave
     * Synchronized Method so Threads can not access the energyClientsInformation List at the same time
     * @param username  Client's username
     * @param needEnergyPoints  Energy Points that the Client wants
     * @param choice    Choice that the Client gave
     */
    public synchronized void findEnergySupplier(String username, int needEnergyPoints, int choice){

        /*
         * Clear energySuppliers List so as to find the new Energy Suppliers
         */
        energySuppliers.clear();

        /*
         * found  = true if Server found Energy Suppliers
         * found = false if Server has not found Energy Suppliers
         */
        boolean found = false;

        /*
         * For each Energy Client
         */
        for(EnergyStatus energyStatus: energyClientsInformation){

            /*
             * Check if there is one Energy Supplier who can give Energy
             */
            if(energyStatus.getCurrentEnergyPoints() >= needEnergyPoints && !waitingClients.contains(energyStatus) && !energyStatus.getUsername().equals(username)){
                System.out.println("Server found one energyClient: " + energyStatus.getUsername() + " ____ : " + username);

                /*
                 * Server found Energy Supplier
                 */
                found = true;

                /*
                 * Add Energy Supplier to the energySuppliers List
                 */
                energySuppliers.add(energyStatus);

                /*
                 * Update Supplier Info with the new Energy Points
                 */
                updateEnergyClientInformation(energyStatus.getUsername(), energyStatus.getCurrentEnergyPoints() - needEnergyPoints);

                /*
                 * Update Info of Client who wanted Energy Points
                 */
                updateEnergyClientInformation(username, needEnergyPoints);
            }
        }

        /*
         * If an Energy Supplier has been found then set the Energy Points that the Client needs to 0
         */
        if(found){
            for(EnergyStatus energyStatus : energyClientsInformation){
                if(energyStatus.getUsername().equals(username)){
                    energyStatus.setNeedEnergyPoints(0);
                }
            }
        }

        /*
         * There is no energy user with more energy than the requested.
         */
        if(choice == 1){
            //Find two or more clients to send the energy

            /*
             * Calculate the metric for every Client
             */
            for(EnergyStatus energyStatus : energyClientsInformation){

                /*
                 * Initialize the probability of each Client to be a Supplier
                 */
                energyStatus.setSupplierProbability(Math.max(energyStatus.getGivenEnergyPoints() - energyStatus.getReceivedEnergyPoints(), 0) * energyStatus.getT());

                /*
                 * Add the Client to possibleEnergySuppliers List
                 */
                possibleEnergySuppliers.add(energyStatus);
            }

            /*
             * After the possibleEnergySuppliers List has been created and initialized
             * Sort the List by the probability
             */
            Collections.sort(possibleEnergySuppliers, new Comparator<EnergyStatus>() {
                @Override
                public int compare(EnergyStatus o1, EnergyStatus o2) {
                    return (int) (o1.getSupplierProbability() - o2.getSupplierProbability());
                }
            });

            /*
             * Reverse the List so it is in descending order
             */
            Collections.reverse(possibleEnergySuppliers);

            /*
             * Energy Points that have been obtained by taking the i-th first Suppliers
             */
            int energyPointsCounter = 0;

            /*
             * For each possible Energy Supplier
             */
            for(EnergyStatus energyStatus : possibleEnergySuppliers){

                /*
                 * If the Supplier is not waiting for Energy
                 * AND the Energy Points that the Client needs have not been collected
                 * AND the Energy Points that have been obtained are less than the Energy Points that the Client needs
                 * AND the Supplier is not the same as the Client
                 */
                if(!waitingClients.contains(energyStatus) && !found && energyPointsCounter < needEnergyPoints && !energyStatus.getUsername().equals(username)){

                    /*
                     * Increase the Energy Points that have been obtained by the Energy Points of the current Supplier
                     */
                    energyPointsCounter += energyStatus.getCurrentEnergyPoints();

                    /*
                     * Add Supplier inside the energySuppliers List
                     */
                    energySuppliers.add(energyStatus);

                    /*
                     * If the Energy Points have been obtained
                     */
                    if(energyPointsCounter >= needEnergyPoints){

                        /*
                         * Suppliers have been found
                         */
                        found = true;
                        System.out.println("Server FOUND " + energySuppliers.size() + " Suppliers");
                        break;
                    }
                }
            }

            /*
             * If Server found Energy Suppliers for the Client
             */
            if(found){

                /*
                 * For each Supplier
                 */
                for(EnergyStatus energySupplier : energySuppliers){

                    /*
                     * Reduce the Energy Points that have been obtained by the Energy Points of each Supplier
                     */
                    int newEnergyPoints = energyPointsCounter - energySupplier.getCurrentEnergyPoints();

                    /*
                     * Reduce the Energy Points that have been obtained by the Energy Points of each Supplier
                     */
                    energyPointsCounter -= energySupplier.getCurrentEnergyPoints();

                    /*
                     * If the Energy Points that remains are positive means that the Energy Supplier has given all of his Energy
                     */
                    if(newEnergyPoints >= 0){

                        /*
                         * Energy Points that the Supplier will have is 0
                         */
                        newEnergyPoints = 0;
                    }else{

                        /*
                         * Energy Points that the Supplier will have is the absolute of the Energy Points that have been remained
                         */
                        newEnergyPoints = Math.abs(newEnergyPoints);
                    }

                    /*
                     * Update the Energy Points of the Suppliers
                     */
                    updateEnergyClientInformation(energySupplier.getUsername(), newEnergyPoints);
                }

                for(EnergyStatus energyStatus : energyClientsInformation){
                    if(username.equals(energyStatus.getUsername())){

                        /*
                         * Update the Energy Points of the Client
                         */
                        updateEnergyClientInformation(username, energyStatus.getNeedEnergyPoints() + energyStatus.getCurrentEnergyPoints());
                    }
                }
            }

            if(!found){
                System.out.println("Server could not found Energy Suppliers");
            }

        }else if(choice == 2){
            //Put Client into a WaitingList for a specific WaitingTime
            found = true;
            int waitingTime = 0;
            try {

                /*
                 * Get Waiting Time from the Client
                 */
                 waitingTime = in.readInt();
                 System.out.println(username+" Waiting Time: " + waitingTime);
            } catch (IOException e) {
                e.printStackTrace();
            }

            /*
             * For each Client
             */
            for(EnergyStatus energyStatus : energyClientsInformation){

                /*
                 * If the Client is the one who wanted the Energy Points
                 */
                if(energyStatus.getUsername().equals(username)){

                    /*
                     * Set his choice
                     */
                    energyStatus.setChoice(choice);

                    /*
                     * Set his waiting time
                     */
                    energyStatus.setWaitingTime(waitingTime);

                    /*
                     * Put him in the Waiting List
                     */
                    boolean foundinWaitingList = false;
                    for(EnergyStatus energyStatus1 : waitingClients){
                        if(energyStatus1.getUsername().equals(energyStatus.getUsername())){
                            foundinWaitingList = true;
                        }
                    }
                    if(foundinWaitingList){
                        System.out.println("Server updated " + username + " in WaitingList");
                    }else{
                        System.out.println("Server put " + username + " in WaitingList");
                        waitingClients.add(energyStatus);
                    }

                    Collections.sort(waitingClients, new Comparator<EnergyStatus>() {
                        @Override
                        public int compare(EnergyStatus o1, EnergyStatus o2) {
                            return o1.getWaitingTime() - o2.getWaitingTime();
                        }
                    });

                }
            }
        }

        /*
         * Inform Client that the Server cannot find any Energy Suppliers
         */
        if(!found){
            System.out.println("Server cannot find Energy Supplier");
        }
    }

    /**
     * Return the Energy Suppliers for a Client
     * @return energySuppliers List
     */
    public synchronized List<EnergyStatus> getEnergySuppliers() {
        return energySuppliers;
    }
}
