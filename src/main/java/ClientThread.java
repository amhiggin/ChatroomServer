package main.java;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;

/*
 * Thread which is created for every new client interaction.
 * Enables handling multiple requests and responses in parallel.
 */

public class ClientThread extends Thread {

	private static final String SPLIT_PATTERN = ": ";
	private static final String HELO_IDENTIFIER = "HELO ";
	private static final int UNDEFINED_JOIN_ID = -1;
	private static final String JOIN_CHATROOM_IDENTIFIER = "JOIN_CHATROOM: ";
	private static final String CHAT_IDENTIFIER = "CHAT: ";
	private static final String LEAVE_CHATROOM_IDENTIFIER = "LEAVE_CHATROOM: ";
	private static final String JOIN_ID_IDENTIFIER = "JOIN_ID: ";
	private static final String CLIENT_NAME_IDENTIFIER = "CLIENT_NAME: ";
	public static final String STUDENT_ID = "13327954";

	PrintWriter socketOutputStream = null;
	BufferedReader socketInputStream = null;
	private Socket socket;
	List<Chatroom> joinedChatrooms = null;
	private int joinId;
	private String clientName = null;

	public ClientThread(Socket clientSocket) {
		printThreadMessageToConsole("Creating new runnable task for client connection...");
		try {
			this.socket = clientSocket;
			this.socketOutputStream = new PrintWriter(clientSocket.getOutputStream(), true);
			this.socketInputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/*
	 * Every time the thread is run, the message, request type and clientInfo
	 * should be extracted. These are request-specific. The socket, input and
	 * output streams, and joinIds should all be common to the connection
	 * itself. They don't need to be redefined every time the client connects.
	 */

	@Override
	public void run() {
		while (true) {
			try {
				List<String> receivedFromClient = getFullMessageFromClient(this.socket);
				ClientRequest requestType = requestedAction(receivedFromClient);
				if (requestType == null) {
					ChatroomServer.outputServiceErrorMessageToConsole("Could not parse request");
					return;
				}
				ClientNode clientNode = extractClientInfo(this.socket, requestType, receivedFromClient);
				if (clientNode == null) {
					ChatroomServer
							.outputServiceErrorMessageToConsole(String.format("Could not process invalid request"));
					return;
				}
				dealWithRequestAsAppropriate(receivedFromClient, clientNode, requestType);
			} catch (Exception e) {
				ChatroomServer.outputServiceErrorMessageToConsole(String.format("%s", e));
				e.printStackTrace();
			}
		}
	}

	private void dealWithRequestAsAppropriate(List<String> receivedFromClient, ClientNode clientNode,
			ClientRequest requestType) throws Exception {
		if (clientNode == null) {
			ChatroomServer.outputServiceErrorMessageToConsole("Null client node");
			return;
		}
		switch (requestType) {
		case JOIN_CHATROOM:
			joinChatroom(clientNode);
			break;
		case HELO:
			sayHello(clientNode, receivedFromClient);
			break;
		case LEAVE_CHATROOM:
			leaveChatroom(clientNode);
			break;
		case CHAT:
			chat(clientNode);
			break;
		case DISCONNECT:
			disconnectFromServer(clientNode);
			break;
		case KILL_SERVICE:
			killService(clientNode);
			break;
		default:
			handleRequestProcessingError(Error.InvalidRequest, clientNode);
			return;
		}
		ChatroomServer.recordClientChangeWithServer(requestType, this.socket, clientNode);

	}

	private void killService(ClientNode clientNode) {
		ChatroomServer.printServerMessageToConsole(
				String.format("Client %s requested to kill service", clientNode.getName()));
		ChatroomServer.setRunning(false);
		try {
			wait(10000);
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
		if (!ChatroomServer.getServerSocket().isClosed()) {
			handleRequestProcessingError(Error.KillService, clientNode);
		}
	}

	private void handleRequestProcessingError(Error errorMessage, ClientNode clientNode) {
		String errorResponse = String.format(ServerResponse.ERROR.getValue(), errorMessage.getValue(),
				errorMessage.getDescription());
		try {
			this.socket.getOutputStream().write(errorResponse.getBytes());
		} catch (IOException e) {
			String temporaryErrorMessageHolder = errorResponse;
			errorResponse = "Failed to communicate failure response to client: " + temporaryErrorMessageHolder
					+ ". Exception thrown: " + e.getMessage();
			e.printStackTrace();
		}
		printThreadMessageToConsole(errorResponse);
	}

	private void joinChatroom(ClientNode clientNode) {
		String chatroomRequested = clientNode.getChatroomId();
		printThreadMessageToConsole(
				String.format("Client %s joining chatroom %s", clientNode.getName(), chatroomRequested));
		try {
			String requestedChatroomToJoin = chatroomRequested;
			Chatroom requestedChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToJoin);
			if (clientNode.getJoinId() == null) {
				clientNode.setJoinId(ChatroomServer.nextClientId.getAndIncrement());
			}

			if (requestedChatroom != null) {
				printThreadMessageToConsole(String.format("Chatroom %s already exists.. Will add client %s",
						requestedChatroom.getChatroomId(), clientNode.getName()));
				try {
					requestedChatroom.addNewClientToChatroom(this.socket, clientNode);
					this.joinedChatrooms.add(requestedChatroom);
				} catch (Exception e) {
					e.printStackTrace();
					printThreadMessageToConsole(String.format("%s was already a member of %s - resending JOIN response",
							clientNode, requestedChatroom.getChatroomId()));
					writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom, clientNode);
					return;
				}
			} else {
				requestedChatroom = createChatroom(clientNode, chatroomRequested);
				printThreadMessageToConsole(
						String.format("Chatroom %s was created!", requestedChatroom.getChatroomId()));
				// update server records
				requestedChatroom.addNewClientToChatroom(this.socket, clientNode);
				ChatroomServer.getActiveChatRooms().add(requestedChatroom);
			}
			printThreadMessageToConsole(String.format("Sending join response to client %s", clientNode.getName()));
			writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom, clientNode);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.JoinChatroom, clientNode);
		}
		printThreadMessageToConsole("Finished in join method");

	}

	private Chatroom createChatroom(ClientNode clientNode, String chatroomRequested) {
		Chatroom chatroom = new Chatroom(chatroomRequested, ChatroomServer.nextChatroomId);
		printThreadMessageToConsole(String.format("Created new chatroom %s", chatroom.getChatroomId()));
		return chatroom;
	}

	private void writeJoinResponseToClientAndBroadcastMessageInChatroom(Chatroom requestedChatroom,
			ClientNode clientNode) {
		String responseToClient = String.format(ServerResponse.JOIN.getValue(), requestedChatroom.getChatroomId(),
				ChatroomServer.serverIP, ChatroomServer.serverPort, requestedChatroom.getChatroomRef(),
				clientNode.getJoinId());
		writeResponseToClient(responseToClient);

		// Broadcast message in chatroom
		String clientJoinedChatroomMessage = String.format("%s has joined this chatroom", clientNode.getName());
		String chatMessage = String.format(ServerResponse.CHAT.getValue(), requestedChatroom.getChatroomRef(),
				clientNode.getName(), clientJoinedChatroomMessage);
		requestedChatroom.broadcastMessageInChatroom(chatMessage);
	}

	private void leaveChatroom(ClientNode clientNode) {
		String chatroomRequested = clientNode.getChatroomId();
		printThreadMessageToConsole(
				String.format("Client %s leaving chatroom %s", clientNode.getName(), chatroomRequested));
		try {
			String requestedChatroomToLeave = chatroomRequested;
			Chatroom existingChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToLeave);
			if (existingChatroom != null) {
				// NOTE: don't need to remove client from server records
				existingChatroom.removeClientNode(socket, clientNode);
			}
			String responseToClient = String.format(ServerResponse.LEAVE.getValue(), existingChatroom.getChatroomRef(),
					clientNode.getJoinId());
			writeResponseToClient(responseToClient);
			String clientLeftChatroomMessage = String.format("%s has left this chatroom", clientNode.getName());
			// must create chat message
			String chatMessage = String.format(ServerResponse.CHAT.getValue(), existingChatroom.getChatroomRef(),
					clientNode.getName(), clientLeftChatroomMessage);
			existingChatroom.broadcastMessageInChatroom(chatMessage);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.LeaveChatroom, clientNode);
		}
	}

	private void sayHello(ClientNode clientNode, List<String> receivedFromClient) {
		printThreadMessageToConsole("Going to say hello!");
		try {
			String response = constructHelloResponse(receivedFromClient);
			writeResponseToClient(response);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.Helo, clientNode);
		}
	}

	private String constructHelloResponse(List<String> receivedFromClient) {
		String helloResponse = String.format(ServerResponse.HELO.getValue(),
				receivedFromClient.get(0).split(HELO_IDENTIFIER)[1].replaceAll("\n", ""), ChatroomServer.serverIP,
				ChatroomServer.serverPort, STUDENT_ID);
		return helloResponse;
	}

	private void chat(ClientNode clientNode) throws IOException {
		String message = clientNode.getReceivedFromClient().get(3).split(SPLIT_PATTERN, 0)[1];
		Chatroom chatroomAlreadyOnRecord = ChatroomServer.retrieveRequestedChatroomIfExists(clientNode.getChatroomId());
		if (chatroomAlreadyOnRecord != null) {
			String responseToClient = String.format(ServerResponse.CHAT.getValue(),
					chatroomAlreadyOnRecord.getChatroomId(), clientNode.getJoinId(), clientNode.getName(), message);
			chatroomAlreadyOnRecord.broadcastMessageInChatroom(responseToClient);
			return;
		}
		printThreadMessageToConsole(
				String.format("Client %s chatting in chatroom %s", clientNode.getName(), clientNode.getChatroomId()));

		handleRequestProcessingError(Error.Chat, clientNode);
	}

	private void disconnectFromServer(ClientNode clientNode) throws Exception {
		String chatroomRequested = clientNode.getChatroomId();
		printThreadMessageToConsole(String.format("Client %s disconnecting from server ", clientNode.getName()));
		String requestedChatroom = chatroomRequested;
		Chatroom chatroomAlreadyOnRecord = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroom);
		chatroomAlreadyOnRecord.removeClientNode(this.socket, clientNode);
	}

	private void writeResponseToClient(String response) {
		printThreadMessageToConsole(String.format("Writing response to client: %s", response));
		try {
			this.socketOutputStream.write(response);
			this.socketOutputStream.flush();
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
			return new ClientNode(message.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1], ChatroomServer.nextClientId.getAndIncrement(),
					message);
		case CHAT:
			return new ClientNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(CHAT_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]), message);
		case LEAVE_CHATROOM:
			return new ClientNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(LEAVE_CHATROOM_IDENTIFIER, 0)[1],
					Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]), message);
		case DISCONNECT:
			return new ClientNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1], null, UNDEFINED_JOIN_ID, message);
		case HELO:
			printThreadMessageToConsole("Helo client node created");
			return new ClientNode(null, null, UNDEFINED_JOIN_ID, message);
		case KILL_SERVICE:
			return new ClientNode(null, null, null, message);
		case NULL:
			return null;
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
		String[] requestType = message.get(0).split(SPLIT_PATTERN, 0);
		if (requestType[0].contains("HELO")) {
			String temp = requestType[0];
			String[] splitString = temp.split(" ", 0);
			requestType[0] = splitString[0];
		}
		printThreadMessageToConsole(String.format("Parsed request type '%s", requestType[0]));

		return requestType[0];
	}

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private void printThreadMessageToConsole(String message) {
		System.out.println(String.format("%s>> THREAD: %s", getCurrentDateTime(), message));

	}

	// FIXME TODO @Amber see where I need this
	private void checkIfAlreadyAssignedJoinId(Socket clientSocket) {
		for (Socket socket : ChatroomServer.getAllConnectedClients()) {
			if (socket == clientSocket) {
				return;
			}
		}
		this.joinId = ChatroomServer.nextClientId.getAndIncrement();
	}

	// TODO FIXME @Amber figure out where this comes into play
	private String getRequestedChatroomRef(List<String> receivedFromClient) {
		String chatroomRef = null;
		for (String line : receivedFromClient) {
			if (line.contains(LEAVE_CHATROOM_IDENTIFIER)) {
				chatroomRef = line.split(LEAVE_CHATROOM_IDENTIFIER, 0)[1];
			} else if (line.contains(JOIN_CHATROOM_IDENTIFIER)) {
				chatroomRef = line.split(JOIN_CHATROOM_IDENTIFIER, 0)[1];
			} else if (line.contains(CHAT_IDENTIFIER)) {
				chatroomRef = line.split(CHAT_IDENTIFIER, 0)[1];
			}
		}
		return chatroomRef;
	}

}
