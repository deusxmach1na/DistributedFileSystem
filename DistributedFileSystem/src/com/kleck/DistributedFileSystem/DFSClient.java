package com.kleck.DistributedFileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Properties;


public class DFSClient {
    public static void main(String[] args) throws IOException {
        
        //read hostname file, load into props
        Properties props = loadParams();
        String[] hosts = props.getProperty("servers").split(";");
        String command = "";
        
        //get BufferedReader to read input from user
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        
        while((command = br.readLine()) != null) {
        	//break out of here if the user types exit
        	if(command.trim().equals("exit"))	
        		break;
        	
        	//otherwise start client threads
        	//and pass the command
        	startClientThreads(hosts, command, false);  
        }     
    }

    
    //starts 1 client thread per host 
    //passes the command to each client thread
    //this method used in Testing
	public static void startClientThreads(String[] hosts, String command, boolean isLogTest) {
		//put ClientThreads in an arrayList for easier management
		ArrayList<DFSClientThread> lct = new ArrayList<DFSClientThread>();
		String tempCommand = command;
		//start each client thread and wait for
		//them to finish processing the command
		if(!command.trim().equals("")) {
		    for(int i=0;i<hosts.length;i++) {
		    	String[] host = hosts[i].split(",");
		    	if(!host[0].equals("") && !host[1].equals("")) {
		            lct.add(new DFSClientThread(host[0], Integer.parseInt(host[1]), command));
		            lct.get(i).start();	
		    	}
		    }	        
		    
		    //wait for all threads to return
		    for(int i=0;i<lct.size();i++){
		    	try {
					lct.get(i).join();
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
		    }
		}

	
	}
    
	//open property file to get the hostName and portNumber
	public static Properties loadParams() {
	    Properties props = new Properties();
	    InputStream is = null;
	    
	    //load file
	    try {
	        File f = new File("./settings.prop");
	        is = new FileInputStream(f);
	 
	        // Try loading properties from the file (if found)
	        props.load(is);
	        is.close();
	    }
	    catch (Exception e) { 
	    	System.out.println("Did not find hostname file. Ensure it is in the same folder as the jar.");
	    }
	    
	    return props;
	}
}
