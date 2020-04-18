import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * PaxosServer main runner class that will bind to a port and setup the server
 * implementation from the KeyStoreImpl class.
 * 
 * @author Neil Routley
 * @since 04/17/2020
 */
public class PaxosServer extends Thread {

  /**
   * Main method to start the server.
   * 
   * @param args port numbers where the server(and its replicas) will be running
   */
  public static void main(String args[]) {

    int[] serverArr = new int[args.length];

    if (args.length == 5) {
      for (int i = 0; i < args.length; i++) {
        serverArr[i] = Integer.valueOf(args[i]);
      }
    } else {
      System.out.println("Usage: java PaxosServer <port1> <port2> <port3> <port4> <port5>");
      System.exit(1);
    }

    try {
      KeyStore obj = new KeyStoreImpl(serverArr);
      LocateRegistry.createRegistry(serverArr[0]);
      Naming.rebind("rmi://localhost:" + serverArr[0] + "/keystore" + serverArr[0], obj);
      System.out.println(serverArr[0] + " RPC server started");
    } catch (Exception e) {
      System.out.println("error in rpc server: " + e);
    }
  }

}