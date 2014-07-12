package com.kleck.DistributedFileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class DFSClient {
	private Properties prop;
	
	public DFSClient() { 
		this.prop = loadParams();
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
			
			if(command.equals("quit")) {
				break;
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
						spinUpThreads(command);
					}
				}
			}
			else if(command.split(" ")[0].equals("get")) {
				if(command.split(" ").length < 3) {
					System.out.println("Invalid command");
				}
				else {
					spinUpThreads(command);
				}
			}
			else if(command.split(" ")[0].equals("delete")) {
				spinUpThreads(command);
			}
		}	
	}
	
	private void spinUpThreads (String command) {
		List<DFSClientThread> sends = new ArrayList<DFSClientThread>();
		String[] hosts = prop.getProperty("servers").split(";");
		
		//get a list of the servers and ports
	    for(int i=0;i<hosts.length;i++) {
	    	String[] host = hosts[i].split(",");
	    	if(!host[0].equals("") && !host[1].equals("")) {
	    		//"rmi://localhost/DFSServer"
	    		String rmiServer = "rmi://" + host[0] + "/" + host[0].replace(".", "") + host[1];
	            sends.add(new DFSClientThread(rmiServer, command));
	            sends.get(i).start();	
	    	}
	    }	        
	    
	    //wait for all threads to return
	    for(int i=0;i<sends.size();i++){
	    	try {
	    		sends.get(i).join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
	    }
	}
	
	//main
	public static void main(String[] args) {
		new DFSClient();
	}
	
	//see if file is valid
	private boolean checkLocalFilename(String filePath) {
		File f = new File(filePath);
		boolean result = f.exists() && !f.isDirectory();
		return result;
	}
	
	//open property file to get the hostName and portNumber
	public static Properties loadParams() {
	    Properties props = new Properties();
	    InputStream is = null;
	    //load file
	    try {
	        File f = new File("settings.prop");
	        is = new FileInputStream(f);
	        // Try loading properties from the file (if found)
	        props.load(is);
	        is.close();
	    }
	    catch (Exception e) { 
	    	System.out.println("Did not find property file settings.prop.\nEnsure it is in the same folder as the jar.");
	    }
	    return props;
	}

}
