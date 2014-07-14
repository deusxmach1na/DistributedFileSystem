package com.kleck.DistributedFileSystem;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.Path;


public class DFSServer extends UnicastRemoteObject implements DFSServerInterface {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private GroupServer gs;
	
	//spin up a new GroupServer for gossip
	public DFSServer(int port, boolean isContact) throws RemoteException {
		super();
		//sets security so you can connect to rmi
		System.setProperty("java.security.policy","security.policy");
		this.gs = new GroupServer(port, isContact);
		this.gs.start();
		this.gs.startRMIServer(this);
	}
	
	//main method
	//get args and spin up the Logging Server
	public static void main (String args[]) {
		int port = 6667;
		boolean isContact = false;
		
		try {
			if(args.length >= 1) {
				port = Integer.parseInt(args[0]);
			}		
		}
		catch (NumberFormatException nfe) {
			System.out.println("Using default port " + port + " for gossip communication.");
		}
		//change port and server if args are passed
		if(args.length == 2) {
			if(args[1].equals("true")) {
				System.out.println("Starting Contact Server");
				isContact = true;
			}
		}
		else {
			System.out.println("Starting Server.");
			isContact = false;
		}	
		try {
			new DFSServer(port, isContact);
		} catch (RemoteException e) {
			e.printStackTrace();
		}
	}


