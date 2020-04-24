import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Scanner;
import java.io.Writer;

/**
 * FileStoreImpl is the main implementation class for all of the server
 * instances. Provides a distributed text file storage across 5 servers that are
 * coordinated using a Paxos algorithm.
 *
 * @author Neil Routley, Sanchit Saini, Abasiama Akpan
 * @since 04/24/2020
 */
public class FileStoreImpl extends UnicastRemoteObject implements FileStore {
  private static final long serialVersionUID = 1L;

  // date formatter
  SimpleDateFormat date = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss:SSS");

  // state and storage variables
  int[] serverArr;
  FileStore[] serverRpcArr;
  boolean abort, commit, ready, setup, wait = false;
  int votes, acks = 0;
  String msg, command, fileName, contents = "";
  Date prepDate;
  int promisedHost;

  /**
   * Constructor taking in an array of server ports
   *
   * @param serverArr integer array of server ports
   * @throws RemoteException an exception if anything goes wrong
   */
  FileStoreImpl(int[] serverArr) throws RemoteException {
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
    String[] parsedMessages = message.split(" ");

    // check if we actually have a message to check
    if (parsedMessages.length == 0) {
      return (date.format(new Date()) + ": Invalid operation.");
    }

    // setup human readable variables
    this.command = parsedMessages[0].toLowerCase();

    if (parsedMessages.length > 1) {
      this.fileName = parsedMessages[1];
      this.contents = parsedMessages.length >= 3
          ? message.substring(parsedMessages[0].length() + parsedMessages[1].length() + 2, message.length())
          : "empty";
    }

    // save message to use later
    this.msg = message;

    // evaluate statement for next steps
    if (command.equals("list")) {
      String ret = "";

      String homeDir = System.getProperty("user.dir");

      File f = new File(homeDir + "/ServerFiles" + serverArr[0] + "/");
      String[] fileList = f.list();
      for (int i = fileList.length - 1; i >= 0; i--) {
        if (i == 0) {
          ret += fileList[i];
        } else {
          ret += fileList[i] + "\n";
        }

      }

      return ret;

    } else if (command.equals("upload") || command.equals("remove")) {
      System.out.println(date.format(new Date()) + ": begin_commit");
      messageAll("prepare");
    } else if (command.equals("download")) {
      return readFile(fileName);
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

      // if we have all 4 ack messages, lets commit to our filestore
      if (acks >= 2) {
        String res = "no action taken.";
        if (command.equals("upload")) {
          // need to separate the filename and contents from the incoming client message
          // for this function
          res = writeFile(fileName, contents);
        } else {
          // need to separate the filename from the message for this function
          res = deleteFile(fileName);
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
    FileStore access = null;
    try {
      access = (FileStore) Naming.lookup("rmi://localhost:" + port + "/filestore" + port);
    } catch (Exception e) {
      System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
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
   * commit rpc will attempt to make a commit against the local filestore and
   * respond with an ack message to the issuing server.
   *
   * @param port  requesting server port for response
   * @param cmd   a command message type for commit (put or delete)
   * @param key   key string for the filestore
   * @param value value string for the filestore
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  public void commit(int port, String cmd, String fileName, String contents) {
    FileStore access = null;
    try {
      access = (FileStore) Naming.lookup("rmi://localhost:" + port + "/filestore" + port);
    } catch (Exception e) {
      System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
    }

    if (port == promisedHost && commit || ready) {
      if (cmd.toLowerCase().equals("upload")) {
        // try to write to a file locally
        String writeStatus = writeFile(fileName, contents);
        System.out.println(date.format(new Date()) + " write status: " + writeStatus);

        // reply with ack to let the controling server know we finished our commit
        // successfully
        if (ready) {
          try {
            access.reply("ack");
          } catch (Exception e) {
            System.err.println(date.format(new Date()) + ": Error connecting to rpc: " + e);
          }
        }
      } else if (cmd.toLowerCase().equals("remove")) {
        try {
          String deleteRes = deleteFile(fileName);
          System.out.println(date.format(new Date()) + " " + deleteRes);

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
      System.out.println(date.format(new Date()) + " Something went wrong.");
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

  /**
   * delete method will remove a file from the local server
   * 
   * @param fileName name of file to remove
   * @return status message
   */
  private String deleteFile(String fileName) {
    String homeDir = System.getProperty("user.dir");
    File f = new File(homeDir + "/ServerFiles" + serverArr[0] + "/" + fileName);

    if (f.delete()) {
      return "deleted";
    } else {
      return "failed";
    }
  }

  /**
   * A function that writes to files in the 'ServerFiles<port>' directory
   * 
   * @param fileName name of the file
   * @param response the contents to be written
   * @return write command status
   */
  private String writeFile(String fileName, String contents) {
    String res = "write failed";
    String homeDir = System.getProperty("user.dir");
    String filepath = homeDir + "/ServerFiles" + serverArr[0] + "/";

    try (Writer writer = new BufferedWriter(
        new OutputStreamWriter(new FileOutputStream(filepath + fileName), "utf-8"))) {
      writer.write(contents);
      res = "Successful!";
    } catch (IOException e) {
      e.printStackTrace();
    }

    return res;
  }

  /**
   * read a files contents from the local storage
   * 
   * @param fileName name of file to read
   * @return all the data from the file
   */
  private String readFile(String fileName) {
    String data = "error reading file";
    try {
      String homeDir = System.getProperty("user.dir");
      File myObj = new File(homeDir + "/ServerFiles" + serverArr[0] + "/" + fileName);

      Scanner myReader = new Scanner(myObj);
      data = fileName + " ";
      while (myReader.hasNextLine()) {
        data += myReader.nextLine() + "\n";
      }
      // removes the trailing new line character \n
      data = data.substring(0, data.length() - 1);

      myReader.close();
    } catch (FileNotFoundException e) {
      System.out.println(date.format(new Date()) + " An error occurred.");
      e.printStackTrace();
    }
    return data;
  }

  /**
   * Sets up our rpc connections from the array of ports using the Naming lookup
   * service. This will only run once on initial call.
   */
  private void setupConnections() {
    // setup connections for all connected servers
    serverRpcArr = new FileStore[serverArr.length];
    for (int i = 1; i < serverArr.length; i++) {
      try {
        serverRpcArr[i] = (FileStore) Naming.lookup("rmi://localhost:" + serverArr[i] + "/filestore" + serverArr[i]);
      } catch (Exception e) {
        System.err.println(date.format(new Date()) + ": Error connecting to remote rpc: " + e);
      }
    }
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
          serverRpcArr[i].commit(serverArr[0], command, fileName, contents);
        } else if (type.equals("abort")) {
          serverRpcArr[i].abort();
        }
      } catch (Exception e) {
        // abort = true;
        System.out.println(date.format(new Date()) + ": Error sending " + type + " to server: " + "rmi://localhost:"
            + serverArr[i] + "/filestore" + serverArr[i]);
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
    this.fileName = "";
    this.contents = "";
    this.votes = 0;
    this.acks = 0;
    this.prepDate = null;
    this.promisedHost = -1;
  }
}
