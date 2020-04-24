import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.Date;

/**
 * FileStore interface class to setup RPC methods
 * 
 * @author Neil Routley, Sanchit Saini, Abasiama Akpan
 * @since 04/24/2020
 */
public interface FileStore extends Remote {

  /**
   * Main function that is called to process client request messages.
   * 
   * @param message a string containing the client message
   * @return response to the client after processing
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  String clientRequest(String key) throws RemoteException;

  /**
   * prepare rpc will check if we can enter a ready state to wait for a commit
   * 
   * @param port integer port for the server requesting
   * @param time is the time the request was made for id
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  void prepare(int port, Date time) throws RemoteException;

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
  void commit(int port, String cmd, String key, String value) throws RemoteException;

  /**
   * reply rpc is a reply from slave servers to count and check whatever vote or
   * ack they submit
   * 
   * @param type string for the kind of reply received
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  void reply(String type) throws RemoteException;

  /**
   * abort method logs the event and initates the reset of state
   * 
   * @throws RemoteException if anything happens when trying to execute rpc
   */
  void abort() throws RemoteException;
}
