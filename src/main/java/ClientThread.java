package main.java;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/*
 * Thread which is created for every new client interaction.
 * Enables handling multiple requests and responses in parallel.
 */

public class ClientThread extends Thread {

	private static final int KILL_REQUEST_TIMEOUT_MILLIS = 10000;

	private static final String SPLIT_PATTERN = ": ";

	private static final String HELO_IDENTIFIER = "HELO ";

	private ClientNode clientNode;
	private int serverPort;
	private ClientRequest requestType;
	private List<String> receivedFromClient;

	public ClientThread(ClientNode client, ClientRequest requestType, List<String> receivedFromClient) {
		super();
		this.clientNode = client;
		this.requestType = requestType;
		this.receivedFromClient = receivedFromClient;
	}

	@Override
	public void run() {
		try {
			if (this.requestType == null) {
				handleError(Error.InvalidRequest);
				return;
			}
			switch (this.requestType) {
			case JOIN_CHATROOM:
				joinChatroom();
				break;
			case HELO:
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
			case KILL_SERVICE:
				killService();
				break;
			default:
				handleError(Error.InvalidRequest);
				return;
			}
			ChatroomServer.recordClientChangeWithServer(this.requestType, this.clientNode);
		} catch (Exception e) {
			// TODO figure out how to handle the generic exception
		}
	}

	private void killService() {
		ChatroomServer.setTerminateServer(new AtomicBoolean(true));
		try {
			// Assume after 10 seconds the request must have succeeded
			sleep(KILL_REQUEST_TIMEOUT_MILLIS);
		} catch (InterruptedException e) {
			; // Do nothing
		}
		if (!ChatroomServer.getServerSocket().isClosed()) {
			handleError(Error.KillService);
		}
	}

	private void handleError(Error errorMessage) {
		String errorResponse = String.format(ServerResponse.ERROR.getValue(), errorMessage.getValue(),
				errorMessage.getDescription());
		try {
			this.clientNode.getConnection().getOutputStream().write(errorResponse.getBytes());
		} catch (IOException e) {
			String temporaryErrorMessageHolder = errorResponse;
			errorResponse = "Failed to communicate failure response to client: " + temporaryErrorMessageHolder;
		}
		ChatroomServer.outputRequestErrorMessageToConsole(errorResponse, this.clientNode);
	}

	private Chatroom createChatroom() throws Exception {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId());
		return chatroom;
	}

	private void joinChatroom() {
		try {
			String requestedChatroomToJoin = this.clientNode.getChatroomId();

			Chatroom requestedChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToJoin);
			if (this.clientNode.getJoinId() == null) {
				this.clientNode.setJoinId(ChatroomServer.clientId.getAndIncrement());
			}

			if (requestedChatroom != null) {
				requestedChatroom.addNewClientToChatroomAndNotifyMembers(clientNode);
			} else {
				requestedChatroom = createChatroom();
				requestedChatroom.addNewClientToChatroomAndNotifyMembers(clientNode);
				// update server records
				ChatroomServer.getActiveChatRooms().add(requestedChatroom);
			}
			String responseToClient = String.format(ServerResponse.JOIN.getValue(), this.clientNode.getChatroomId(), 0,
					this.serverPort, this.clientNode.getChatroomId(), this.clientNode.getJoinId());
			writeResponseToClient(responseToClient);
			requestedChatroom.broadcastMessageInChatroom(
					String.format("A new client called %s has joined the chatroom!", clientNode.getName()));
		} catch (Exception e) {
			handleError(Error.JoinChatroom);
		}
	}

	private void leaveChatroom() {
		/*
		 * LEAVE_CHATROOM: [ROOM_REF] JOIN_ID: [integer previously provided by
		 * server on join] CLIENT_NAME: [string Handle to identifier client
		 * user]
		 */
		String requestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom existingChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToLeave);
		try {
			if (existingChatroom != null) {
				// NOTE: don't need to remove client from server records
				existingChatroom.removeClientNodeAndInformOtherMembers(this.clientNode);
			}
			String responseToClient = String.format(ServerResponse.LEAVE.getValue(), this.clientNode.getChatroomId(),
					this.clientNode.getJoinId());
			writeResponseToClient(responseToClient);
		} catch (Exception e) {
			handleError(Error.LeaveChatroom);
		}
	}

	private void sayHello() {
		try {
			String response = constructHelloResponse(this.receivedFromClient);
			writeResponseToClient(response);
		} catch (Exception e) {
			handleError(Error.Helo);
		}
	}

	private String constructHelloResponse(List<String> receivedFromClient2) {
		return String.format(ServerResponse.HELO.getValue(),
				this.receivedFromClient.get(0).split(HELO_IDENTIFIER)[1].replaceAll("\n", ""), Constants.SERVER_IP,
				this.serverPort, Constants.STUDENT_ID);
	}

	private void chat() throws IOException {
		String message = this.receivedFromClient.get(3).split(SPLIT_PATTERN, 0)[1];
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(this.clientNode.getChatroomId());
		if (chatroomAlreadyOnRecord != null) {
			String responseToClient = String.format(ServerResponse.CHAT.getValue(),
					chatroomAlreadyOnRecord.getChatroomId(), this.clientNode.getJoinId(), this.clientNode.getName(),
					message);
			chatroomAlreadyOnRecord.broadcastMessageInChatroom(responseToClient);
			return;
		}
		handleError(Error.Chat);
	}

	private void disconnectFromServer() throws Exception {
		String parsedRequestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToLeave);
		chatroomAlreadyOnRecord.removeClientNodeAndInformOtherMembers(clientNode);
	}

	private void writeResponseToClient(String response) throws IOException {
		this.clientNode.getConnection().getOutputStream().write(response.getBytes());
	}
}
