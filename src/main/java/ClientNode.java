package main.java;

import java.net.Socket;

/*
 * An object for storing the info about a client connection.
 * Assumed that a client may only be a member of 1 chatroom at a time.
 * 
 */

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
