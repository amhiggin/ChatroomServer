package main.java;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.ArrayList;
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
	private static final int UNDEFINED_JOIN_ID = -1;
	private static final String JOIN_CHATROOM_IDENTIFIER = "JOIN_CHATROOM: ";
	private static final String CHAT_IDENTIFIER = "CHAT: ";
	private static final String LEAVE_CHATROOM_IDENTIFIER = "LEAVE_CHATROOM: ";
	private static final String JOIN_ID_IDENTIFIER = "JOIN_ID: ";
	private static final String CLIENT_NAME_IDENTIFIER = "CLIENT_NAME: ";

	private ClientNode clientNode;
	private ClientRequest requestType;
	private List<String> receivedFromClient;
	private OutputStreamWriter clientOutputStreamWriter;

	public ClientThread(Socket clientSocket) {
		printThreadMessageToConsole("spawning new client thread...");
		try {
			this.receivedFromClient = getFullMessageFromClient(clientSocket);

			if (this.receivedFromClient != null) {
				this.requestType = requestedAction(this.receivedFromClient);
				this.clientNode = extractClientInfo(clientSocket, this.requestType, this.receivedFromClient);
				this.clientOutputStreamWriter = new OutputStreamWriter(
						this.clientNode.getConnection().getOutputStream());
			}
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
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
		} catch (Exception e) {
			e.printStackTrace(); // TODO @Amber remove later
			ChatroomServer.outputServiceErrorMessageToConsole(String.format("%s", e));
		} finally {
			this.interrupt();
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

	public List<String> getFullMessageFromClient(Socket clientSocket) throws IOException {
		BufferedInputStream inputStream = new BufferedInputStream(clientSocket.getInputStream());
		ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
		int result = inputStream.read();
		while ((result != -1) && (inputStream.available() > 0)) {
			outputStream.write((byte) result);
			result = inputStream.read();
		}
		// Assuming UTF-8 encoding
		String inFromClient = outputStream.toString("UTF-8");
		List<String> lines = getRequestStringAsArrayList(inFromClient);

		return lines;
	}

	private List<String> getRequestStringAsArrayList(String inFromClient) {
		String[] linesArray = inFromClient.split("\n");
		List<String> lines = new ArrayList<String>();
		for (String line : linesArray) {
			lines.add(line);
		}
		return lines;
	}

	public ClientNode extractClientInfo(Socket clientSocket, ClientRequest requestType, List<String> message)
			throws IOException {
		switch (requestType) {
		case JOIN_CHATROOM:
			return new ClientNode(clientSocket, message.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1],
					ChatroomServer.nextClientId.getAndIncrement());
		case CHAT:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(CHAT_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case LEAVE_CHATROOM:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(LEAVE_CHATROOM_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]));
		case DISCONNECT:
			return new ClientNode(clientSocket, message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1], null,
					UNDEFINED_JOIN_ID);
		case HELO:
			printThreadMessageToConsole("Helo client node created");
			return new ClientNode(clientSocket, null, null, UNDEFINED_JOIN_ID);
		case KILL_SERVICE:
			return new ClientNode(null, null, null, UNDEFINED_JOIN_ID);
		default:
			printThreadMessageToConsole("Null clientnode created: no match with expected request types");
			return null;
		}
	}

	public ClientRequest requestedAction(List<String> message) throws IOException {
		String requestType = parseClientRequestType(message);
		try {
			ClientRequest clientRequest = ClientRequest.valueOf(requestType);
			printThreadMessageToConsole("The parsed request type is " + clientRequest.getValue());
			return clientRequest;
		} catch (Exception e) {
			ChatroomServer.outputServiceErrorMessageToConsole("Error occurred trying to fetch the request type");
			return null;
		}
	}

	private String parseClientRequestType(List<String> message) throws IOException {
		printThreadMessageToConsole("In parseClientRequestType method");
		String[] requestType = message.get(0).split(SPLIT_PATTERN, 0);
		if (requestType[0].contains("HELO")) {
			String temp = requestType[0];
			String[] splitString = temp.split(" ", 0);
			requestType[0] = splitString[0];
		}
		printThreadMessageToConsole(String.format("Parsed request type '%s", requestType[0]));

		return requestType[0];
	}
}
