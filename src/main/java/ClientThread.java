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
 * 
	 * Every time the thread is run, the message, request type and clientInfo
	 * should be extracted. These are request-specific. The socket, input and
	 * output streams, and joinIds should all be common to the connection
	 * itself. They don't need to be redefined every time the client connects.
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

	ClientConnectionObject connectionObject;
	List<Chatroom> joinedChatrooms = null;
	private int joinId;

	public ClientThread(Socket clientSocket) {
		printThreadMessageToConsole("Creating new runnable task for client connection...");
		try {
			this.connectionObject = new ClientConnectionObject(clientSocket,
					new PrintWriter(clientSocket.getOutputStream(), true),
					new BufferedReader(new InputStreamReader(clientSocket.getInputStream())));
			this.joinId = ChatroomServer.nextClientId.getAndIncrement();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@Override
	public void run() {
		while (true) {
			try {
				List<String> receivedFromClient = getFullMessageFromClient(this.connectionObject.getSocket());
				ClientRequest requestType = requestedAction(receivedFromClient);
				if (requestType == null) {
					ChatroomServer.outputServiceErrorMessageToConsole("Could not parse request");
					return;
				}
				ClientRequestNode clientNode = extractClientInfo(this.connectionObject.getSocket(), requestType,
						receivedFromClient);
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

	private synchronized void dealWithRequestAsAppropriate(List<String> receivedFromClient,
			ClientRequestNode clientNode, ClientRequest requestType) throws Exception {
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
		ChatroomServer.recordClientChangeWithServer(this.connectionObject, clientNode);

	}

	private void killService(ClientRequestNode clientNode) {
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

	private void handleRequestProcessingError(Error errorMessage, ClientRequestNode clientNode) {
		String errorResponse = String.format(ServerResponse.ERROR.getValue(), errorMessage.getValue(),
				errorMessage.getDescription());
		try {
			this.connectionObject.getSocket().getOutputStream().write(errorResponse.getBytes());
		} catch (IOException e) {
			String temporaryErrorMessageHolder = errorResponse;
			errorResponse = "Failed to communicate failure response to client: " + temporaryErrorMessageHolder
					+ ". Exception thrown: " + e.getMessage();
			e.printStackTrace();
		}
		printThreadMessageToConsole(errorResponse);
	}

	private void joinChatroom(ClientRequestNode clientNode) {
		String chatroomRequested = clientNode.getChatroomRequested();
		printThreadMessageToConsole(
				String.format("Client %s joining chatroom %s", clientNode.getName(), chatroomRequested));
		try {
			String requestedChatroomToJoin = chatroomRequested;
			Chatroom requestedChatroom = ChatroomServer
					.retrieveRequestedChatroomByRoomIdIfExists(requestedChatroomToJoin);
			if (this.joinId == UNDEFINED_JOIN_ID) {
				this.joinId = ChatroomServer.nextClientId.getAndIncrement();
			}

			if (requestedChatroom == null) {
				requestedChatroom = createChatroom(chatroomRequested);
				printThreadMessageToConsole(
						String.format("Chatroom %s was created!", requestedChatroom.getChatroomId()));
				requestedChatroom.addNewClientToChatroom(this.connectionObject, clientNode);
				// update server records
				ChatroomServer.getActiveChatRooms().add(requestedChatroom);
			} else {
				printThreadMessageToConsole(String.format("Chatroom %s already exists.. Will add client %s",
						requestedChatroom.getChatroomId(), clientNode.getName()));
				try {
					requestedChatroom.addNewClientToChatroom(this.connectionObject, clientNode);
				} catch (Exception e) {
					e.printStackTrace();
					printThreadMessageToConsole(String.format("%s was already a member of %s - resending JOIN response",
							clientNode, requestedChatroom.getChatroomId()));
					writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom, clientNode);
					return;
				}
			}
			printThreadMessageToConsole(String.format("Sending join response to client %s", clientNode.getName()));
			writeJoinResponseToClientAndBroadcastMessageInChatroom(requestedChatroom, clientNode);
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.JoinChatroom, clientNode);
		}
		printThreadMessageToConsole("Finished in join method");

	}

	private Chatroom createChatroom(String chatroomRequested) {
		Chatroom chatroom = new Chatroom(chatroomRequested, ChatroomServer.nextChatroomId.getAndIncrement());
		printThreadMessageToConsole(String.format("Created new chatroom %s", chatroom.getChatroomId()));
		return chatroom;
	}

	private void writeJoinResponseToClientAndBroadcastMessageInChatroom(Chatroom requestedChatroom,
			ClientRequestNode clientNode) {
		String responseToClient = String.format(ServerResponse.JOIN.getValue(), requestedChatroom.getChatroomId(),
				ChatroomServer.serverIP, ChatroomServer.serverPort, requestedChatroom.getChatroomRef(), this.joinId);
		writeResponseToClient(responseToClient);

		// Broadcast message in chatroom
		String clientJoinedChatroomMessage = String.format("%s has joined this chatroom", clientNode.getName());
		String chatMessage = String.format(ServerResponse.CHAT.getValue(), requestedChatroom.getChatroomRef(),
				clientNode.getName(), clientJoinedChatroomMessage);
		requestedChatroom.broadcastMessageInChatroom(chatMessage);
	}

	private void leaveChatroom(ClientRequestNode clientNode) {
		String chatroomRequested = clientNode.getChatroomRequested();
		printThreadMessageToConsole(
				String.format("Client %s leaving chatroom %s", clientNode.getName(), chatroomRequested));
		try {
			String requestedChatroomToLeave = chatroomRequested;
			Chatroom existingChatroom = ChatroomServer
					.retrieveRequestedChatroomByRoomRefIfExists(requestedChatroomToLeave);

			if (existingChatroom != null) {
				// First, send leave response to client in question
				if (clientPresentInChatroom(existingChatroom)) {
					String responseToClient = String.format(ServerResponse.LEAVE.getValue(),
							existingChatroom.getChatroomRef(), this.joinId);
					writeResponseToClient(responseToClient);
				}

				// Secondly, send message in chatroom about client leaving
				// This is done regardless of whether client is still in the
				// chatroom
				String clientLeftChatroomMessage = String.format("%s has left this chatroom", clientNode.getName());
				String chatMessage = String.format(ServerResponse.CHAT.getValue(), existingChatroom.getChatroomRef(),
						clientNode.getName(), clientLeftChatroomMessage);

				existingChatroom.broadcastMessageInChatroom(chatMessage);

				// Thirdly, remove the client from the chatroom
				if (clientPresentInChatroom(existingChatroom)) {
					existingChatroom.removeClientRecord(this.connectionObject.getSocket(), clientNode);
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
			handleRequestProcessingError(Error.LeaveChatroom, clientNode);
		}
	}

	private boolean clientPresentInChatroom(Chatroom existingChatroom) {
		for (ClientConnectionObject record : existingChatroom.getListOfConnectedClients()) {
			if (record.getSocket().equals(this.connectionObject.getSocket())) {
				return true;
			}
		}
		return false;
	}

	private void sayHello(ClientRequestNode clientNode, List<String> receivedFromClient) {
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

	private void chat(ClientRequestNode clientNode) throws IOException {
		String message = clientNode.getReceivedFromClient().get(3).split(SPLIT_PATTERN, 0)[1];
		Chatroom chatroomAlreadyOnRecord = ChatroomServer
				.retrieveRequestedChatroomByRoomRefIfExists(clientNode.getChatroomRequested());
		if (chatroomAlreadyOnRecord != null) {
			String responseToClient = String.format(ServerResponse.CHAT.getValue(),
					chatroomAlreadyOnRecord.getChatroomRef(), clientNode.getName(), message);
			chatroomAlreadyOnRecord.broadcastMessageInChatroom(responseToClient);
			return;
		}
		printThreadMessageToConsole(String.format("Client %s chatting in chatroom %s", clientNode.getName(),
				clientNode.getChatroomRequested()));

		handleRequestProcessingError(Error.Chat, clientNode);
	}

	private void disconnectFromServer(ClientRequestNode clientNode) throws Exception {
		printThreadMessageToConsole(String.format("Client %s disconnecting from server ", clientNode.getName()));
		ChatroomServer.recordClientChangeWithServer(this.connectionObject, clientNode);
	}

	private synchronized void writeResponseToClient(String response) {
		printThreadMessageToConsole(String.format("Writing response to client: %s", response));
		try {
			this.connectionObject.getSocketOutputStream().write(response);
			this.connectionObject.getSocketOutputStream().flush();
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

	public ClientRequestNode extractClientInfo(Socket clientSocket, ClientRequest requestType, List<String> message)
			throws IOException {
		switch (requestType) {
		case JOIN_CHATROOM:
			return new ClientRequestNode(message.get(3).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(JOIN_CHATROOM_IDENTIFIER, 0)[1], message, requestType);
		case CHAT:
			this.joinId = Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]);
			return new ClientRequestNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(CHAT_IDENTIFIER, 0)[1], message, requestType);
		case LEAVE_CHATROOM:
			this.joinId = Integer.parseInt(message.get(1).split(JOIN_ID_IDENTIFIER, 0)[1]);
			return new ClientRequestNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1],
					message.get(0).split(LEAVE_CHATROOM_IDENTIFIER, 0)[1], message, requestType);
		case DISCONNECT:
			return new ClientRequestNode(message.get(2).split(CLIENT_NAME_IDENTIFIER, 0)[1], null, message,
					requestType);
		case HELO:
			printThreadMessageToConsole("Helo client node created");
			return new ClientRequestNode(null, null, message, requestType);
		case KILL_SERVICE:
			return new ClientRequestNode(null, null, message, requestType);
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

}
