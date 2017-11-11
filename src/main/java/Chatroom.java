package main.java;

import java.io.PrintWriter;
import java.net.Socket;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.joda.time.LocalDateTime;

public class Chatroom implements Comparable<Chatroom> {

	private Map<Socket, PrintWriter> listOfConnectedClients;
	private String chatroomId;
	private Integer chatroomRef;

	public Chatroom(String id, int chatroomRef) {
		chatroomId = id;
		this.listOfConnectedClients = new HashMap<Socket, PrintWriter>();
		this.chatroomRef = chatroomRef;
	}

	public void removeClientRecord(Socket clientSocket, ClientRequestNode node) throws Exception {
		printChatroomMessageToConsole(
				String.format("Removing node %s from chatroom %s", node.getName(), this.chatroomRef));
		for (Entry<Socket, PrintWriter> record : listOfConnectedClients.entrySet()) {
			if (record.getKey() == clientSocket) {
				this.listOfConnectedClients.remove(record);
				return;
			}
		}
		throw new Exception("Client " + node.getName() + " was not part of chatroom " + this.chatroomId);
	}

	public void addNewClientToChatroom(Socket clientSocket, ClientRequestNode node, PrintWriter socketOutputStream)
			throws Exception {
		printChatroomMessageToConsole(String.format("Adding new node %s to chatroom %s", node.getName(), chatroomId));
		for (Socket socket : listOfConnectedClients.keySet()) {
			if (socket == clientSocket) {
				return;
			}
		}
		listOfConnectedClients.put(clientSocket, socketOutputStream);
	}

	public synchronized void broadcastMessageInChatroom(String message) {
		printChatroomMessageToConsole("Will broadcast message to all clients in chatroom as follows: " + message);

		for (Entry<Socket, PrintWriter> record : listOfConnectedClients.entrySet()) {
			if (record.getKey() != null) {
				try {
					printChatroomMessageToConsole("Sending...");
					record.getValue().print(message);
					record.getValue().flush();
				} catch (Exception e) {
					printChatroomMessageToConsole(
							"Failed to broadcast to client at socket " + record.getKey().getInetAddress().toString());
				}
			}
		}
		printChatroomMessageToConsole("Finished broadcast in chatroom " + getChatroomId());
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

	public Map<Socket, PrintWriter> getListOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}

	public Integer getChatroomRef() {
		return this.chatroomRef;
	}
}
