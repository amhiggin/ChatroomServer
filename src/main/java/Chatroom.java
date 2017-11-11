package main.java;

import java.io.PrintStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;

public class Chatroom implements Comparable<Chatroom> {

	private List<Socket> listOfConnectedClients;
	private String chatroomId;
	private Integer chatroomRef;

	public Chatroom(String id, int chatroomRef) {
		chatroomId = id;
		this.listOfConnectedClients = new ArrayList<Socket>();
		this.chatroomRef = chatroomRef;
	}

	public void removeClientRecord(Socket clientSocket, ClientRequestNode node) throws Exception {
		printChatroomMessageToConsole(
				String.format("Removing node %s from chatroom %s", node.getName(), this.chatroomRef));
		for (Socket socket : listOfConnectedClients) {
			if (socket == clientSocket) {
				this.listOfConnectedClients.remove(clientSocket);
				return;
			}
		}
		throw new Exception("Client " + node.getName() + " was not part of chatroom " + this.chatroomId);
	}

	public void addNewClientToChatroom(Socket clientSocket, ClientRequestNode node) throws Exception {
		printChatroomMessageToConsole(String.format("Adding new node %s to chatroom %s", node.getName(), chatroomId));
		for (Socket socket : listOfConnectedClients) {
			if (socket == clientSocket) {
				return;
			}
		}
		listOfConnectedClients.add(clientSocket);
	}

	// CHAT(
	// "CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE:
	// %s\n\n"),
	public synchronized void broadcastMessageInChatroom(String message) {
		printChatroomMessageToConsole("Will broadcast message to all clients in chatroom as follows: " + message);

		for (Socket socket : listOfConnectedClients) {
			if (socket != null) {
				try {
					PrintStream socketPrintStream = new PrintStream(socket.getOutputStream());
					socketPrintStream.print(message);
				} catch (Exception e) {
					printChatroomMessageToConsole(
							"Failed to broadcast to client at socket " + socket.getInetAddress().toString());
				}
			}
		}
		printChatroomMessageToConsole("Finished broadcast in chatroom" + getChatroomId());
	}

	public void printChatroomMessageToConsole(String message) {
		System.out.println(String.format("%s>> CHATROOM%s: %s", getCurrentDateTime(), getChatroomId(), message));
	}

	private static String getCurrentDateTime() {
		LocalDateTime now = new LocalDateTime();
		return now.toString();
	}

	@Override
	public int compareTo(Chatroom chatroom) {
		if (this.getChatroomRef() < chatroom.getChatroomRef()) {
			return -1;
		} else if (this.getChatroomRef() > chatroom.getChatroomRef()) {
			return 1;
		}
		return 0;
	}

	public List<Socket> getListOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}

	public Integer getChatroomRef() {
		return this.chatroomRef;
	}
}
