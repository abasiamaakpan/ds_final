import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.FileWriter;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.io.BufferedWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.FileSystems;

/**
 * KeyStoreImpl is the main implementation class for all of the server
 * instances. Provides a distributed keyvalue storage across 5 servers that are
 * coordinated using a Paxos algorithm.
 *
 * @author Neil Routley
 * @since 04/17/2020
 */
public class KeyStoreImpl extends UnicastRemoteObject implements KeyStore {
  private static final long serialVersionUID = 1L;
  // keyvalue storage
  Map<String, String> keystore = Collections.synchronizedMap(new HashMap<>());

  // date formatter
  SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");

  // state and storage variables

  int[] serverArr;
  KeyStore[] serverRpcArr;
  boolean abort, commit, ready, setup, wait = false;
  int votes, acks = 0;
  String msg, command, key, value = "";
  Date prepDate;
  int promisedHost;

  private static Path rootDirectory = FileSystems.getDefault().getPath("~");

  /**
   * Constructor taking in an array of server ports
   *
   * @param serverArr integer array of server ports
   * @throws RemoteException an exception if anything goes wrong
   */
  KeyStoreImpl(int[] serverArr) throws RemoteException {
    super();
    this.serverArr = serverArr;
  }

  /**
   * Main function that is called to process client request messages.
   *
   * @param message a string containing the client message
   * @return response to the client after processing
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public String clientRequest(String message) {
    System.out.println("From Impl file " + message);

    String[] parsedMessages = message.split(" ");

    System.out.println(parsedMessages.length);

    // check if we actually have a message to check
    if (parsedMessages.length == 0) {
      return (date.format(new Date()) + ": Invalid operation.");
    }

    // setup human readable variables
    this.command = parsedMessages[0].toLowerCase();
    System.out.println(command);

    if (parsedMessages.length > 1) {
      this.key = parsedMessages[1];
      this.value = parsedMessages.length == 3 ? parsedMessages[2] : "empty";
    }
    System.out.println(command.equals("remove"));

    // save message to use later
    this.msg = message;

    // System.out.println("Is get called here...");

    // evaluate statement for next steps
    if (command.equals("get")) {

      // System.out.println("Is get called here...");

      // check if we have that key in our keystore
      if (!keystore.containsKey(key)) {
        return ("The key you entered does not exist!");
      } else {
        return ("{" + keystore.get(key) + "}");
      }

    } else if (command.equals("put") || command.equals("delete")) {
      // System.out.println("In put or delete");

      // if delete, check if we have to do anything at all
      if (command.equals("delete") && !keystore.containsKey(key)) {
        return ("The key you entered does not exist!");
      }

      // if we get here, all should be good, so lets try to make the 2 stage commit.
      System.out.println(date.format(new Date()) + ": begin_commit");
      messageAll("prepare");
    } else if (command.equals("list")) {
      // System.out.println("In list Function" + command.equals("list"));
      String ret = "";

      // File f=null;
      // Path tempDirectory = Files.createTempDirectory(rootDirectory, "");
      // //System.out.println("Temporary directory created successfully!");
      // String dirPath = tempDirectory.toString();
      // System.out.println(dirPath);
      // f = File.createTempFile(key, ".txt", new
      // File("/var/folders/t3/xhjd4pfs3v1f1ywlkd96jp640000gn/T/"));
      // System.out.println("Temporary file created successfully!");

      // File f = new File(tmpCustomPrefix + "/" + key);

      String homeDir = System.getProperty("user.home");

      File f = new File(homeDir + "/" + key);
      String[] fileList = f.list();
      for (int i = 0; i < fileList.length; i++) {
        System.out.println("list: " + fileList[i]);
        ret += fileList[i] + "\n";
      }

      /*
       * File file = new File("/Users"); String[] fileList = file.list(); String
       * ret=""; for(String name:fileList){ ret+=name; }
       */

