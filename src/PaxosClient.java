import java.rmi.Naming;
import java.rmi.RemoteException;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PaxosClient is the client for Project 4. This project for connecting to 5
 * servers using the Paxos algorithm. When running, pass the 5 ports for the
 * servers assumed on localhost for this example.
 *
 * java PaxosClient 9090 9091 9092 9093 9094
 *
 * Example: <command> <key> <value>
 *
 * @author Neil Routley
 * @since 04/17/2020
 */
public class PaxosClient {
  static KeyStore[] serverRpcArr;
  static int[] serverArr;
  static SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");

  /**
   * setup the RMI connections for all of the servers
   */
  private static void setupConnections() {
    for (int i = 0; i < serverArr.length; i++) {
      try {
        serverRpcArr[i] = (KeyStore) Naming.lookup("rmi://localhost:" + serverArr[i] + "/keystore" + serverArr[i]);
      } catch (Exception e) {
      }
    }
  }

  /**
   * try to get a value based on a key from all the servers. majority consesus
   * required.
   *
   * @param msg the string message being passed to the servers
   * @return response from the consensus.
   */
  private static String tryGet(String msg) {
    HashMap<String, Integer> map = new HashMap<String, Integer>();
    String res = "";
    // check all servers
    for (int i = 0; i < 5; i++) {
      // try each server here and save the response
      res = tryRmi(i, msg);

      // check map to see if we have a matching key (entire return statement)
      // if we do, +1 our count for that result
      if (map.containsKey(res)) {
        int count = map.get(res);
        map.put(res, count + 1);
        // else we add that result to our HM and give it an initial count of 1
      } else {
        map.put(res, 1);
      }
    }

    // temp result key where we will store the response
    String key = "";
    // temp count for how many of each result we received
    int val = -1;
    // check the whole map to see the highest result count
    for (Map.Entry<String, Integer> entry : map.entrySet()) {
      if (entry.getValue() > val) {
        key = entry.getKey();
        val = entry.getValue();
      }
    }

    // if our highest result value is above our consenses number
    // set that as the real result
    if (val > 2) {
      res = key;
      // otherwise, return some kind of bad response
    } else {
      res = "The key you entered does not exist!";
    }

    return res;
  }

  /**
   * try to submit a put or delete command to any avaliable server.
   *
   * @param msg put or delete message being sent
   * @return response from trying to commit a put or delete
   */
  private static String tryPutDelete(String msg) {
    String res = "";
    for (int i = 0; i < 5; i++) {
      res = tryRmi(i, msg);
      if (!res.equals("")) {
        break;
      }
    }
    return res;
  }

  /**
   * try to submit a query to a specified server. helper function.
   *
   * @param server port which the server is on
   * @param msg    message being sent to the server
   * @return response from attempted query
   */
  private static String tryRmi(int server, String msg) {
    setupConnections();
    String response;
    try {
      response = serverRpcArr[Integer.valueOf(server)].clientRequest(msg);
    } catch (Exception e) {
      response = "";
    }
    return response;
  }

  private static String readLocalFile(String fileName) {
    String fileContents = "";
    // here we need to do the local file read from the fileName and fill the
    // fileContents with what is in that file.

    return "upload " + fileName + fileContents;
  }

  private static String writeFile(String fileName, String response) {
    String res = "download failed";
    // here we need to do a file write from the returned response
    // write new file as fileName
    // write response into that new file
    // change our string res to successful if this all went well to be returned
    return res;
  }

  /**
   * Main Client method that connects to as many servers as you pass arguments.
   * You should pass 5 port arguments for this assignment
   *
   * @param args should pass 5 port arguments
   */
  public static void main(String args[]) {
    serverArr = new int[args.length];

    if (args.length == 5) {
      for (int i = 0; i < args.length; i++) {
        serverArr[i] = Integer.valueOf(args[i]);
      }
    } else {
      System.out.println("Usage: java PaxosClient <port1> <port2> <port3> <port4> <port5>");
      System.exit(1);
    }

    serverRpcArr = new KeyStore[serverArr.length];
    Scanner sc = new Scanner(System.in);

    try {
      // command loop with initial info prompt
      System.out.println("Commands (upload <fileName>, download <fileName>, list, remove <fileName>)");
      while (true) {
        // user prompt
        System.out.print("Enter command:");
        String operation = sc.nextLine();
        String[] myArray = operation.trim().split(" ");

        String res = "";
        if (myArray.length == 1 && myArray[0].toLowerCase().equals("list")) {
          res = tryGet("list");
          // res = tryRmi(0, "list");
          System.out.println("Response" + res);

        } else if (myArray.length == 3 && myArray[0].toLowerCase().equals("upload")) {
          // here we have to read from a file, and create our new "operation" message
          // this will then be sent to the tryPutDelete() function
          String requestMessage = readLocalFile(myArray[1]);
          res = tryPutDelete(requestMessage);
          if (res.equals("")) {
            System.out.println("ERROR - No response.");
          } else {
            System.out.println(res);
          }
        } else if (myArray.length == 2 && myArray[0].toLowerCase().equals("remove")) {
          // should be our delete operation with no modification needed
          res = tryPutDelete(operation);
          if (res.equals("")) {
            System.out.println("ERROR - No response.");
          } else {
            System.out.println(res);
          }
        } else if (myArray.length == 2 && myArray[0].toLowerCase().equals("download")) {
          res = tryGet(operation);
          if (res.equals("")) {
            System.out.println("ERROR - No response.");
          } else {
            // this is where we need to create a file, and output the resut to that file
            // lets return a response that makes sense for the client like "download
            // successful" or "download failed"
            String writeRes = writeFile(myArray[1], res);
            System.out.println(writeRes);
          }
        } else {
          System.out.println("Command invalid. Usage: (upload <fileName>, upload <fileName>, list, remove <fileName>)");
          continue;
        }
      }
    } catch (Exception e) {
      System.err.println("Client exception: " + e);
    }
    sc.close();
  }
}
