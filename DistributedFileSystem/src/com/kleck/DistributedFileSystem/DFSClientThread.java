package com.kleck.DistributedFileSystem;


import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.EOFException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.Socket;

public class DFSClientThread extends Thread {
	private String ipAddress;
	private int portNumber;
	private String commandType;
	private byte[] data;
	private String fileToSaveAs;
	
	
	public DFSClientThread(String ipAddress, int portNumber, String command, byte[] data) {
		this.ipAddress = ipAddress;
		this.portNumber = portNumber;
		this.commandType = command.split(" ")[0];
		this.fileToSaveAs = command.split(" ")[1];
		this.data = data;
	}

	public void run() {		
		
		//open a socket to the server and send data
		try {
			//get the user command (put, get, delete)		
			if(this.commandType.equals("put")) {
				//System.out.println("issuing put to server");
				Socket dlSocket = new Socket(this.ipAddress, this.portNumber);
				OutputStream out = dlSocket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				dos.writeInt(this.data.length);
				dos.write(this.data);
				dos.flush();
				
				InputStream in = dlSocket.getInputStream();
				DataInputStream dis = new DataInputStream(in);
				int len = dis.readInt();
			    byte[] data = new byte[len];
			    if (len > 0) {
			        dis.readFully(data);
			    }
				//System.out.println(new String(data));
				dlSocket.close();
			}
			if(this.commandType.equals("get")) {
				
				//go get the files we need
				Socket dlSocket = new Socket(this.ipAddress, this.portNumber);
				OutputStream out = dlSocket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				dos.writeInt(this.data.length);
				dos.write(this.data);
				dos.flush();
				
				InputStream in = dlSocket.getInputStream();
				DataInputStream dis = new DataInputStream(in);
				int len = dis.readInt();
			    byte[] data = new byte[len];
			    if (len > 0) {
			        dis.readFully(data);
			    }
				FileOutputStream fos = new FileOutputStream("FromDFS/" + this.fileToSaveAs);
				fos.write(data);
				fos.close();
				dlSocket.close();
			}
			if(this.commandType.equals("del")) {
				//System.out.println("issuing del to server");
				Socket dlSocket = new Socket(this.ipAddress, this.portNumber);
				OutputStream out = dlSocket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				dos.writeInt(this.data.length);
				dos.write(this.data);
				dos.flush();
				
				InputStream in = dlSocket.getInputStream();
				DataInputStream dis = new DataInputStream(in);
				int len = dis.readInt();
			    byte[] data = new byte[len];
			    if (len > 0) {
			        dis.readFully(data);
			    }
				//System.out.println(new String(data));
				dlSocket.close();
			}
			if(this.commandType.equals("reb")) {
				//System.out.println("issuing del to server");
				Socket dlSocket = new Socket(this.ipAddress, this.portNumber);
				OutputStream out = dlSocket.getOutputStream();
				DataOutputStream dos = new DataOutputStream(out);
				dos.writeInt(this.data.length);
				dos.write(this.data);
				dos.flush();
				
				InputStream in = dlSocket.getInputStream();
				DataInputStream dis = new DataInputStream(in);
				int len = dis.readInt();
			    byte[] data = new byte[len];
			    if (len > 0) {
			        dis.readFully(data);
			    }
				//System.out.println(new String(data));
				dlSocket.close();
			}
			/*
			else if(command.split(" ")[0].equals("delete")) {
				this.dfsServer.delete(command.split(" ")[1], true);
			}		
			*/
		} catch (ConnectException e) {
			//issue connecting to server assume it failed
			//e.printStackTrace();	
			Thread.currentThread().interrupt();
			return;	
		} catch (EOFException e) {
			//issue connecting to server assume it failed
			//e.printStackTrace();	
			//if(this.commandType.equals("get")) {
			//	System.out.println("DFS file not found.");
			//}
			Thread.currentThread().interrupt();
			return;	
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
}
