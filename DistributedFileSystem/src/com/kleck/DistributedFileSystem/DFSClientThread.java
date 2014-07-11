package com.kleck.DistributedFileSystem;

import java.io.File;

public class DFSClientThread extends Thread {
	private String command;
	private String host;
	private int port;
	
	public DFSClientThread(String host, int port, String command) {
		this.command = command;
		this.host = host;
		this.port = port;
	}
	
	public void run() {
		//this.dfsServer = (DFSServerInterface) Naming.lookup(rmiMaster);
	}
	
	private boolean checkLocalFilename(String filePath) {
		File f = new File(filePath);
		boolean result = f.exists() && !f.isDirectory();
		return result;
	}	

}
