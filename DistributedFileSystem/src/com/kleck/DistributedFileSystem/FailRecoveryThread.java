package com.kleck.DistributedFileSystem;

public class FailRecoveryThread extends Thread {
	private GroupServer gs;
	
	public FailRecoveryThread(GroupServer gs) {
		this.gs = gs;
	}
	
	public void run() {
		long currentTime = System.currentTimeMillis();
		this.gs.startRMIServer(this.gs.getDfsServer());
		this.gs.getMembershipList().setMaster();
		long masterTime = System.currentTimeMillis();
		System.out.println("***********************");
		System.out.println("** Master Selection took " + (masterTime - currentTime) + " milliseconds.**");
		System.out.println("***********************");
		this.gs.getMembershipList().setSuccessors();
		this.gs.replicateFiles();
		long runTime = System.currentTimeMillis();
		System.out.println("***********************");
		System.out.println("** Replication took " + (runTime - currentTime) + " milliseconds.**");
		System.out.println("***********************");
		
	}

}
