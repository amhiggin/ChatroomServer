package main.java;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.joda.time.LocalDateTime;

public class Chatroom implements Comparable<Chatroom> {

	private ConcurrentSkipListSet<ClientNode> listOfConnectedClients;
	private String chatroomId;
	private Integer chatroomRef;

	public Chatroom(String id, AtomicInteger chatroomRef) {
		chatroomId = id;
		this.listOfConnectedClients = new ConcurrentSkipListSet<ClientNode>();
		this.chatroomRef = Integer.valueOf(chatroomRef.intValue());
	}

	public void removeClientNode(ClientNode node) {
		printChatroomMessageToConsole(String.format("Removing node %s from chatroom %s", node.getName(), chatroomId));
		if (this.listOfConnectedClients.contains(node)) {
			this.listOfConnectedClients.remove(node);
		}
	}

	public void addNewClientToChatroom(ClientNode node) throws Exception {
		printChatroomMessageToConsole(String.format("Adding new node %s to chatroom %s", node.getName(), chatroomId));
		if (!listOfConnectedClients.contains(node)) {
			listOfConnectedClients.add(node);
			return;
		}
		throw new Exception(String.format("Client %s already added to chatroom %s", node.getName(), chatroomId));
	}

	// CHAT(
	// "CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE:
	// %s\n\n"),
	public synchronized void broadcastMessageInChatroom(String message) {
		printChatroomMessageToConsole("Will broadcast message to all clients in chatroom as follows: " + message);

		for (ClientNode client : listOfConnectedClients) {
			if (client != null) {
				try {
					PrintStream socketPrintStream = new PrintStream(client.getConnection().getOutputStream());
					socketPrintStream.print(message);
					printChatroomMessageToConsole("Broadcasted to client " + client.getName());
				} catch (Exception e) {
					printChatroomMessageToConsole("Failed to broadcast to client " + client.getName());
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

	public ConcurrentSkipListSet<ClientNode> getSetOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}

	public Integer getChatroomRef() {
		return this.chatroomRef;
	}
}
