package com.kleck.DistributedFileSystem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigInteger;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.UnknownHostException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.ExportException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Properties;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;


public class GroupServer extends Thread {
	private boolean isContact;
	private String processId;
	private String ipAddress;
	private MembershipList ml;
	private int bytesused;
	private long runtime;
	private long starttime;
	private int portNumber;
	private Properties props;
	private DFSServer dfsServer;

	public GroupServer(int portNumber, boolean isContact) {
		//start server
		this.portNumber = portNumber;
		this.isContact = isContact;
	}
	
	public void run() {
		this.ml = new MembershipList();
		this.props = loadParams();
		this.bytesused = 0;
		this.runtime = 0;
		this.starttime = System.currentTimeMillis();
		
		try {
			this.ipAddress = InetAddress.getLocalHost().getHostAddress();
		} catch (UnknownHostException e) {
			System.out.println("Could not get host address");
			e.printStackTrace();
		}
		this.processId = Long.toString(starttime) + "-" + this.ipAddress; 
		
		//add yourself to your membership list
		ml.addNewMember(processId, this.portNumber, this.isContact);
		
		//start listening for gossip
		GossipListenThread glt = new GossipListenThread(this.portNumber, this);
		glt.start();
		
		//send data to contact server
		sendToContactServer();
		/*
		if(!this.isContact) {
			ElectionMessage em = new ElectionMessage(this.getProcessId(), false);
			SendElectionThread set = new SendElectionThread(this, em);
			set.start();
		}
		*/
		

		//schedule threads to run
		ScheduledExecutorService scheduledThreadPool = Executors.newScheduledThreadPool(3);
		scheduledThreadPool.scheduleAtFixedRate(new UpdateHeartbeatThread(this), 0, new Long(this.props.getProperty("timeUpdateHeartbeat")), TimeUnit.MILLISECONDS);
		scheduledThreadPool.scheduleAtFixedRate(new RandomGossipThread(this), 0, new Long(this.props.getProperty("timeGossip")), TimeUnit.MILLISECONDS);
		scheduledThreadPool.scheduleAtFixedRate(new UpdateMembershipListThread(this, new Long(this.props.getProperty("timeFail"))), 0, new Long(this.props.getProperty("timeUpdateList")), TimeUnit.MILLISECONDS);
		//scheduledThreadPool.scheduleAtFixedRate(new GossipSendThread(contactHostname, contactPortNumber, this), 0, 15, TimeUnit.SECONDS);
		
		
		//allow user to simulate a fail/stop by typing in stop
		BufferedReader inFromUser = new BufferedReader(new InputStreamReader(System.in));
		while(true) {
			try {
				//System.out.println(">>");
				String command = inFromUser.readLine();
				if(command.equals("stop")) {
					System.out.println("Stopping Server");					
					
					//clean up threads
					scheduledThreadPool.shutdown();
					
					this.ml.getMember(this.processId).setHasLeft(true);
					UpdateHeartbeatThread lastHeartbeat = new UpdateHeartbeatThread(this);
					RandomGossipThread lastGossip = new RandomGossipThread(this);
					lastGossip.start();
					lastHeartbeat.run();
					
					//clean up everything else
					//scheduledThreadPool.shutdown();
					glt.setStop(true);
					glt.getServer().close();
					inFromUser.close();
					break;
				}
				if(command.equals("print")) {
					System.out.println("Membership List for " + this.getProcessId());					
					
					//clean up threads
					System.out.println(this.getMembershipList().toString());
				}
				
			} catch (IOException e) {
				System.out.println("could not get input from user");
				e.printStackTrace();
			}
		}
		
	}

	/*
	//main method
	//get args and spin up the Logging Server
	public static void main (String args[]) {
		int port = 6667;
		try {
			if(args.length >= 1) {
				port = Integer.parseInt(args[0]);
			}		
		}
		catch (NumberFormatException nfe) {
			port = 6667;
			System.out.println("Using default port " + port + " for gossip communication.");
		}
			
		
		//change port and server if args are passed
		if(args.length == 2) {
			if(!args[1].equals("false")) {
				System.out.println("Starting Contact Server");
				new GroupServer(port, true);
			}
			else {
				System.out.println("Starting Server.");
				new GroupServer(port, false);
			}		
		}
		else {
			System.out.println("Starting Server.");
			new GroupServer(port, false);
		}	
	}
	*/
	
	//send your info to the contact server
	public void sendToContactServer() {
		//contact the contact server to add yourself
		String contactHostname = props.getProperty("contactserver").split(",")[0];
		int contactPortNumber = Integer.parseInt(props.getProperty("contactserver").split(",")[1]);
		if(!this.isContact) {
			GossipSendThread gst = new GossipSendThread(contactHostname, contactPortNumber, this.getMembershipList());
			gst.start();
		}
	}
	
