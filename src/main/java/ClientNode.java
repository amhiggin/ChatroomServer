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
	private Integer joinId;

	public ClientNode(Socket connection, String clientName, String chatroomId, Integer joinId) {
		this.connection = connection;
		this.clientName = clientName;
		this.chatroomId = chatroomId;
		this.joinId = joinId;
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

	public Integer getJoinId() {
		return this.joinId;
	}

	public void setJoinId(Integer joinId) {
		this.joinId = joinId;
	}

}
