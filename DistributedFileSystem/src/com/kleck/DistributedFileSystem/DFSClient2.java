package com.kleck.DistributedFileSystem;

import java.io.BufferedReader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class DFSClient2 {
	private DFSServerInterface dfsServer;
	private String rmiMaster;
	
	public DFSClient2() { 
		//sets security so you can connect to rmi
		//"rmi://localhost/DFSServer"
		System.setProperty("java.security.policy","security.policy");
		
		//set the DFS master initially
		try {
			this.dfsServer = (DFSServerInterface) Naming.lookup("rmi://localhost/DFSServer");
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (MalformedURLException e) {
			e.printStackTrace();
		} catch (NotBoundException e) {
			e.printStackTrace();
		}
		runClient();
	}
	
	private void runClient() {
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			//get the user command (put, get, delete)
			String command = "";
			System.out.print("Enter a command:");
			try {
				command = inFromUser.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			
			//get user input
			if(command.split(" ").length > 3 || command.split(" ").length < 2) {
				System.out.println("Invalid command");
			}
			else if(command.split(" ")[0].equals("put")) {
				if(command.split(" ").length < 3) {
					System.out.println("Invalid command");
				}
				else {
					boolean fileCheck = checkLocalFilename(command.split(" ")[1]);
					if(!fileCheck)
						System.out.println("Could not find local file " + command.split(" ")[1]);
					else {
						try {
							Path pathToFile = Paths.get(command.split(" ")[1]);
							this.dfsServer.put(command.split(" ")[2], Files.readAllBytes(pathToFile), true);
						} catch (RemoteException e) {
							e.printStackTrace();
						} catch (IOException e) {
							e.printStackTrace();
						}
					}
				}
			}
			else if(command.split(" ")[0].equals("get")) {
				if(command.split(" ").length < 3) {
					System.out.println("Invalid command");
				}
				else {
					try {
						byte[] fileToSave = this.dfsServer.get(command.split(" ")[1], true);
						FileOutputStream fos;
						fos = new FileOutputStream("FromDFS/" + command.split(" ")[2]);
						fos.write(fileToSave);
						fos.close();
					} catch (FileNotFoundException e1) {
						// TODO Auto-generated catch block
						e1.printStackTrace();
					} catch (RemoteException e) {
						e.printStackTrace();
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					} 
				}
			}
			else if(command.split(" ")[0].equals("delete")) {
				try {
					this.dfsServer.delete(command.split(" ")[1], true);
				} catch (RemoteException e) {
					e.printStackTrace();
				}
			}
			
			
			
		}
			
	}
	
	//main
	public static void main(String[] args) {
		new DFSClient2();
	}
	
	//see if file is valid
	private boolean checkLocalFilename(String filePath) {
		File f = new File(filePath);
		boolean result = f.exists() && !f.isDirectory();
		return result;
	}

}
