
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.*;
import java.text.SimpleDateFormat;
import java.util.*;

public class Client {
    private String serverIP;    //IP of the Server
    private int serverPort;     //Port of the Server
    private String username;    //Client's Username
    private String password;    //Client's Password


    private int maxEnergyPoints;        //max Energy Points that the Client can have
    private int currentEnergyPoints;    //current Energy Points that the Client has
    private int needEnergyPoints;       //Energy Points that the Client needs
    private int waitingTime;            //Waiting time of the Client at Waiting List

    private int requestedEnergy;        //Energy Points requested from Server


    private int UDPport = 4004;                 //Client waits at this port UDP packets from Suppliers
    private DatagramSocket dtsocket = null;     //Socket for UDP packet which Client will receive

    HashMap<String, Integer> SuppliedEnergy = new HashMap<String, Integer>();   //HashMap with all the connected to server Clients who have sent to this Client Energy


    private List<EnergyStatus> energySuppliers = new ArrayList<>(); //List with energy Suppliers found by the Server

    /*
     * Request:
     * 1 : Inform Server about energy status
     * 2 : Ask Server for energy points
     */
    private int request;

    /*
     * Choice
     * 1 : Client want to be supplied by two other Clients
     * 2 : Client want to be putted in WaitingList for a specific WaitingTime
     */
    private int choice;

    private ObjectOutputStream out = null;  //Stream for sending data
    private ObjectInputStream in = null;    //Stream for taking data

    private Socket socket = null;           //Socket for connection between Client and Server

    /**
     * Constructor of Client
     * @param serverIP IP of the Server
     * @param serverPort Port of the Server
     * @param username Client's Username
     * @param password Client's Password
     * @param request Client's Request
     * @param choice Client's Choice
     * @param waitingTime Waiting time of the Client at Waiting List
     */
    public Client(String serverIP, int serverPort, String username, String password, int request, int choice, int waitingTime){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        this.username = username;
        this.password = password;
        this.request = request;
        this.choice = choice;
        this.waitingTime = waitingTime;
    }

    /**
     * Initialize the Energy Status of the Client
     * @param maxEnergyPoints max Energy Points that the Client can have
     * @param currentEnergyPoints current Energy Points that the Client has
     * @param needEnergyPoints Energy Points that the Client needs
     */
    public void initEnergy(int maxEnergyPoints, int currentEnergyPoints, int needEnergyPoints){
        this.maxEnergyPoints = maxEnergyPoints;
        this.currentEnergyPoints = currentEnergyPoints;
        this.needEnergyPoints = needEnergyPoints;
        requestedEnergy = needEnergyPoints;
        if(request == 2){
            changeEnergyPointsPeriodically();
        }
    }

