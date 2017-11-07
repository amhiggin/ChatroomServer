package main.java;

import java.io.PrintStream;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class Chatroom implements Comparable<Chatroom> {

	private ConcurrentSkipListSet<ClientNode> listOfConnectedClients;
	private String chatroomId;
	private Integer chatroomRef;

	public Chatroom(String id, AtomicInteger chatroomRef) {
		this.chatroomId = id;
		this.listOfConnectedClients = new ConcurrentSkipListSet<ClientNode>();
		this.chatroomRef = Integer.valueOf(chatroomRef.intValue());
	}

	public void removeClientNodeAndInformOtherMembers(ClientNode node) {
		ChatroomServer.printMessageToConsole(
				String.format("Removing node %s from chatroom %s", node.getName(), this.chatroomId));
		if (this.listOfConnectedClients.contains(node)) {
			this.listOfConnectedClients.remove(node);
			broadcastMessageInChatroom(String.format("Client %s has left the chatroom", node.getName()));
		}
	}

	public void addNewClientToChatroomAndNotifyMembers(ClientNode node) throws Exception {
		ChatroomServer.printMessageToConsole(
				String.format("Adding new node %s to chatroom %s", node.getName(), this.chatroomId));
		if (!listOfConnectedClients.contains(node)) {
			listOfConnectedClients.add(node);
			broadcastMessageInChatroom(String.format(ServerResponse.JOIN.getValue(), this.chatroomId,
					ChatroomServer.serverIP, ChatroomServer.getServerPort(), this.chatroomRef, node.getJoinId()));
			return;
		}
		throw new Exception(String.format("Client %s already added to chatroom %s", node.getName(), this.chatroomId));
	}

	// Synchronized to account for completion of message sending before allowing
	// client to leave chatroom
	public synchronized void broadcastMessageInChatroom(String message) {
		ChatroomServer
				.printMessageToConsole("Will broadcast message to all clients in chatroom as follows: " + message);
		for (ClientNode client : listOfConnectedClients) {
			if (client != null) {
				try {
					PrintStream socketPrintStream = new PrintStream(client.getConnection().getOutputStream());
					socketPrintStream.print(message);
				} catch (Exception e) {
					ChatroomServer.printMessageToConsole("Failed to broadcast to client " + client.getName());
				}
			}
		}
		ChatroomServer.printMessageToConsole("Finished broadcast");
	}

	@Override
	public int compareTo(Chatroom o) {
		if (this.getChatroomRef() < o.getChatroomRef()) {
			return -1;
		} else if (this.getChatroomRef() > o.getChatroomRef()) {
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
