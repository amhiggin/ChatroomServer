package main.java;

import java.io.IOException;

/*
 * Thread which is created for every new client interaction
 * Handles requests from clients and performs required responses.
 */

public class ClientThread extends Thread {

	private ClientNode clientNode;
	private int serverPort;
	private ClientRequest requestType;
	private String messageReceived;

	public ClientThread(ClientNode client, ClientRequest requestType, String message, int serverPort) {
		super();
		this.clientNode = client;
		this.serverPort = serverPort;
		this.requestType = requestType;
		this.messageReceived = message;
	}

	@Override
	public void run() {
		try {
			switch (this.requestType) {
			case JOIN_CHATROOM:
				joinChatroom();
				break;
			case HELO_TEXT:
				sayHello();
				break;
			case LEAVE_CHATROOM:
				leaveChatroom();
				break;
			case CHAT:
				chat();
				break;
			case DISCONNECT:
				disconnectFromServer();
				break;
			default:
				return;
			}
		} catch (Exception e) {
			handleError(e.getMessage());
		}
	}

	private void handleError(String exceptionMessage) {
		ChatroomServer.handleError(exceptionMessage);
	}

	private void createChatroomAndAddToServerRecords() throws Exception {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId());
		chatroom.addClientNodeAndNotifyMembersOfChatroom(clientNode);
		ChatroomServer.getActiveChatRooms().put(chatroom, chatroom.getListOfConnectedClients());
	}

	private void joinChatroom() throws Exception {
		// TODO correct the parsing using tests
		String[] contentsAfterRequestType = this.messageReceived.split(":");
		String[] restOfMessage = contentsAfterRequestType[1].split("\n");
		String parsedRequestedChatroomToJoin = this.clientNode.getChatroomId();

		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToJoin);
		if (chatroomAlreadyOnRecord != null) {
			chatroomAlreadyOnRecord.addClientNodeAndNotifyMembersOfChatroom(clientNode);
		} else {
			createChatroomAndAddToServerRecords();
		}
	}

	private void leaveChatroom() throws Exception {
		// TODO correct the parsing using tests
		String[] contentsAfterRequestType = this.messageReceived.split(":");
		String[] restOfMessage = contentsAfterRequestType[1].split("\n");
		String parsedRequestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom chatroomExists = ChatroomServer.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToLeave);
		if (chatroomExists != null) {
			chatroomExists.removeClientNodeAndInformOtherMembers(this.clientNode);
		}
	}

	private void sayHello() throws IOException {
		String response = String.format("HELO text\nIP:[%s]\nPort:[%s]\nStudentID:[%s]", Constants.SERVER_IP,
				this.serverPort, Constants.STUDENT_ID);
		// TODO review whether below is correct
		this.clientNode.getConnection().getOutputStream().write(response.getBytes());
	}

	private void chat() throws IOException {
		// TODO correct the parsing using tests
		String[] contentsAfterRequestType = this.messageReceived.split(":");
		String[] restOfMessage = contentsAfterRequestType[1].split("\n");
		String parsedRequestedChatroomToJoin = this.clientNode.getChatroomId();

		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToJoin);
		if (chatroomAlreadyOnRecord != null) {
			chatroomAlreadyOnRecord.broadcastMessageInChatroom(this.messageReceived);
		}
	}

	private void disconnectFromServer() throws Exception {
		// get the entry corresponding to the chatroom its in
		// then remove the chatroom from that entry
		String parsedRequestedChatroomToJoin = this.clientNode.getChatroomId();
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToJoin);
		chatroomAlreadyOnRecord.removeClientNodeAndInformOtherMembers(clientNode);
	}
}
