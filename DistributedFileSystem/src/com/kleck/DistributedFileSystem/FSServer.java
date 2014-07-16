package com.kleck.DistributedFileSystem;

import java.io.IOException;
import java.net.BindException;
import java.net.ServerSocket;
import java.net.Socket;

public class FSServer {
	private int portNumber;
	private int serverNumber;
	private ServerSocket serverSocket;
	private GroupServer gs;

	//spin up a new GroupServer for gossip
	//LoggingServer constructor
	//needs to take a portNumber and the server id
	public FSServer(int portNumber, int gossipPort, int serverNumber, boolean isContact) {
		this.portNumber = portNumber;
		this.serverNumber = serverNumber;
		this.gs = new GroupServer(gossipPort, isContact, portNumber);
		this.gs.start();
		this.gs.setFs(this);
		try {
			//initialize server socket
			this.serverSocket = new ServerSocket(this.portNumber);
			//create a new thread when client connects
			while(true) {
				Socket sock = serverSocket.accept();
				FSServerThread fst = new FSServerThread(sock, this);
				fst.start();
			}
		}	
		catch (BindException be) {
			System.out.println("File Server has already been started on port number " + portNumber + ".");
		}
		catch (IOException e) {
			System.out.println("I/O Error listening on port number " + portNumber + ".");
		}	 
		
	}

	//main method
	//get args and spin up the Logging Server
	public static void main (String args[]) {
		int port = 6666;
		int gossipPort = 6665;
		int server = 1;
		boolean isContact = false;
		
		//change port and server if args are passed
		if(args.length == 4) {
			port = Integer.parseInt(args[0]);
			gossipPort = Integer.parseInt(args[1]);
			server = Integer.parseInt(args[2]);
			isContact = Boolean.parseBoolean(args[3]);
		}
		if(args.length < 4) {
			System.out.println("Using default port " + port + ", and default server Number " + server +".");
		}
		else {
			System.out.println("Using custom port " + port + ", and server Number " + server + ".");
		}
		new FSServer(port, gossipPort, server, isContact);
	}

	//what server is this
	public int getServerNumber() {
		return this.serverNumber;
	}
	
	//groupServer
	public GroupServer getGs() {
		return gs;
	}

	public void setGs(GroupServer gs) {
		this.gs = gs;
	}
	
}
