package com.kleck.DistributedFileSystem;


import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.ConnectException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class DFSClientThread extends Thread {
	private DFSServerInterface dfsServer;
	private String rmiServer;
	private String command;
	
	public DFSClientThread(String rmiServer, String command) { 
		this.rmiServer = rmiServer;
		this.command = command;
	}
	
	public void run() {		
		//sets security so you can connect to rmi
		//"rmi://localhost/DFSServer"
		System.setProperty("java.security.policy","security.policy");
		
		//set the DFS master initially
		try {
			this.dfsServer = (DFSServerInterface) Naming.lookup(rmiServer);
			//get the user command (put, get, delete)		
			if(command.split(" ")[0].equals("put")) {
				Path pathToFile = Paths.get(command.split(" ")[1]);
				this.dfsServer.put(command.split(" ")[2], Files.readAllBytes(pathToFile), true);
			}
			if(command.split(" ")[0].equals("get")) {
				byte[] fileToSave = this.dfsServer.get(command.split(" ")[1], true);
				if(fileToSave != null) {
					FileOutputStream fos;
					fos = new FileOutputStream("FromDFS/" + command.split(" ")[2]);
					fos.write(fileToSave);
					fos.close();
				}
				else {
					System.out.println("File Not Found On SDFS.");
				}
			}
			else if(command.split(" ")[0].equals("delete")) {
				this.dfsServer.delete(command.split(" ")[1], true);
			}		
		} catch (RemoteException e) {
			e.printStackTrace();
			//Thread.currentThread().interrupt();
			//return;
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (ConnectException e) {
			//issue connecting to server assume it failed
			return;
			//e.printStackTrace();
		} catch (NotBoundException e) {
			//could not find the server it must have failed
			return;
			//e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