	@Override
	//this needs to take a file and shard it then push it to other servers
	//so it is duplicated the number of times indicated in replicationfactor setting
	public boolean put(String filename, byte[] file, boolean isFirstRun) throws RemoteException {
		//System.out.println("put entrance");
		this.gs.getMembershipList().setMaster();
		//if you are the master you need to shard the file and send a call to the
		//other servers to save the file
		this.gs.getMembershipList().setSuccessors();
		
		if(this.gs.getMembershipList().getMember(this.gs.getProcessId()).isMaster() && isFirstRun) {
			int size = Integer.parseInt(this.gs.getProps().getProperty("shardsize"));
			List<byte[]> files = DFSServer.shardFile(file, size);
			System.out.println("Sharded into " + files.size() + " files.");
			int replicationFactor = Integer.parseInt(this.gs.getProps().getProperty("replicationfactor"));
			//append a digit to the filename then save 1 copy on the correct node
			//then store another copy on the each nodes successor
			for(int i=0;i<files.size();i++) {
				String newFile = filename + i;
				String sentToProcess = this.gs.getSendToProcess(newFile);
				//System.out.println("rep factor " + replicationFactor + sentToProcess);
				
				//stores a copy on sentToProcess then 
				//stores an additional copy on sentToProcess successor
				for(int j=0;j<replicationFactor;j++) {
					DFSServerInterface dfsServer = null;
					//String rmiServer = "rmi://localhost/DFSServer";
					String rmiServer = getRMIHostname(sentToProcess);
					//System.out.println("saving file on " + rmiServer);
					try {
						dfsServer = (DFSServerInterface) Naming.lookup(rmiServer);
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					dfsServer.put(newFile, files.get(i), false);
					this.gs.getMembershipList().getMember(sentToProcess).getSuccessor();
				}
			}
		}
		if (!isFirstRun) {
			//System.out.println("saving file");
			//the master is telling you to save a file shard
			FileOutputStream fos;
			try {
				fos = new FileOutputStream(filename);
				fos.write(file);
				LoggerThread lt = new LoggerThread(this.gs.getProcessId(), "#PUT_FILE#" + filename);
				lt.start();	 
			} catch (FileNotFoundException e) {
				e.printStackTrace();
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		//System.out.println("put finished");
		return false;
	}
	
	//get the hostname
	private String getRMIHostname(String sentToProcess) {
		String ipAddress = this.gs.getMembershipList().getMember(sentToProcess).getIpAddress();
		if(ipAddress.equals("127.0.0.1") || ipAddress.equals("192.168.1.7")) {
			ipAddress = "localhost";
		}
		String rmiServer = "rmi://" + this.gs.getMembershipList().getMember(sentToProcess).getIpAddress()
				+ "/" + ipAddress
				+ this.gs.getMembershipList().getMember(sentToProcess).getPortNumber();
		return rmiServer;
	}

	@Override
	public byte[] get(String filename, boolean isFirstRun) throws RemoteException {
		byte[] result = null;
		//if you are the master you need to find all the file shards
		//and re-assemble them
		if(this.gs.getMembershipList().getMaster().equals(this.gs.getProcessId())) {
			//System.out.println("the master");
			this.gs.getMembershipList().getMember(this.gs.getProcessId()).setMaster(true);
		}
		else {
			//System.out.println("not the master");
			this.gs.getMembershipList().getMember(this.gs.getProcessId()).setMaster(false);
		}
		this.gs.getMembershipList().setSuccessors();
		//System.out.println(this.gs.getMembershipList().getMember(this.gs.getProcessId()).isMaster());
		if(this.gs.getMembershipList().getMember(this.gs.getProcessId()).isMaster() && isFirstRun) {
			byte[] fileToReturn = null;
			boolean isLastShard = false;
			int filesFound = 0;
			//don't know how many file fragments there are
			//loop through with i and increment by 1
			int i = 0;
			while(!isLastShard) {
				//calculate the hash of the filename and ask the appropriate
				//server if they have the filename, if not try the successor
				String fileToFind = filename + i;
				String potentialProcess = this.gs.getSendToProcess(fileToFind);
				
				//loop through all the potential processes so you can find the right file
				int replicationFactor = Integer.parseInt(gs.getProps().getProperty("replicationfactor"));
				for(int j=0;j<replicationFactor;j++) {
					DFSServerInterface dfsServer = null;
					String rmiServer = getRMIHostname(potentialProcess);
					try {
						dfsServer = (DFSServerInterface) Naming.lookup(rmiServer);
						if(dfsServer.fileExists(fileToFind) && filesFound == i) {
							filesFound++;
							fileToReturn = concatenateByte(fileToReturn, dfsServer.get(fileToFind, false));
							break;  //break the for loop we got our shard
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					potentialProcess = this.gs.getMembershipList().getMember(potentialProcess).getSuccessor();
				}
				//we should have found the file by now so if not 
				//it should be done
				if(filesFound == i) {
					isLastShard = true;  //breaks the while loop
				}
				i++;
			}
			
			//we now have all the pieces, sew them together
			result = fileToReturn;
			//System.out.println("get complete");
		}
		
		//if you are not the master just return the file you are asked to get
		if (!isFirstRun) {
			//System.out.println("getting file");
			Path path = Paths.get(filename);
			try {
				LoggerThread lt = new LoggerThread(this.gs.getProcessId(), "#GET_FILE#" + filename);
				lt.start();	 
				result = Files.readAllBytes(path);
				//System.out.println("here");
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return result;
	}

	@Override
	//
	public void delete(String filename, boolean isFirstRun) throws RemoteException {
		boolean isLastShard = false;
		int filesFound = 0;
		//if you are the master you need to find all the file shards
		//and re-assemble them
		if(this.gs.getMembershipList().getMaster().equals(this.gs.getProcessId())) {
			this.gs.getMembershipList().getMember(this.gs.getProcessId()).setMaster(true);
		}
		else {
			this.gs.getMembershipList().getMember(this.gs.getProcessId()).setMaster(false);
		}
		this.gs.getMembershipList().setSuccessors();
		//System.out.println(this.gs.getMembershipList().getMember(this.gs.getProcessId()).isMaster());
		if(this.gs.getMembershipList().getMember(this.gs.getProcessId()).isMaster() && isFirstRun) {
			//find all the files and delete them
			int i = 0;
			while(!isLastShard) {
				//calculate the hash of the filename and ask the appropriate
				//server if they have the filename, if not try the successor
				String fileToFind = filename + i;
				String potentialProcess = this.gs.getSendToProcess(fileToFind);
				boolean fileShardFound = false;
				
				//loop through all the potential processes so you can find the right file
				//int replicationFactor = Integer.parseInt(gs.getProps().getProperty("replicationfactor"));
				for(int j=0;j<this.gs.getMembershipList().getActiveKeys().size();j++) {
					DFSServerInterface dfsServer = null;
					String rmiServer = getRMIHostname(potentialProcess);
					try {
						dfsServer = (DFSServerInterface) Naming.lookup(rmiServer);
						if(dfsServer.fileExists(fileToFind)) {
							dfsServer.delete(fileToFind, false);
							if(!fileShardFound) {
								filesFound++;
								fileShardFound = true;
							}
						}
					} catch (MalformedURLException e) {
						e.printStackTrace();
					} catch (NotBoundException e) {
						e.printStackTrace();
					}
					potentialProcess = this.gs.getMembershipList().getMember(potentialProcess).getSuccessor();
				}
				//we should have found the file by now so if not 
				//it should be done
				if(filesFound == i) {
					isLastShard = true;  //breaks the while loop
				}
				i++;
			}
			
			
		}
		if (!isFirstRun){
			File file = new File(filename);
			file.delete();
		}
		//System.out.println("hello from delete");
	}
	
	@Override
	public boolean fileExists(String filename) throws RemoteException {
		boolean result = false;
		File f = new File(filename);
		result = f.exists() && !f.isDirectory();
		//System.out.println("hello from file exists");
		return result;
	}
	
	//splits file into file.size()/size chunks
	public static List<byte[]> shardFile(byte[] file, int size) {
	    List<byte[]> result = new ArrayList<byte[]>();
	    int start = 0;
	    while (start < file.length) {
	        int end = Math.min(file.length, start + size);
	        result.add(Arrays.copyOfRange(file, start, end));
	        start += size;
	    }
	    return result;
	}
	
	
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
