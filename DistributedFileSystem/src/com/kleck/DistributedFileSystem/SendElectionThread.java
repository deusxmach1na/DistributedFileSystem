package com.kleck.DistributedFileSystem;

public class SendElectionThread extends Thread {
	private ElectionMessage em;
	private GroupServer gs;
	
	public SendElectionThread (GroupServer gs, ElectionMessage em) {
		this.em = em;
		this.gs = gs;
	}
	
	public void run() {
		//send to the successor
		this.gs.updateSuccessors();
		MembershipListRow member = this.gs.getMembershipList().getMember(this.gs.getMembershipList().getMember(this.gs.getProcessId()).getSuccessor());
		GossipSendThread gst = new GossipSendThread(member.getIpAddress(), member.getPortNumber(), this.em);
		gst.start();
	}
}