	//set successors
	public void updateSuccessors() {
		this.getMembershipList().setSuccessors();
	}
	
	//may use this function to hash file names later
	public static long getHashValue (String processId) {
		MessageDigest md = null;
		try {
			md = MessageDigest.getInstance("md5");
		} catch (NoSuchAlgorithmException e) {
			System.out.println("could not find algorithm");
			//e.printStackTrace();
		}
		BigInteger hashValue = new BigInteger(md.digest(processId.getBytes()));
		long finalHash = hashValue.longValue() % 1000007L;
		return Math.abs(finalHash);
		
	}

	public String getSendToProcess(String newFile) {
		long hashedFile = getHashValue(newFile);
		String result = "";
		long max = 1000007L;
		long min = 1000007L;
		
		for(String key:this.getMembershipList().getActiveKeys()) {
			if(hashedFile < this.getMembershipList().getMember(key).getHashKey() && this.getMembershipList().getMember(key).getHashKey() < max) {
				result = this.getMembershipList().getMember(key).getProcessId();
				max = this.getMembershipList().getMember(key).getHashKey();
			}
			if(this.getMembershipList().getMember(key).getHashKey() < min) {
				min = this.getMembershipList().getMember(key).getHashKey();
			}
		}
		
		//if max has not changed then the hash key was greater than all the hashkeys
		if(max == 1000007L) {
			result = this.getMembershipList().getProcessIdByHash(min);
		}
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
	    	System.out.println("Did not find hostname file. Ensure it is in the same folder as the jar.");
	    }
	    
	    return props;
	}
	
	//get set MembershipList
	public MembershipList getMembershipList() {
		return ml;
	}

	public void setMembershipList(MembershipList ml) {
		this.ml = ml;
	}
	
	public boolean isContact() {
		return this.isContact;
	}
	
	public String getProcessId() {
		return this.processId;
	}
	
	public int getBytesUsed() {
		return bytesused;
	}


	public void updateBytesUsed(int bytesused) {
		this.bytesused += bytesused;
	}


	public void updateRunTime() {
		this.runtime = System.currentTimeMillis() - this.starttime;
		System.out.println("*********************");
		System.out.println("**BANDWIDTH = " + this.bytesused * 1.000 / (this.runtime / 1000));
		System.out.println("*********************");
		System.out.println(this.bytesused);
		System.out.println(this.runtime);
	}

	public Properties getProps() {
		return props;
	}

	public void startRMIServer(DFSServer dfsServer) {
		this.setDfsServer(dfsServer);
		try {
			//create registry and start server
			System.setProperty("java.rmi.server.hostname", "localhost");
			try {
				LocateRegistry.createRegistry(1099);
			}
			catch (ExportException e) {
				LocateRegistry.getRegistry(1099);
				//e.printStackTrace();
			}
			//new GroupServer(port, isContact);
			//rmi host will be something like "rmi://localhost/<IPAddress><Port>";
			while(this.getMembershipList().size() < 1) {
				System.out.println("...");
			}
			//System.out.println(this.gs.getMembershipList().size() + " entries in ML");
			String rmiHostname = this.getRMIHostname(this.getProcessId()).replace("rmi://" + 
					this.getMembershipList().getMember(this.getProcessId()).getIpAddress()
					+ "/", "");
			Naming.rebind(rmiHostname, dfsServer);
			//System.out.println("rmi created" + rmiHostname);	
		}
		catch(RemoteException re) {
			re.printStackTrace();
		}
		catch(MalformedURLException e) {
			System.out.println("Could not create server in registry");
			e.printStackTrace();
		}
		/*
		catch (AlreadyBoundException e) {
			//Naming.rebind("DFSServer", dfs);
			e.printStackTrace();
		}
		*/
	}
	
	//get the hostname
	private String getRMIHostname(String sentToProcess) {
		String ipAddress = this.getMembershipList().getMember(sentToProcess).getIpAddress();
		if(ipAddress.equals("127.0.0.1") || ipAddress.equals("192.168.1.7")) {
			ipAddress = "localhost";
		}
		String rmiServer = "rmi://" + this.getMembershipList().getMember(sentToProcess).getIpAddress()
				+ "/" + ipAddress
				+ this.getMembershipList().getMember(sentToProcess).getPortNumber();
		return rmiServer;
	}

	public DFSServer getDfsServer() {
		return dfsServer;
	}

	public void setDfsServer(DFSServer dfsServer) {
		this.dfsServer = dfsServer;
	}
}
