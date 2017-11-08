package main.java;

import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.List;

import org.joda.time.LocalDateTime;

/*
 * Thread which is created for every new client interaction.
 * Enables handling multiple requests and responses in parallel.
 */

public class ClientThread extends Thread {

	private static final int KILL_REQUEST_TIMEOUT_MILLIS = 10000;
	private static final String SPLIT_PATTERN = ": ";
	private static final String HELO_IDENTIFIER = "HELO ";

	private ClientNode clientNode;
	private ClientRequest requestType;
	private List<String> receivedFromClient;
	private OutputStreamWriter clientOutputStreamWriter;

	public ClientThread(ClientNode client, ClientRequest requestType, List<String> receivedFromClient) {
		printThreadMessageToConsole("spawning new client thread...");
		this.clientNode = client;
		try {
			this.clientOutputStreamWriter = new OutputStreamWriter(this.clientNode.getConnection().getOutputStream());
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		this.requestType = requestType;
		this.receivedFromClient = receivedFromClient;
		printThreadMessageToConsole("Done spawning new thread");
	}

	@Override
	public void run() {
		printThreadMessageToConsole("In run method");
		try {
			if (this.requestType == null) {
				handleRequestProcessingError(Error.InvalidRequest);
				return;
			}
			if (this.clientNode == null) {
				ChatroomServer.outputServiceErrorMessageToConsole("Null client node");
				return;
			}
			switch (this.requestType) {
			case JOIN_CHATROOM:
				joinChatroom();
				printThreadMessageToConsole("Going to execute break now");
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
				return;
			}
			printThreadMessageToConsole("Exited the switch statement");
			ChatroomServer.recordClientChangeWithServer(this.requestType, this.clientNode);
			printThreadMessageToConsole("Finished running thread");
			return;
		} catch (Exception e) {
			e.printStackTrace(); // TODO @Amber remove later
			ChatroomServer.outputServiceErrorMessageToConsole(String.format("%s", e));
		}
	}

	private void killService() {
		ChatroomServer.printServerMessageToConsole(
				String.format("Client %s requested to kill service", this.clientNode.getName()));
		ChatroomServer.setRunning(false);
		try {
			// Assume after 10 seconds the request must have succeeded
			sleep(KILL_REQUEST_TIMEOUT_MILLIS);
		} catch (InterruptedException e) {
			e.printStackTrace();
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
			errorResponse = "Failed to communicate failure response to client: " + temporaryErrorMessageHolder
					+ ". Exception thrown: " + e.getMessage();
			e.printStackTrace();
		}
		printThreadMessageToConsole(errorResponse);
	}

	private Chatroom createChatroom() {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId(), ChatroomServer.nextChatroomId);
		printThreadMessageToConsole(String.format("Created new chatroom %s", chatroom.getChatroomId()));
		return chatroom;
	}

