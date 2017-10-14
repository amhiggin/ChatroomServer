package main.java;

import java.net.Socket;

public class ClientNode {

	private Socket connection;
	private String clientName;
	private int chatroomId;

	public ClientNode(Socket connection, String clientName, int chatroomId) {
		this.connection = connection;
		this.clientName = clientName;
		this.chatroomId = chatroomId;
	}

	public String getName() {
		return this.clientName;
	}

	public Socket getConnection() {
		return this.connection;
	}

	public int getChatroomId() {
		return this.chatroomId;
	}

}
