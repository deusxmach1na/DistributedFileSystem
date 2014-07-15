package com.kleck.DistributedFileSystem;

import java.io.Serializable;
import java.util.ArrayList;

public class FileOperationMessage implements Serializable {
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	private ArrayList<Long> processIdList;
	private String initiator;
	private boolean isCoordinator;
	private Long winningProcess;
	
	public FileOperationMessage(String processId, boolean isCoord) {
		this.processIdList = new ArrayList<Long>();
		this.setInitiator(processId);
		this.setCoordinator(isCoord);
		this.winningProcess = 1000007L;
	}
	
	public void appendToList(Long hashKey) {
		this.processIdList.add(hashKey);
	}
	
	public String getInitiator() {
		return initiator;
	}

	public void setInitiator(String initiator) {
		this.initiator = initiator;
	}

	public boolean isCoordinator() {
		return isCoordinator;
	}

	public void setCoordinator(boolean isCoordinator) {
		this.isCoordinator = isCoordinator;
	}

	public Long getWinningProcess() {
		return winningProcess;
	}

	public void setWinningProcess() {
		long result = 1000007L;
		//choose the process with the lowest hashkey
		for(Long hashkey:this.processIdList) {
			if(hashkey < result) {
				result = hashkey;
			}
		}
		
		this.winningProcess = result;
	}
	
	public String toString() {
		String result = "";
		for(Long key:this.processIdList) {
			result += key.toString() + ", ";
		}
		result = result + this.getInitiator() + this.getWinningProcess();
		return result;
	}
	
}