      return ret;

    } else if (command.equals("upload") || command.equals("remove")) {
      System.out.println(date.format(new Date()) + ": begin_commit");
      messageAll("prepare");
    } else if (command.equals("read")) {
      // if (!keystore.containsKey(key)) {
      // return ("The key you entered does not exist!");
      // } else {
      // return ("{" + keystore.get(key) + "}");
      try {

        // File myObj=null;
        // Path tempDirectory = Files.createTempDirectory(rootDirectory, "");
        // //System.out.println("Temporary directory created successfully!");
        // String dirPath = tempDirectory.toString();
        // System.out.println(dirPath);
        // myObj = File.createTempFile(key, ".txt", new
        // File("/var/folders/t3/xhjd4pfs3v1f1ywlkd96jp640000gn/T/"));

        // File myObj = new File(tmpCustomPrefix + "/" + key);

        String homeDir = System.getProperty("user.home");

        File myObj = new File(homeDir + "/" + key);

        Scanner myReader = new Scanner(myObj);
        String data = "";
        // System.out.println("What is data");
        while (myReader.hasNextLine()) {
          data = myReader.nextLine();
          System.out.println(data);
        }
        myReader.close();
        return data;
      } catch (FileNotFoundException e) {
        System.out.println("An error occurred.");
        e.printStackTrace();
      }

      // }

    } else {
      return ("Invalid operation. Try again.");
    }

    // setup timeout to make sure we don't hang if a server crashes or doesn't reply
    // in time
    long start_time = System.currentTimeMillis();
    long wait_time = 10000;
    long end_time = start_time + wait_time;

    // wait loop to allow clients to respond
    while (!abort && wait) {
      // abort if one of the servers take too long
      if (System.currentTimeMillis() > end_time) {
        this.abort = true;
      }

      // if we have all 4 votes, send out commit message to all servers
      if (votes >= 2 && !commit) {
        this.commit = true;
        messageAll("commit");
      }

      // if we have all 4 ack messages, lets commit to our keystore
      if (acks >= 2) {
        String res = "no action taken.";
        if (command.equals("upload")) {
          // need to separate the filename and contents from the incoming client message
          // for this function
          // res = writeFile(fileName, contents);
        } else {
          // need to separate the filename from the message for this function
          // res = deleteFile(fileName);
        }
        System.out.println(date.format(new Date()) + ": end_of_transaction");
        // reset state
        reset();
        return (res);
      }
    }
    if (abort) {
      messageAll("abort");
      abort();
      return ("Aborted.");
    }
    return ("Something went wrong. Try again.");

  }

  /**
   * prepare rpc will check if we can enter a ready state to wait for a commit
   *
   * @param port integer port for the server requesting
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public void prepare(int port, Date time) {
    KeyStore access = null;
    try {
      access = (KeyStore) Naming.lookup("rmi://localhost:" + port + "/keystore" + port);
    } catch (Exception e) {
      System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);

      // reset all state since we had a problem
      // reset();
    }

    if (!wait && !ready || ready && prepDate.compareTo(time) < 0) {
      System.out.println(date.format(new Date()) + ": ready to commit.");
      this.ready = true;
      prepDate = time;
      promisedHost = port;

      // connect and send vote message to the server on the parameter port
      try {
        access.reply("vote-commit");
      } catch (Exception e) {
        System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
      }
    }
  }

  /**
   * commit rpc will attempt to make a commit against the local keystore and
   * respond with an ack message to the issuing server.
   *
   * @param port  requesting server port for response
   * @param cmd   a command message type for commit (put or delete)
   * @param key   key string for the keystore
   * @param value value string for the keystore
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public void commit(int port, String cmd, String key, String value) {
    KeyStore access = null;
    try {
      access = (KeyStore) Naming.lookup("rmi://localhost:" + port + "/keystore" + port);
    } catch (Exception e) {
      System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
    }

    System.out.println("TESTINGGGGGGGGG " + cmd.toLowerCase().equals("remove"));

    if (port == promisedHost && commit || ready) {
      if (cmd.toLowerCase().equals("upload")) {
        try {
          String homeDir = System.getProperty("user.home");
          File f = new File(homeDir + "/" + key);
          FileWriter fileWriter = new FileWriter(f);
          PrintWriter printWriter = new PrintWriter(fileWriter);

          printWriter.println(value);
          printWriter.close();

          // reply with ack to let the controling server know we finished our commit
          // successfully
          if (ready) {
            try {
              access.reply("ack");
            } catch (Exception e) {
              System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
            }
          }
        } catch (IOException e) {
          System.out.println(date.format(new Date()) + "Had an IOException: " + e);
        }
      } else if (cmd.toLowerCase().equals("remove")) {
        try {
          String deleteRes = deleteFile(key);
          System.out.println(date.format(new Date()) + deleteRes);

          // reply with ack to let the controling server know we finished our commit
          // successfully
          if (ready) {
            try {
              access.reply("ack");
            } catch (Exception e) {
              System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
            }
          }
        } catch (Exception e) {
          e.printStackTrace();
        }
      }

      System.out.println(date.format(new Date()) + ": commit recorded.");
      reset();
    } else {
      System.out.println(date.format(new Date()) + "Something went wrong.");
      abort();
    }
  }

  /**
   * reply rpc is a reply from slave servers to count and check whatever vote or
   * ack they submit
   *
   * @param type string for the kind of reply received
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public void reply(String type) {
    if (type.equals("vote-commit")) {
      this.votes += 1;
    } else if (type.equals("vote-abort")) {
      this.abort = true;
    } else if (type.equals("ack")) {
      this.acks += 1;
    }
  }

  /**
   * abort method logs the event and initates the reset of state
   *
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public void abort() {
    System.out.println(date.format(new Date()) + ": aborting.");
    reset();
  }

  private String deleteFile(String fileName) {
    String homeDir = System.getProperty("user.home");
    File f = new File(homeDir + "/" + fileName);

    if (f.delete()) {
      return "deleted";
    } else {
      return "failed";
    }
  }

  private String writeFile(String fileName, String contents) {
    return "write successful";
  }

  /**
   * Sets up our rpc connections from the array of ports using the Naming lookup
   * service. This will only run once on initial call.
   */
  private void setupConnections() {
    // setup connections for all connected servers
    // if (!setup) {
    serverRpcArr = new KeyStore[serverArr.length];
    for (int i = 1; i < serverArr.length; i++) {
      try {
        serverRpcArr[i] = (KeyStore) Naming.lookup("rmi://localhost:" + serverArr[i] + "/keystore" + serverArr[i]);
      } catch (Exception e) {
        System.err.println(date.format(new Date()) + ": Error connecting to remote rpc: " + e);
      }
    }
    // setup = true;
    // }
  }

  /**
   * Sends a message to all servers for the two stage commit process; prepare,
   * commit, and abort
   *
   * @param type is the type of message to send
   * @throws RemoteException
   */
  private void messageAll(String type) {
    setupConnections();

    // send message to all connected servers
    for (int i = 1; i < serverArr.length; i++) {
      try {
        if (type.equals("prepare")) {
          serverRpcArr[i].prepare(serverArr[0], new Date());
        } else if (type.equals("commit")) {
          serverRpcArr[i].commit(serverArr[0], command, key, value);
        } else if (type.equals("abort")) {
          serverRpcArr[i].abort();
        }
      } catch (Exception e) {
        // abort = true;
        System.out.println(date.format(new Date()) + ": Error sending " + type + " to server: " + "rmi://localhost:"
            + serverArr[i] + "/keystore" + serverArr[i]);
      }
    }

    // set state
    if (type.equals("prepare")) {
      this.wait = true;
    } else if (type.equals("commit")) {
      this.commit = true;
    } else if (type.equals("abort")) {
      this.abort = true;
    }
  }

  /**
   * this resets all instance state variables used
   */
  private void reset() {
    this.abort = false;
    this.commit = false;
    this.ready = false;
    this.wait = false;
    this.msg = "";
    this.command = "";
    this.key = "";
    this.value = "";
    this.votes = 0;
    this.acks = 0;
    this.prepDate = null;
    this.promisedHost = -1;
  }
}
