# Client and Server Text File Store

Here we have a client using the Paxos algorithm and server program for 5 replicated servers and 1 client. You can run multiple clients, but each one can connect to any server.

# Easy startup

To start 5 servers and 1 client with the predefined ports below, just run the included rullAll.sh script after extracting the zip file.

./runAll.sh

# Manually Starting servers

To start the servers, you will run them individually with all 5 server ports as arguments, the first of which is its own port, followed by the fellow replicated servers. The following examples would be run from a new terminal.

java -jar paxos_server.jar 9090 9091 9092 9093 9094
java -jar paxos_server.jar 9091 9090 9092 9093 9094
java -jar paxos_server.jar 9092 9090 9091 9093 9094
java -jar paxos_server.jar 9093 9090 9091 9092 9094
java -jar paxos_server.jar 9094 9090 9091 9092 9093

# Manually Starting client

To start the client, you need to give it all 5 server ports that it can connect to though its arguments.
java -jar paxos_client.jar 9090 9091 9092 9093 9094

# Performing commands on the client

With all servers running as well as the client, the client will take commands. The general example is shown below for each operation followed by examples of using the keystore.
