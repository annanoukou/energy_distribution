import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.HashMap;
import java.util.List;

public class ClientHandler extends Thread{

    private Socket clientSocket;        //Client's Socket
    private ObjectOutputStream out;     //Stream for sending data
    private ObjectInputStream in;       //Stream for taking data
    private EnergyStatus energyStatus;  //EnergyStatus instance
    private int request = 0;            //Request of Client

    private Server server;              //Previous Server instance

    private int SupplierPort = 5055;    //Port of the EnergyClients who is a Supplier for the current Client

    private HashMap<String, Integer> SuppliedEnergy = new HashMap<>();  //HashMap with all the connected to server Clients who have sent to this Client Energy


    /**
     * Constructor of ClientHandler
     * @param clientSocket  Socket of client
     * @param in    ObjectInputStream of the connection
     * @param out   ObjectOutputStream of the connection
     * @param energyStatus  Energy Status of the client
     * @param server    Server instance
     */
    public ClientHandler(Socket clientSocket, ObjectInputStream in, ObjectOutputStream out, EnergyStatus energyStatus, Server server, int request){
        this.clientSocket = clientSocket;
        this.in = in;
        this.out = out;
        this.energyStatus = energyStatus;
        this.server = server;
        this.request = request;
    }

    @Override
    public void run() {
        System.out.println("Starting new Thread");
            try {

                /*
                 * Print request
                 */
                System.out.println("Request: " + request);

                /*
                 * Request is 1 if the Client wants to update his information at Server
                 */
                if(request == 1){

                    /*
                     * Get from Client the energy Points that he has at the current time
                     */
                    int newCurrentEnergyPoints = in.readInt();

                    /*
                     * Get From Client the HashMap
                     */
                    try {
                        SuppliedEnergy = (HashMap) in.readObject();
                    } catch (ClassNotFoundException e) {
                        e.printStackTrace();
                    }

                    /*
                     * Update Client with given username and with the new energy Points
                     */
                    server.updateEnergyClientInformation(energyStatus.getUsername(), energyStatus.getCurrentEnergyPoints() + newCurrentEnergyPoints);

                    /*
                     * Find energy Suppliers after the update of the EnergyClientInformation List
                     */
                    server.findEnergySupplierAfterUpdate();

                    /*
                     * Send to Client the List with all of his Energy Suppliers
                     */
                    out.writeInt(server.getEnergySuppliers().size());
                    out.flush();

                    /*
                     * Copy of energySuppliers List
                     */
                    List<EnergyStatus> energySupplierss = server.getEnergySuppliers();

                    /*
                     * For each Supplier
                     */
                    for(EnergyStatus energySupplier : energySupplierss){

                        /*
                         * Send Suppliers info to Client in need
                         */
                        System.out.println("Send Energy Supplier info to Client");
                        out.writeUTF(energySupplier.getUsername());
                        out.flush();
                        out.writeUTF(energySupplier.getIp());
                        out.flush();
                        out.writeInt(energySupplier.getMaxEnergyPoints());
                        out.flush();
                        out.writeInt(energySupplier.getCurrentEnergyPoints());
                        out.flush();
                        out.writeInt(energySupplier.getNeedEnergyPoints());
                        out.flush();

                        /*
                         * Close the connection between Server and Client
                         */
                        out.close();
                        in.close();
                        clientSocket.close();

                        /*
                         * Send Client's info to Suppliers
                         */
                        System.out.println("Send Client info to Suppliers: " + energySupplier.getIp() + ", " + SupplierPort);

                        /*
                         * Create connection between Server and Suppliers
                         */
                        Socket socket = new Socket(energySupplier.getIp(), SupplierPort);
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                        /*
                         * Send Information of the Client to Suppliers
                         */
                        System.out.println("inNeedClientIP: " + energyStatus.getIp() + ", " + energyStatus.getNeedEnergyPoints());
                        outputStream.writeUTF(energyStatus.getIp());
                        outputStream.flush();
                        outputStream.writeInt(energyStatus.getNeedEnergyPoints());
                        outputStream.flush();
                        System.out.println("Data has been Sent to Suppliers");

                        /*
                         * Close the connection between Server and Suppliers
                         */
                        outputStream.close();
                        inputStream.close();
                        socket.close();
                    }
                }else if(request == 2){

                    /*
                     * Request = 2 if the Client wants to ask for Energy Points
                     */

                    /*
                     * Get Energy Points that the Client wants
                     */
                    int needEnergyPoints = in.readInt();

                    /*
                     * Set the Energy Points as wanted from the Client
                     */
                    energyStatus.setNeedEnergyPoints(needEnergyPoints);

                    /*
                     * Get Choice(a,b,c) from Client
                     */
                    int choice = in.readInt();

                    server.findEnergySupplier(energyStatus.getUsername(), needEnergyPoints, choice);

                    /*
                     * Send the size of Energy Supplier List to Client
                     */
                    out.writeInt(server.getEnergySuppliers().size());
                    out.flush();

                    /*
                     * Copy of Energy Suppliers List
                     */
                    List<EnergyStatus> energySupplierss = server.getEnergySuppliers();

                    /*
                     * For each Energy Supplier
                     */
                    for(EnergyStatus energySupplier : energySupplierss){

                        /*
                         * Send Suppliers info to Client in need
                         */
                        System.out.println("energySupplier at Server: " + energySupplier.getUsername());
                        out.writeUTF(energySupplier.getUsername());
                        out.flush();
                        out.writeUTF(energySupplier.getIp());
                        out.flush();
                        out.writeInt(energySupplier.getMaxEnergyPoints());
                        out.flush();
                        out.writeInt(energySupplier.getCurrentEnergyPoints());
                        out.flush();
                        out.writeInt(energySupplier.getNeedEnergyPoints());
                        out.flush();

                        /*
                         * Close the connection between Server and Client
                         */
                        out.close();
                        in.close();
                        clientSocket.close();

                        /*
                         * Create connection between Server and Suppliers
                         */
                        System.out.println("Send Client info to Suppliers: " + energySupplier.getIp() + ", " + SupplierPort);
                        Socket socket = new Socket(energySupplier.getIp(), SupplierPort);
                        ObjectOutputStream outputStream = new ObjectOutputStream(socket.getOutputStream());
                        ObjectInputStream inputStream = new ObjectInputStream(socket.getInputStream());

                        /*
                         * Send Client's info to Suppliers
                         */
                        System.out.println("inNeedClientIP : " + energyStatus.getIp() + ", " + needEnergyPoints);
                        outputStream.writeUTF(energyStatus.getIp());
                        outputStream.flush();
                        outputStream.writeInt(needEnergyPoints);
                        outputStream.flush();
                        System.out.println("Data has been Sent to Supplier");

                        /*
                         * Close the connection between Server and Suppliers
                         */
                        outputStream.close();
                        inputStream.close();
                        socket.close();
                    }
                }else{
                    System.out.println("Wrong Request");
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
    }
}
