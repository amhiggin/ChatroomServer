package main.java;

import java.io.IOException;
import java.util.List;

/*
 * Thread which is created for every new client interaction.
 * Enables handling requests and responses in parallel.
 */

public class ClientThread extends Thread {

	private ClientNode clientNode;
	private int serverPort;
	private ClientRequest requestType;
	private List<String> receivedFromClient;

	public ClientThread(ClientNode client, ClientRequest requestType, List<String> receivedFromClient, int serverPort) {
		super();
		this.clientNode = client;
		this.serverPort = serverPort;
		this.requestType = requestType;
		this.receivedFromClient = receivedFromClient;
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
			handleError(e);
		}
	}

	private void handleError(Exception e) {
		ChatroomServer.handleError(e);
	}

	private Chatroom createChatroomAndAddToServerRecords() throws Exception {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId());
		return chatroom;
	}

	private void joinChatroom() throws Exception {
		String requestedChatroomToJoin = this.clientNode.getChatroomId();

		Chatroom requestedChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToJoin);
		if (this.clientNode.getJoinId() == null) {
			this.clientNode.setJoinId(ChatroomServer.clientId.getAndIncrement());
		}

		if (requestedChatroom != null) {
			requestedChatroom.addNewClientToChatroomAndNotifyMembers(clientNode);
			// update server records
			ChatroomServer.getActiveChatRooms().get(requestedChatroom).add(clientNode);
		} else {
			requestedChatroom = createChatroomAndAddToServerRecords();
			requestedChatroom.addNewClientToChatroomAndNotifyMembers(clientNode);
			// update server records
			ChatroomServer.getActiveChatRooms().put(requestedChatroom, requestedChatroom.getSetOfConnectedClients());
		}
		String responseToClient = String.format(ServerResponse.JOIN.getValue(), this.clientNode.getChatroomId(), 0,
				this.serverPort, this.clientNode.getChatroomId(), this.clientNode.getJoinId());
		writeResponseToClient(responseToClient);
		requestedChatroom.broadcastMessageInChatroom(
				String.format("A new client called %s has joined the chatroom!", clientNode.getName()));
	}

	private void leaveChatroom() throws Exception {
		/*
		 * LEAVE_CHATROOM: [ROOM_REF] JOIN_ID: [integer previously provided by
		 * server on join] CLIENT_NAME: [string Handle to identifier client
		 * user]
		 */
		String parsedRequestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom chatroomExists = ChatroomServer.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToLeave);
		if (chatroomExists != null) {
			// NOTE: don't need to remove client from server records
			chatroomExists.removeClientNodeAndInformOtherMembers(this.clientNode);
		}
		String responseToClient = String.format(ServerResponse.LEAVE.getValue(), this.clientNode.getChatroomId(),
				this.clientNode.getJoinId());
		writeResponseToClient(responseToClient);
	}

	private void sayHello() throws IOException {
		String response = String.format("HELO text\nIP:[%s]\nPort:[%s]\nStudentID:[%s]", Constants.SERVER_IP,
				this.serverPort, Constants.STUDENT_ID);
		writeResponseToClient(response);
	}

	private void chat() throws IOException {
		String message = this.receivedFromClient.get(3).split(":", 0)[1];
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(this.clientNode.getChatroomId());
		if (chatroomAlreadyOnRecord != null) {
			String responseToClient = String.format(ServerResponse.CHAT.getValue(),
					chatroomAlreadyOnRecord.getChatroomId(), this.clientNode.getJoinId(), this.clientNode.getName(),
					message);
			writeResponseToClient(responseToClient);
			chatroomAlreadyOnRecord.broadcastMessageInChatroom(
					String.format("Client %s has left the chatroom.", this.clientNode.getJoinId()));
		}
	}

	private void disconnectFromServer() throws Exception {
		// get the entry corresponding to the chatroom its in
		// then remove the chatroom from that entry
		String parsedRequestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToLeave);
		chatroomAlreadyOnRecord.removeClientNodeAndInformOtherMembers(clientNode);
		ChatroomServer.recordClientChangeWithServer(ClientRequest.DISCONNECT, clientNode);
	}

	private void writeResponseToClient(String response) throws IOException {
		this.clientNode.getConnection().getOutputStream().write(response.getBytes());
	}
}
