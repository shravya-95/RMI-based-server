import java.rmi.server.UnicastRemoteObject;
import java.rmi.RMISecurityManager;
import java.rmi.RemoteException;
import java.rmi.Naming;
import java.rmi.registry.*;
import java.util.Hashtable;
import java.io.*;
public class BankServerImpl implements BankServer {
  protected static Hashtable<Integer, Account> accounts;
  private static int uuidCount = 0;
  public BankServerImpl () throws RemoteException{
    super();
  }
  public int getNewUid(){
    return ++uuidCount;
  }
  class Account{
    public int uid;// unique Id for accounts - an integer sequence counter starting with 1
    int balance = 0;
    public Account(int uid){
      this.uid=uid;
    }
    public int withdraw(int amount) {

      this.balance = this.balance - amount;
      return this.balance;

    }
    public int deposit(int amount){
      this.balance = this.balance+amount;
      return this.balance;
    }
    public int getBalance(){
      return this.balance;
    }
  }

  public synchronized static void writeToLog(String fileName, String content) {
    try {

      File oFile = new File(fileName);
      if (!oFile.exists()) {
        oFile.createNewFile();
      }
      if (oFile.canWrite()) {
        BufferedWriter oWriter = new BufferedWriter(new FileWriter(fileName, true));
        oWriter.write(content);
        oWriter.close();
      }
    } catch (IOException e) {
      e.printStackTrace();
    }
  }

  public int createAccount(){
    int uid = getNewUid();
    Account account = new Account(uid);
    accounts.put(uid, account);
    String[] content = new String[3];
    content[0]="createAccount";
    content[1]="";
    content[2]= String.valueOf(uid);
    String logMsg = String.format("Operation: %s | Inputs: %s | Result: %s \n", (Object[]) content);
    writeToLog("severLogfile.txt",logMsg);
    return uid;
  }

  public boolean deposit(int uid, int amount){
    Account account = accounts.get(uid);
    boolean status = true;
    if (account==null){
      //TODO: add logging for this
        System.out.printf("Account uid %d not found",uid);
        //TODO: change to return false here in TCP
        status = false;
    }
    account.deposit(amount);
    String[] content = new String[3];
    content[0]="deposit";
    content[1]= "UID: "+uid + "," + "Amount:" + amount;
    content[2]= String.valueOf(status);
    String logMsg = String.format("Operation: %s | Inputs: %s | Result: %s \n", (Object[]) content);
    writeToLog("severLogfile.txt",logMsg);
    return status;
  }
  public int getBalance(int uid){
      Account account = accounts.get(uid);
      if (account==null){
        System.out.printf("Account uid %d not found",uid);
        return -1;
      }
      int balance = account.getBalance();
      String[] content = new String[3];
      content[0]="getBalance";
      content[1]="UID: "+uid;
      content[2]= String.valueOf(balance);
      String logMsg = String.format("Operation: %s | Inputs: %s | Result: %s \n", (Object[]) content);
      writeToLog("severLogfile.txt",logMsg);
      return balance;
  }
  public synchronized boolean transfer(int sourceUid, int targetUid, int amount){
//    synchronized (this){
      if(!accounts.containsKey(sourceUid)){
        writeToLog("severLogfile.txt", "Accounts doesn't have key"+String.valueOf(sourceUid));
      }
      if(accounts.get(sourceUid).getBalance()<amount){
        //write to log file
          String[] content = new String[3];
          content[0]="transfer";
          content[1]="From:"+ sourceUid +", To:"+ targetUid +", Amount:"+ amount;
          content[2]= "False";
          String logMsg = String.format("Operation: %s | Inputs: %s | Result: %s \n", (Object[]) content);
          writeToLog("severLogfile.txt",logMsg);
        return false;
      }
      accounts.get(sourceUid).withdraw(amount);
      accounts.get(targetUid).deposit(amount);
      String msg = "Transferred %d from %d to %d\n";
      System.out.printf(msg,amount,sourceUid,targetUid);
      notifyAll();
      String[] content = new String[3];
      content[0]="transfer";
      content[1]="From:"+ sourceUid +", To:"+ targetUid +", Amount:"+ amount;
      content[2]= "True";
      String logMsg = String.format("Operation: %s | Inputs: %s | Result: %s \n", (Object[]) content);
      writeToLog("severLogfile.txt",logMsg);
      return true;
//    }
  }

  public static void main (String args[]) throws Exception {
    if (System.getSecurityManager() == null) {
      System.setSecurityManager(new SecurityManager());
    }
    BankServerImpl  bankServer  = new BankServerImpl( );
    BankServer bankServerStub  =  (BankServer) UnicastRemoteObject.exportObject(bankServer, 0) ;

    if ( args.length == 0 ) {
      // If no port number is given for the rmiregistry, assume it is on the default port 1099
      Naming.bind ("BankServer", bankServerStub);
    }
    else {
      // rmiregistry is on port specified in args[0]. Bind to that registry.
      Registry localRegistry = LocateRegistry.getRegistry( Integer.parseInt( args[0] ));
      localRegistry.bind ("BankServer", bankServerStub);
    }
    accounts = new Hashtable<>();
  }
}
