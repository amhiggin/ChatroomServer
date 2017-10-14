package main.java;

import java.io.IOException;

public class ClientThread extends Thread {

	private ClientNode clientNode;
	private int serverPort;
	private Request requestType;
	private String messageReceived;

	public ClientThread(ClientNode client, Request requestType, String message, int serverPort) {
		this.clientNode = client;
		this.serverPort = serverPort;
		this.requestType = requestType;
		this.messageReceived = message;
		// At this point we need to know what chatroom we are going to talk to
		// So this needs to be done on the server side
		// Possible to have a null chatroom... use Integer and not int
	}

	@Override
	public void run() {
		try {
			switch (requestType) {
			case JoinChatroom:
				joinChatroom();
				break;
			case HelloText:
				sayHello();
				break;
			case LeaveChatroom:
				leaveChatroom();
				break;
			case Chat:
				chat();
				break;
			case Disconnect:
				disconnectFromServer();
				break;
			default:
				return;
			}
		} catch (Exception e) {
			handleError();
		}
	}

	private void handleError() {
		// TODO Implement using the Error enum

	}

	private Chatroom createChatroom() throws Exception {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId());
		chatroom.addClientNode(clientNode);
		return chatroom;
	}

	private void joinChatroom() throws Exception {
		String[] contentsAfterRequestType = this.messageReceived.split(":");
		String[] restOfMessage = contentsAfterRequestType[1].split("\n");
		int parsedRequestedChatroomToJoin = 0; // TODO parse this properly
		Chatroom chatroomAlreadyOnRecord = ChatroomServer.doesChatroomAlreadyExistByReference(parsedRequestedChatroomToJoin);
		if (chatroomAlreadyOnRecord != null) {
			chatroomAlreadyOnRecord.addClientNode(clientNode);
		} else {
			createChatroom();
		}

		// identify which chatroom we want to join
		// chatroom.addClientNode(clientNode);
		// update the server with what happened too
	}

	private void leaveChatroom() {
		// TODO @amber
		// Figure out how to identify the chatroom the client is in
		// Then call the removeNodeFromChatroom method
	}

	private void sayHello() throws IOException {
		String response = String.format("HELO text\nIP:[%s]\nPort:[%s]\nStudentID:[%s]", Constants.SERVER_IP,
				this.serverPort, Constants.STUDENT_ID);
		// TODO review whether below is correct
		clientNode.getConnection().getOutputStream().write(response.getBytes());
	}

	private void chat() {
		// TODO implement
	}

	private void disconnectFromServer() {
		// TODO Auto-generated method stub

	}
}
