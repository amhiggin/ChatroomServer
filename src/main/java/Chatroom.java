package main.java;

import java.util.ArrayList;
import java.util.List;

import org.joda.time.LocalDateTime;

public class Chatroom implements Comparable<Chatroom> {

	private List<ClientConnectionObject> listOfConnectedClients;
	private String chatroomId;
	private Integer chatroomRef;

	public Chatroom(String id, int chatroomRef) {
		chatroomId = id;
		this.listOfConnectedClients = new ArrayList<ClientConnectionObject>();
		this.chatroomRef = chatroomRef;
	}

	public void removeClientRecord(ClientConnectionObject clientConnectionObject, ClientRequestNode node)
			throws Exception {
		printChatroomMessageToConsole(
				String.format("Removing node %s from chatroom %s", node.getName(), this.chatroomRef));
		for (ClientConnectionObject record : listOfConnectedClients) {
			if (record == clientConnectionObject) {
				this.listOfConnectedClients.remove(record);
				printChatroomMessageToConsole(
						String.format("Removed node %s from chatroom %s", node.getName(), this.chatroomRef));
				return;
			}
		}
		throw new Exception("Client " + node.getName() + " was not part of chatroom: can't remove." + this.chatroomId);
	}

	public void addNewClientToChatroom(ClientConnectionObject clientConnectionObject, ClientRequestNode node)
			throws Exception {
		printChatroomMessageToConsole(String.format("Adding new node %s to chatroom %s", node.getName(), chatroomId));
		for (ClientConnectionObject record : listOfConnectedClients) {
			if (record == clientConnectionObject) {
				return;
			}
		}
		listOfConnectedClients.add(clientConnectionObject);
	}

	public synchronized void broadcastMessageInChatroom(String message) {
		printChatroomMessageToConsole("Will broadcast message to all clients in chatroom as follows: " + message);

		for (ClientConnectionObject record : listOfConnectedClients) {
			if (record.getSocket() != null) {
				try {
					printChatroomMessageToConsole("Sending...");
					record.getSocketOutputStream().print(message);
					record.getSocketOutputStream().flush();
				} catch (Exception e) {
					printChatroomMessageToConsole("Failed to broadcast to client at socket "
							+ record.getSocket().getInetAddress().toString());
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

	public List<ClientConnectionObject> getListOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}

	public Integer getChatroomRef() {
		return this.chatroomRef;
	}
}
