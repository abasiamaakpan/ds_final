import java.io.File;
import java.rmi.Naming;
import java.rmi.registry.LocateRegistry;

/**
 * PaxosServer main runner class that will bind to a port and setup the server
 * implementation from the FileStoreImpl class.
 *
 * @author Neil Routley, Sanchit Saini, Abasiama Akpan
 * @since 04/24/2020
 */
public class PaxosServer extends Thread {

  /**
   * A function that creates a 'ClientFiles' directory in the current directory.
   * This directory is the root directory for our distributed file system.
   */
  private static void createDirectory(int port) {
    String currentDir = System.getProperty("user.dir");
    File tempDir = new File(currentDir + "/ServerFiles" + port);
    boolean exists = tempDir.exists();
    if (!exists) {
      tempDir.mkdir();
    }
  }

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
      System.out.println("Usage: java -jar FileServer.jar <port1> <port2> <port3> <port4> <port5>");
      System.exit(1);
    }

    createDirectory(serverArr[0]);

    try {
      FileStore obj = new FileStoreImpl(serverArr);
      LocateRegistry.createRegistry(serverArr[0]);
      Naming.rebind("rmi://localhost:" + serverArr[0] + "/filestore" + serverArr[0], obj);
      System.out.println(serverArr[0] + " RPC server started");
    } catch (Exception e) {
      System.out.println("error in rpc server: " + e);
    }
  }

}