package com.kleck.DistributedFileSystem;


import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

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
				FileOutputStream fos;
				fos = new FileOutputStream("FromDFS/" + command.split(" ")[2]);
				fos.write(fileToSave);
				fos.close();
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
		} catch (NotBoundException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e1) {
			e1.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
