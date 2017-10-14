package main.java;

import java.net.Socket;

public class ClientNode {

	private Socket connection;
	private String clientName;
	private String chatroomId;

	public ClientNode(Socket connection, String clientName, String chatroomId) {
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

	public String getChatroomId() {
		return this.chatroomId;
	}

}
