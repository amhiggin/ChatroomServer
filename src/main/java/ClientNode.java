package main.java;

import java.util.List;

/*
 * An object for storing the temporary info about a client request.
 */

public class ClientNode implements Comparable<ClientNode> {

	private String clientName;
	private Integer joinId;
	private String chatroomId;
	private List<String> receivedFromClient;
	// requestType?
	// message received?

	public ClientNode(String clientName, String chatroomId, Integer joinId, List<String> receivedFromClient) {
		this.clientName = clientName;
		this.joinId = joinId;
		this.setChatroomId(chatroomId);
	}

	public String getName() {
		return this.clientName;
	}

	public List<String> getReceivedFromClient() {
		return this.receivedFromClient;
	}

	public Integer getJoinId() {
		return this.joinId;
	}

	public void setJoinId(Integer joinId) {
		this.joinId = joinId;
	}

	public String getChatroomId() {
		return chatroomId;
	}

	public void setChatroomId(String chatroomId) {
		this.chatroomId = chatroomId;
	}

	@Override
	public int compareTo(ClientNode o) {
		if (this.getJoinId() < o.getJoinId()) {
			return -1;
		} else if (this.getJoinId() > o.getJoinId()) {
			return 1;
		}
		return 0;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Client name: %s\n", this.getName()));
		builder.append(String.format("JoinId: %s\n", this.getJoinId()));
		return builder.toString();
	}
}