    /**
     * Client Connect to Server
     */
    public void connect(){
        try {

            /*
             * If there is a connection between Client and Server
             */
        	if(in!=null){

        	    /*
        	     * Close the connection
        	     */
        		in.close();
        		out.close();
        		socket.close();
        	}

            /*
             * Create Connection between Client and Server
             */
            socket = new Socket(serverIP, serverPort);
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            /*
             * Sent local IP, username, password, maxEnergyPoints, currentEnergyPoints and request to Server
             */
            System.out.println("Server IP: " + serverIP);
            System.out.println("IP: " + String.valueOf(Inet4Address.getLocalHost().getHostAddress()));
            out.writeUTF(String.valueOf(Inet4Address.getLocalHost().getHostAddress()));
            out.flush();
            out.writeUTF(username);
            out.flush();
            out.writeUTF(password);
            out.flush();
            out.writeInt(maxEnergyPoints);
            out.flush();
            out.writeInt(currentEnergyPoints);
            out.flush();
            out.writeInt(request);
            out.flush();

            System.out.println("Data sent to Server");

            /*
             * If the Client is valid
             */
            if(in.readBoolean()){
                System.out.println("Connected successfully");
                if(request == 1){
                    //Inform Server for energy status

                    /*
                     * Send currentEnergyPoints to Server
                     */
                    out.writeInt(currentEnergyPoints);
                    out.flush();

                    /*
                     * Send HashMap with all the Clients and the amount of Energy Points taken from each
                     */
                    out.writeObject(SuppliedEnergy);
                    out.flush();

                }else if(request == 2){
                    //Ask Server for Energy points

                    /*
                     * Send needEnergyPoints, choice and waitingTime to Server
                     */
                    out.writeInt(needEnergyPoints);
                    out.flush();
                    out.writeInt(choice);
                    out.flush();
                    out.writeInt(waitingTime);
                    out.flush();

                    /*
                     * Get size of energySuppliers List
                     */
                    int suppliersCount = in.readInt();

                    /*
                     * For each Supplier
                     */
                    for(int i=0; i<suppliersCount; i++){

                        /*
                         * Get username, ip, maxEnergyPoints, currentEnergyPoints and needEnergyPoints of the Supplier from the Server
                         */
                        String username = in.readUTF();
                        String ip = in.readUTF();
                        int maxEnergyPoints = in.readInt();
                        int currentEnergyPoints = in.readInt();
                        int needEnergyPoints = in.readInt();

                        /*
                         * Create an Energy Supplier as a new instance of EnergyStatus
                         */
                        EnergyStatus energySupplier = new EnergyStatus(maxEnergyPoints, currentEnergyPoints, needEnergyPoints, ip, username);

                        /*
                         * Add the Supplier to the energySuppliers List of the Client
                         */
                        energySuppliers.add(energySupplier);
                    }

                    /*
                     * Create a Random instance
                     */
                    Random random = new Random();

                    /*
                     * The random period in which the Client will resent the request to server is [5,15] seconds
                     */
                    int low = 5;
                    int high = 15;
                    int value = random.nextInt(high-low) + low;
                    int sec = Math.abs(value) * 1000;

                    /*
                     * If the Server could not find Energy Suppliers for the Client
                     */
                    if(energySuppliers.size() == 0){

                        /*
                         * Create a schedule that will resent the request to server
                         */
                        new Timer().schedule(new TimerTask() {
                            public void run()  {
                                System.out.println("\nResent the request to Server\n");
                                connect();
                            }
                        }, sec);
                    }

                    /*
                     * Server has found Energy Suppliers for this Client
                     */
                    if(energySuppliers.size() > 0){

                        /*
                         * Index to find Energy Supplier randomly
                         */
                    	int supplier = -1;

                    	/*
                    	 * Find randomly the supplier index and check if it's valid
                    	 */
                    	while(supplier < 0 || supplier >= energySuppliers.size()){
                    		Random random1 = new Random();
                            supplier = random1.nextInt(energySuppliers.size());
                    	}

                    	/*
                    	 * Check if the Energy Supplier has the amount of Energy Points that the Client needs
                    	 */
                        if(energySuppliers.get(supplier).getCurrentEnergyPoints() >= needEnergyPoints){

                            /*
                             * Connect to Energy Supplier
                             */
                            Socket socketToOtherClient = new Socket(energySuppliers.get(supplier).getIp(), 5055);

                            /*
                             * Output Stream for sending data to Energy Supplier
                             */
                            ObjectOutputStream outputStream = new ObjectOutputStream(socketToOtherClient.getOutputStream());

                            /*
                             * Send to Energy Supplier the IP and Energy Points that this Client needs
                             */
                            outputStream.writeUTF(String.valueOf(Inet4Address.getLocalHost().getHostAddress()));
                            outputStream.flush();
                            outputStream.writeInt(needEnergyPoints);
                            outputStream.flush();

                            /*
                             * If the Supplier has sent to this Client Energy Points before
                             */
                            if(SuppliedEnergy.containsKey(energySuppliers.get(supplier).getUsername())){

                                /*
                                 * The Energy Points that Supplier has given to this Client
                                 */
                                int oldvalue = SuppliedEnergy.get(energySuppliers.get(supplier).getUsername());

                                /*
                                 * Update the Energy Points that Supplier has given to this Client
                                 */
                                SuppliedEnergy.replace(energySuppliers.get(supplier).getUsername(), oldvalue + needEnergyPoints);
                            }else{

                                /*
                                 * If the Supplier has not sent to this Client Energy Points before
                                 * Add the Supplier to HashMap
                                 * Set the Energy Points that he sent to Client
                                 */
                                SuppliedEnergy.put(energySuppliers.get(supplier).getUsername(), needEnergyPoints);
                            }

                            /*
                             * Close the Connection between Energy Supplier and Client
                             */
                            outputStream.close();
                            socketToOtherClient.close();
                        }

                        /*
                         * Set request = 1 to inform the Server about the changes of the Client's Energy Points and the changes to HashMap
                         */
                        request = 1;

                        /*
                         * Reconnect to Server
                         */
                        connect();
                    }

                    /*
                     * Listen for UDP packets from Suppliers
                     */
                    listen();
                }else{

                    /*
                     * Close the connection between Client and Server
                     */
                    in.close();
                    out.close();
                    socket.close();
                    System.out.println("Wrong Request");
                }
            }else{
                System.out.println("Problem with Authentication");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }


    /**
     * Listen for UDP packets from Suppliers
     */
    public void listen(){
        System.out.println("Waiting for UDP Packet");

        /*
         * Initialize the data of the incoming packet
         */
        byte[] rec_data = new byte[512];

        /*
         * Initialize a new UDP packet
         */
        DatagramPacket rec_packet = new DatagramPacket(rec_data, rec_data.length);

        /*
         * Get the UDP packet
         */
        try {
        	dtsocket = new DatagramSocket(UDPport);
        	UDPport++;
            dtsocket.receive(rec_packet);
            System.out.println("UDP Packet has been Received: " + new String(rec_data));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Client waits to listen from the Server if he has been declared as Supplier
     * @param port Port that the Client listen
     */
    public void listenFromServer(int port) {
        System.out.println("Waiting Response from Server");
        try {

            /*
             * Waits to listen from Server
             */
            ServerSocket serverSocket = new ServerSocket(port);
            while (true) {
                Socket clientSocket = null;
                try {

                    /*
                     * Accept connection from Server
                     */
                    clientSocket = serverSocket.accept();
                    ObjectInputStream in = new ObjectInputStream(clientSocket.getInputStream());
                    ObjectOutputStream out = new ObjectOutputStream(clientSocket.getOutputStream());

                    /*
                     * Get Information of the Client who wants Energy Points
                     */
                    String inNeedClientIP = in.readUTF();
                    int needEnergyPoints = in.readInt();

                    /*
                     * Send UDP packet to Client with the specified Energy Points
                     */
                    sendUDP(inNeedClientIP, needEnergyPoints);

                    /*
                     * Close the connection between Server and Client
                     */
                    in.close();
                    out.close();
                    clientSocket.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                /*
                 * Set request = 1 to inform the Server about the changes of the Client's Energy Points
                 */
                request=1;

                /*
                 * Reconnect to Server
                 */
                connect();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Sends UDP packet to a specific Client with the Energy Points that he needs
     * @param inNeedClientIP IP of the Client who needs Energy Points
     * @param needEnergyPoints  Amount of Energy Points that the Client needs
     */
    public void sendUDP(String inNeedClientIP, int needEnergyPoints){
        System.out.println("Sending UDP Packet to Client");

        /*
         * Send UDP packet with the amount of energyPoints needed
         */
        InetAddress host = null;
        try {

            /*
             * Find Address of Client
             */
            host = InetAddress.getByName(inNeedClientIP);

            /*
             * Change needEnergyPoints type to String
             */
            String s = String.valueOf(needEnergyPoints);

            /*
             * Table of bytes that contains the value of needEnergyPoints
             */
            byte[] b = s.getBytes();

            /*
             * Create a UDP packet with the above table of bytes
             */
            DatagramPacket dp = new DatagramPacket(b, b.length, host, UDPport);

            /*
             * Create connection between Supplier and Client in need
             */
            DatagramSocket socket = new DatagramSocket();

            /*
             * Send UDP packet to the Client
             */
            socket.send(dp);
        } catch (IOException e) {
            try {

                /*
                 * Close the connection between Supplier and Client
                 */
                in.close();
                out.close();
                socket.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
            e.printStackTrace();
        }

        System.out.println("UDP Packet has been sent to Client");
    }

    /**
     * Change the Energy Status of this Client periodically
     */
    public void changeEnergyPointsPeriodically(){

        /*
         * Change current Energy Points every 10 seconds
         */
        new Timer().schedule(new TimerTask() {
            public void run()  {

                System.out.println("Energy Points before changes: " + needEnergyPoints);

                /*
                 * Create a Calendar instance for finding the current time
                 */
                Calendar cal = Calendar.getInstance();

                /*
                 * Find the current hour
                 */
                SimpleDateFormat sdf = new SimpleDateFormat("HH");

                /*
                 * If the hour is between 21:00 and 6:00
                 */
                if((Integer.valueOf(sdf.format(cal.getTime())) > 20 && Integer.valueOf(sdf.format(cal.getTime())) < 24) || (Integer.valueOf(sdf.format(cal.getTime())) > -1 && Integer.valueOf(sdf.format(cal.getTime())) < 7)){

                    /*
                     * Reduce the Energy Points that Client needs to half
                     */
                    needEnergyPoints = needEnergyPoints/2;
                }else{

                    /*
                     * If it's not night
                     * Create a Random instance
                     */
                    Random rand = new Random();

                    /*
                     * Random variable will be in [0,2]
                     */
                    double value = rand.nextDouble() * 2 ;

                    /*
                     * Change the Energy Points that Client needs randomly in addition to Current Energy Points of the Client
                     */
                    needEnergyPoints =(int) (currentEnergyPoints * value);
                }
                if(needEnergyPoints == 0){
                    needEnergyPoints = 2;
                }else if(needEnergyPoints < 0){
                    needEnergyPoints = 0;
                }
                System.out.println("Energy Points after changes : " + needEnergyPoints);

                if(energySuppliers.size() == 0 && needEnergyPoints < requestedEnergy){
                    System.out.println("\nLess EnergyPoints than before");
                }

                if(needEnergyPoints > requestedEnergy){
                    System.out.println("\nMore EnergyPoints than before");
                }
            }
        }, 1, 10000);
    }
}
