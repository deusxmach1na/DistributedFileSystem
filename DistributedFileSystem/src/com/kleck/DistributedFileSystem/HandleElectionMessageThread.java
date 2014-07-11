package com.kleck.DistributedFileSystem;

public class HandleElectionMessageThread extends Thread{
	private ElectionMessage em;
	private GroupServer gs;
	
	public HandleElectionMessageThread(ElectionMessage em, GroupServer gs) {
		this.em = em;
		this.gs = gs;
	}
	
	public void start() {
		boolean forward = true;
		//System.out.println(this.em.toString());
		//if this is an election message
		if(!em.isCoordinator()) {
			//if this is the person that sent it then decide on the winner
			if(em.getInitiator().equals(this.gs.getProcessId())) {
				//decide on winner 
				//send coordinator message
				this.em.setWinningProcess();
				this.em.setCoordinator(true);
			}
			//else add your hashkey
			else {
				this.em.appendToList(this.gs.getMembershipList().getMember(this.gs.getProcessId()).getHashKey());
			}
		}
		//else if it is a coordinator message
		else {
			//System.out.println("here");
			//if this is the person that sent it then make sure the winner is still responding
			if(this.em.getInitiator() == this.gs.getProcessId()) {
				//restart election if the node is deletable
				if(this.gs.getMembershipList().getMember(this.gs.getMembershipList().getProcessIdByHash(this.em.getWinningProcess())).isDeletable()) {
					ElectionMessage em = new ElectionMessage(this.gs.getProcessId(), false);
					SendElectionThread set = new SendElectionThread(this.gs, em);
					set.start();
				}
				else {
					this.gs.getMembershipList().setMaster(this.em.getWinningProcess());
				}
				forward = false;
			}
			//else set coordinator
			else {
				this.gs.getMembershipList().setMaster(this.em.getWinningProcess());
			}
		}
		
		//if the initiator has a master then don't forward
		if(forward) {
			SendElectionThread set = new SendElectionThread(this.gs, this.em);
			set.start();
		}
	}
	
}
