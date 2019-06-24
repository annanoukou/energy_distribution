public class EnergyStatus {

    private int maxEnergyPoints;        //max Energy Points that the Client can have
    private int currentEnergyPoints;    //current Energy Points that the Client has
    private int needEnergyPoints;       //Energy Points that the Client needs
    private String username;            //Client's Username
    private String ip;                  //IP of the Client
    private int waitingTime;            //Waiting time of the Client at Waiting List
    private int t;                      //The time he has been chosen as Supplier
    private int receivedEnergyPoints;   //Energy Points that has been received to this Client from others
    private int givenEnergyPoints;      //Energy Points that this Client has gave to others
    private int supplierProbability;    //The probability that this Client will be a Supplier

    /*
     * Choice
     * 1 : Client want to be supplied by two other Clients
     * 2 : Client want to be putted in WaitingList for a specific WaitingTime
     */
    private int choice;

    /**
     * Constructor of EnergyStatus
     * @param maxEnergyPoints max Energy Points that the Client can have
     * @param currentEnergyPoints current Energy Points that the Client has
     * @param needEnergyPoints Energy Points that the Client needs
     * @param ip IP of the Client
     * @param username Client's Username
     */
    public EnergyStatus(int maxEnergyPoints, int currentEnergyPoints, int needEnergyPoints, String ip, String username){
        this.maxEnergyPoints = maxEnergyPoints;
        this.currentEnergyPoints = currentEnergyPoints;
        this.needEnergyPoints = needEnergyPoints;
        this.username = username;
        this.ip = ip;
        this.t=0;
        this.receivedEnergyPoints = 0;
        this.givenEnergyPoints = 0;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public int getMaxEnergyPoints() {
        return maxEnergyPoints;
    }

    public void setMaxEnergyPoints(int maxEnergyPoints) {
        this.maxEnergyPoints = maxEnergyPoints;
    }

    public int getCurrentEnergyPoints() {
        return currentEnergyPoints;
    }

    public void setCurrentEnergyPoints(int currentEnergyPoints) {
        this.currentEnergyPoints = currentEnergyPoints;
    }

    public int getNeedEnergyPoints() {
        return needEnergyPoints;
    }

    public void setNeedEnergyPoints(int needEnergyPoints) {
        this.needEnergyPoints = needEnergyPoints;
    }

    public String getIp() {
        return ip;
    }

    public void setIp(String ip) {
        this.ip = ip;
    }

    public int getWaitingTime() {
        return waitingTime;
    }

    public void setWaitingTime(int waitingTime) {
        this.waitingTime = waitingTime;
    }

    public int getChoice() {
        return choice;
    }

    public void setChoice(int choice) {
        this.choice = choice;
    }

    public int getT() {
        return t;
    }

    public void setT(int t) {
        this.t = t;
    }

    public int getReceivedEnergyPoints() {
        return receivedEnergyPoints;
    }

    public void setReceivedEnergyPoints(int receivedEnergyPoints) {
        this.receivedEnergyPoints = receivedEnergyPoints;
    }

    public int getGivenEnergyPoints() {
        return givenEnergyPoints;
    }

    public void setGivenEnergyPoints(int givenEnergyPoints) {
        this.givenEnergyPoints = givenEnergyPoints;
    }

    public int getSupplierProbability() {
        return supplierProbability;
    }

    public void setSupplierProbability(int supplierProbability) {
        this.supplierProbability = supplierProbability;
    }
}
