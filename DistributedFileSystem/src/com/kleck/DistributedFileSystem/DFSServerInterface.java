package com.kleck.DistributedFileSystem;

import java.rmi.Remote;
import java.rmi.RemoteException;

public interface DFSServerInterface extends Remote {
	
	//put 
	//get
	//delete
	public boolean put(String filename, byte[] file, boolean isFirstRun) throws RemoteException;
	
	public byte[] get(String filename, boolean isFirstRun) throws RemoteException;
	
	public void delete(String filename, boolean isFirstRun) throws RemoteException;
	
	public boolean fileExists(String filename) throws RemoteException;

}
