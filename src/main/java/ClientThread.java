package main.java;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
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

	private OutputStreamWriter clientOutputStreamWriter;
	PrintWriter socketOutputStream = null;
	BufferedReader socketInputStream = null;
	private Socket socket;

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

	@Override
	public void run() {
		while (true) {
			try {
				List<String> receivedFromClient = getFullMessageFromClient(this.socket);
				ClientRequest requestType = requestedAction(receivedFromClient);
				ClientNode clientNode = extractClientInfo(this.socket, requestType, receivedFromClient);
				if (requestType == null) {
					handleRequestProcessingError(Error.InvalidRequest, clientNode);
					return;
				}
				if (clientNode == null) {
					ChatroomServer.outputServiceErrorMessageToConsole("Null client node");
					return;
				}
				switch (requestType) {
				case JOIN_CHATROOM:
					joinChatroom(clientNode);
					printThreadMessageToConsole("Going to execute break now");
					break;
				case HELO:
					sayHello(clientNode, receivedFromClient);
					break;
				case LEAVE_CHATROOM:
					leaveChatroom(clientNode);
					break;
				case CHAT:
					chat(receivedFromClient, clientNode);
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
				ChatroomServer.recordClientChangeWithServer(requestType, clientNode);
				printThreadMessageToConsole("Finished running thread");
			} catch (Exception e) {
				e.printStackTrace();
				ChatroomServer.outputServiceErrorMessageToConsole(String.format("%s", e));
			}
		}
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
			clientNode.getConnection().getOutputStream().write(errorResponse.getBytes());
		} catch (IOException e) {
			String temporaryErrorMessageHolder = errorResponse;
			errorResponse = "Failed to communicate failure response to client: " + temporaryErrorMessageHolder
					+ ". Exception thrown: " + e.getMessage();
			e.printStackTrace();
		}
		printThreadMessageToConsole(errorResponse);
	}

	private Chatroom createChatroom(ClientNode clientNode) {
		Chatroom chatroom = new Chatroom(clientNode.getChatroomId(), ChatroomServer.nextChatroomId);
		printThreadMessageToConsole(String.format("Created new chatroom %s", chatroom.getChatroomId()));
		return chatroom;
	}

	private void joinChatroom(ClientNode clientNode) {
		printThreadMessageToConsole(
				String.format("Client %s joining chatroom %s", clientNode.getName(), clientNode.getChatroomId()));
		try {
			String requestedChatroomToJoin = clientNode.getChatroomId();
			Chatroom requestedChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToJoin);
			if (clientNode.getJoinId() == null) {
				clientNode.setJoinId(ChatroomServer.nextClientId.getAndIncrement());
			}

			if (requestedChatroom != null) {
				printThreadMessageToConsole(String.format("Chatroom %s already exists.. Will add client %s",
						requestedChatroom.getChatroomId(), clientNode.getName()));
				try {
					requestedChatroom.addNewClientToChatroom(clientNode);
				} catch (Exception e) {
					e.printStackTrace();
					printThreadMessageToConsole(String.format("%s was already a member of %s - resending JOIN response",
							clientNode, requestedChatroom.getChatroomId()));
					writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom, clientNode);
					return;
				}
			} else {
				requestedChatroom = createChatroom(clientNode);
				printThreadMessageToConsole(
						String.format("Chatroom %s was created!", requestedChatroom.getChatroomId()));
				// update server records
				requestedChatroom.addNewClientToChatroom(clientNode);
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

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	private void printThreadMessageToConsole(String message) {
		System.out.println(String.format("%s>> THREAD: %s", getCurrentDateTime(), message));

	}

	private void writeJoinResponseToClientAndBroadcastMessageInChatroom(Chatroom requestedChatroom,
			ClientNode clientNode) {
		String responseToClient = String.format(ServerResponse.JOIN.getValue(), clientNode.getChatroomId(),
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
		printThreadMessageToConsole(
				String.format("Client %s leaving chatroom %s", clientNode.getName(), clientNode.getChatroomId()));
		String requestedChatroomToLeave = clientNode.getChatroomId();
		Chatroom existingChatroom = ChatroomServer.retrieveRequestedChatroomIfExists(requestedChatroomToLeave);
		try {
			if (existingChatroom != null) {
				// NOTE: don't need to remove client from server records
				existingChatroom.removeClientNode(clientNode);
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

	private void chat(List<String> receivedFromClient, ClientNode clientNode) throws IOException {
		String message = receivedFromClient.get(3).split(SPLIT_PATTERN, 0)[1];
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
		printThreadMessageToConsole(String.format("Client %s disconnecting from server ", clientNode.getName()));
		String requestedChatroom = clientNode.getChatroomId();
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
