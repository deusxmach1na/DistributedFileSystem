package com.kleck.DistributedFileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
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
			System.out.print("sdfs>");
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
				this.printUsage();
			}
			else if(command.split(" ")[0].equals("put")) {
				if(command.split(" ").length < 3) {
					System.out.println("Invalid command");
					this.printUsage();
				}
				else {
					boolean fileCheck = checkLocalFilename(command.split(" ")[1]);
					if(!fileCheck)
						System.out.println("Could not find local file " + command.split(" ")[1]);
					else {
						//long commandStart = System.currentTimeMillis();
						Path path = Paths.get(command.split(" ")[1]);
						byte[] data = null;
						try {
							data = Files.readAllBytes(path);
						} catch (IOException e) {
							e.printStackTrace();
						}
						spinUpThreads(this.formCommand("put", command.split(" ")[2], true, data), "put none");
						//long commandEnd = System.currentTimeMillis();
						//System.out.println("*********************");
						//System.out.println("**Put Time = " + (commandEnd - commandStart) + " milliseconds.");
						//System.out.println("*********************");
					}
				}
			}
			else if(command.split(" ")[0].equals("get")) {
				if(command.split(" ").length < 3) {
					System.out.println("Invalid command");
					this.printUsage();
				}
				else {
					//long commandStart = System.currentTimeMillis();
					spinUpThreads(this.formCommand("get", command.split(" ")[1], true, new String("").getBytes()), "get " + command.split(" ")[2]);
					//long commandEnd = System.currentTimeMillis();
					//System.out.println("*********************");
					//System.out.println("**Get Time = " + (commandEnd - commandStart) + " milliseconds.");
					//System.out.println("*********************");
				}
			}
			else if(command.split(" ")[0].equals("delete")) {
				spinUpThreads(this.formCommand("del", command.split(" ")[1], true, new String("").getBytes()), "del none");
			}
		}	
	}
	
	//turns the command into a byte array
	public byte[] formCommand(String commandType, String filename, boolean b, byte[] data) {
		byte[] result = new byte[data.length + 64];
		byte[] com = new byte[16];
		com = Arrays.copyOf(commandType.getBytes(), 16);
		byte[] file = new byte[32];
		file = Arrays.copyOf(filename.getBytes(), 32);
		byte[] isFirst = new byte[16];
		if(b)
			isFirst = Arrays.copyOf(new String("true").getBytes(), 16);
		else
			isFirst = Arrays.copyOf(new String("false").getBytes(), 16);
		
		//System.out.println(file.length);
		System.arraycopy(com, 0, result, 0, 16);
		System.arraycopy(isFirst, 0, result, 16, 16);
		System.arraycopy(file, 0, result, 32, 32);
		System.arraycopy(data, 0, result, 64, data.length);
		//result = this.concatenateByte(com, file);
		//result = this.concatenateByte(result, isFirst);
		//result = this.concatenateByte(result, data);
		return result;
	}
	
	private void spinUpThreads (byte[] bs, String command) {
		List<DFSClientThread> sends = new ArrayList<DFSClientThread>();
		String[] hosts = prop.getProperty("servers").split(";");
		
		//get a list of the servers and ports
	    for(int i=0;i<hosts.length;i++) {
	    	String[] host = hosts[i].split(",");
	    	if(!host[0].equals("") && !host[2].equals("")) {
	    		//"rmi://localhost/DFSServer"
	    		String ipAddress = host[0];
	    		int portNumber = Integer.parseInt(host[2]);
	    		sends.add(new DFSClientThread(ipAddress, portNumber, command, bs));
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
	
	//print proper usage
	public void printUsage() {
		System.out.println("Sample Usage:");
		System.out.println("put <localfilename> <sdfsfilename>");
		System.out.println("get <sdfsfilename> <localfilename>");
		System.out.println("delete <sdfsfilename>");
	}
	
	//concat bytes
	public byte[] concatenateByte (byte[] a, byte[] b) {
		byte[] result;
		if(a == null) {
			result = new byte[b.length];
			// copy b to result
			System.arraycopy(b, 0, result, 0, b.length);
		}
		else {
			result = new byte[a.length + b.length];
			// copy a to result
			System.arraycopy(a, 0, result, 0, a.length);
			// copy b to result
			System.arraycopy(b, 0, result, a.length, b.length);
		}
		return result;
	}

}