	private void joinChatroom() {
		printThreadMessageToConsole(String.format("Client %s joining chatroom %s", this.clientNode.getName(),
				this.clientNode.getChatroomId()));
		try {
			String requestedChatroomToJoin = this.clientNode.getChatroomId();
			Chatroom requestedChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToJoin);
			if (this.clientNode.getJoinId() == null) {
				this.clientNode.setJoinId(ChatroomServer.nextClientId.getAndIncrement());
			}

			if (requestedChatroom != null) {
				printThreadMessageToConsole(String.format("Chatroom %s already exists.. Will add client %s",
						requestedChatroom.getChatroomId(), this.clientNode.getName()));
				try {
					requestedChatroom.addNewClientToChatroom(clientNode);
				} catch (Exception e) {
					e.printStackTrace();
					printThreadMessageToConsole(String.format("%s was already a member of %s - resending JOIN response",
							this.clientNode, requestedChatroom.getChatroomId()));
					writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom);
					return;
				}
			} else {
				requestedChatroom = createChatroom();
				printThreadMessageToConsole(
						String.format("Chatroom %s was created!", requestedChatroom.getChatroomId()));
				// update server records
				requestedChatroom.addNewClientToChatroom(clientNode);
				ChatroomServer.getActiveChatRooms().add(requestedChatroom);
			}
			printThreadMessageToConsole(String.format("Sending join response to client %s", this.clientNode.getName()));
			writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.JoinChatroom);
		}
		printThreadMessageToConsole("Finished in join method");

	}

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private void printThreadMessageToConsole(String message) {
		System.out.println(String.format("%s>> THREAD%s: %s", getCurrentDateTime(), this.getId(), message));

	}

	private void writeJoinResponseToClientAndBroadcastMessageInChatroom(Chatroom requestedChatroom) {
		String responseToClient = String.format(ServerResponse.JOIN.getValue(), this.clientNode.getChatroomId(),
				ChatroomServer.serverIP, ChatroomServer.serverPort, requestedChatroom.getChatroomRef(),
				this.clientNode.getJoinId());
		writeResponseToClient(responseToClient);

		// Broadcast message in chatroom
		String clientJoinedChatroomMessage = String.format("%s has joined this chatroom\n\n",
				this.clientNode.getName());
		String chatMessage = String.format(ServerResponse.CHAT.getValue(), requestedChatroom.getChatroomRef(),
				this.clientNode.getName(), clientJoinedChatroomMessage);
		requestedChatroom.broadcastMessageInChatroom(chatMessage);
	}

	private void leaveChatroom() {
		/*
		 * LEAVE_CHATROOM: [ROOM_REF] JOIN_ID: [integer previously provided by
		 * server on join] CLIENT_NAME: [string Handle to identifier client
		 * user]
		 */
		printThreadMessageToConsole(String.format("Client %s leaving chatroom %s", this.clientNode.getName(),
				this.clientNode.getChatroomId()));
		String requestedChatroomToLeave = this.clientNode.getChatroomId();
		Chatroom existingChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToLeave);
		try {
			if (existingChatroom != null) {
				// NOTE: don't need to remove client from server records
				existingChatroom.removeClientNode(this.clientNode);
			}
			String responseToClient = String.format(ServerResponse.LEAVE.getValue(), existingChatroom.getChatroomRef(),
					this.clientNode.getJoinId());
			writeResponseToClient(responseToClient);
			String clientLeftChatroomMessage = String.format("%s has left this chatroom\n\n", clientNode.getName());
			// must create chat message
			String chatMessage = String.format(ServerResponse.CHAT.getValue(), existingChatroom.getChatroomRef(),
					this.clientNode.getName(), clientLeftChatroomMessage);
			existingChatroom.broadcastMessageInChatroom(chatMessage);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.LeaveChatroom);
		}
	}

	private void sayHello() {
		printThreadMessageToConsole("Going to say hello!");
		try {
			String response = constructHelloResponse(this.receivedFromClient);
			writeResponseToClient(response);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.Helo);
		}
	}

	private String constructHelloResponse(List<String> receivedFromClient) {
		String helloResponse = String.format(ServerResponse.HELO.getValue(),
				this.receivedFromClient.get(0).split(HELO_IDENTIFIER)[1].replaceAll("\n", ""), ChatroomServer.serverIP,
				ChatroomServer.serverPort, Constants.STUDENT_ID);
		return helloResponse;
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
		printThreadMessageToConsole(String.format("Client %s chatting in chatroom %s", this.clientNode.getName(),
				this.clientNode.getChatroomId()));

		handleRequestProcessingError(Error.Chat);
	}

	private void disconnectFromServer() throws Exception {
		printThreadMessageToConsole(String.format("Client %s disconnecting from server ", this.clientNode.getName()));
		String requestedChatroom = this.clientNode.getChatroomId();
		Chatroom chatroomAlreadyOnRecord = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroom);
		chatroomAlreadyOnRecord.removeClientNode(clientNode);
	}

	private void writeResponseToClient(String response) {
		printThreadMessageToConsole(String.format("Writing response to client: %s", response));
		try {
			clientOutputStreamWriter.write(response);
			clientOutputStreamWriter.flush();
			printThreadMessageToConsole("Response sent to client successfully");
		} catch (Exception e) {
			e.printStackTrace();
			printThreadMessageToConsole("Failed to write response to client: " + response);
		}
	}
}
