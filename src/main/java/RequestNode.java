package main.java;

import java.util.List;

/*
 * An object for storing the temporary info about a client request.
 */

public class RequestNode {

	private String clientName;
	private String chatroomId;
	private List<String> receivedFromClient;
	private ClientRequest requestType;

	public RequestNode(String clientName, String chatroomId, List<String> receivedFromClient,
			ClientRequest requestType) {
		this.clientName = clientName;
		this.setChatroomId(chatroomId);
		this.receivedFromClient = receivedFromClient;
		this.setRequestType(requestType);
	}

	public String getName() {
		return this.clientName;
	}

	public List<String> getReceivedFromClient() {
		return this.receivedFromClient;
	}

	public String getChatroomRequested() {
		return chatroomId;
	}

	public void setChatroomId(String chatroomId) {
		this.chatroomId = chatroomId;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append(String.format("Client name: %s\n", this.getName()));
		return builder.toString();
	}

	public ClientRequest getRequestType() {
		return requestType;
	}

	public void setRequestType(ClientRequest requestType) {
		this.requestType = requestType;
	}
}
