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
				handleRequestProcessingError(Error.InvalidRequest);
				return;
			}
			if (this.clientNode == null) {
				throw new ClientNodeUndefinedException("Null client node");
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
				handleRequestProcessingError(Error.InvalidRequest);
			}
			ChatroomServer.recordClientChangeWithServer(this.requestType, this.clientNode);
		} catch (Exception e) {
			ChatroomServer.outputServiceErrorMessageToConsole(String.format("%s", e));
			e.printStackTrace(); // TODO @Amber remove later
		}
	}

	private void killService() {
		ChatroomServer
				.printMessageToConsole(String.format("Client %s requested to kill service", this.clientNode.getName()));
		ChatroomServer.setTerminateServer(new AtomicBoolean(true));
		try {
			// Assume after 10 seconds the request must have succeeded
			sleep(KILL_REQUEST_TIMEOUT_MILLIS);
		} catch (InterruptedException e) {
			; // Do nothing
		}
		if (!ChatroomServer.getServerSocket().isClosed()) {
			handleRequestProcessingError(Error.KillService);
		}
	}

	private void handleRequestProcessingError(Error errorMessage) {
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
		ChatroomServer.printMessageToConsole(String.format("Created new chatroom %s", chatroom.getChatroomId()));
		return chatroom;
	}

	private void joinChatroom() {
		ChatroomServer.printMessageToConsole(String.format("Client %s joining chatroom %s", this.clientNode.getName(),
				this.clientNode.getChatroomId()));
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
				ChatroomServer.printMessageToConsole(String.format("Added client %s to chatroom %s",
						this.clientNode.getName(), requestedChatroom.getChatroomId()));
			}
			String responseToClient = String.format(ServerResponse.JOIN.getValue(), this.clientNode.getChatroomId(), 0,
					this.serverPort, this.clientNode.getChatroomId(), this.clientNode.getJoinId());
			writeResponseToClient(responseToClient);
			requestedChatroom.broadcastMessageInChatroom(
					String.format("A new client called %s has joined the chatroom!", clientNode.getName()));
		} catch (Exception e) {
			handleRequestProcessingError(Error.JoinChatroom);
		}
	}

	private void leaveChatroom() {
		/*
		 * LEAVE_CHATROOM: [ROOM_REF] JOIN_ID: [integer previously provided by
		 * server on join] CLIENT_NAME: [string Handle to identifier client
		 * user]
		 */
		ChatroomServer.printMessageToConsole(String.format("Client %s leaving chatroom %s", this.clientNode.getName(),
				this.clientNode.getChatroomId()));
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
			handleRequestProcessingError(Error.LeaveChatroom);
		}
	}

	private void sayHello() {
		try {
			String response = constructHelloResponse(this.receivedFromClient);
			writeResponseToClient(response);
		} catch (Exception e) {
			handleRequestProcessingError(Error.Helo);
		}
	}

	private String constructHelloResponse(List<String> receivedFromClient2) {
		String helloResponse = String.format(ServerResponse.HELO.getValue(),
				this.receivedFromClient.get(0).split(HELO_IDENTIFIER)[1].replaceAll("\n", ""),
				ChatroomServer.getServerSocket().getInetAddress(), this.serverPort, Constants.STUDENT_ID);
		ChatroomServer.printMessageToConsole(String.format("Sent %s ", helloResponse));
		return helloResponse;
	}

	private void chat() throws IOException {
		ChatroomServer.printMessageToConsole(String.format("Client %s chatting in chatroom %s",
				this.clientNode.getName(), this.clientNode.getChatroomId()));
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
		handleRequestProcessingError(Error.Chat);
	}

	private void disconnectFromServer() throws Exception {
		ChatroomServer.printMessageToConsole(
				String.format("Client %s disconnecting from server ", this.clientNode.getName()));
		String parsedRequestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomIfExists(parsedRequestedChatroomToLeave);
		chatroomAlreadyOnRecord.removeClientNodeAndInformOtherMembers(clientNode);
	}

	private void writeResponseToClient(String response) throws IOException {
		this.clientNode.getConnection().getOutputStream().write(response.getBytes());
		ChatroomServer.printMessageToConsole(String.format("Wrote %s response to client %s",
				this.requestType.getValue(), this.clientNode.getName()));
	}
}
