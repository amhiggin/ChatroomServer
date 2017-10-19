package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.concurrent.ConcurrentSkipListSet;

public class Chatroom {

	private ConcurrentSkipListSet<ClientNode> listOfConnectedClients;
	private String chatroomId;
	private ServerSocket chatroomSocket;

	private PrintStream socketPrintStream = null;
	private BufferedReader socketInputStream = null;

	public Chatroom(String id) throws IOException {
		this.chatroomId = id;
		this.chatroomSocket = new ServerSocket(Integer.parseInt(id));
		this.listOfConnectedClients = new ConcurrentSkipListSet<ClientNode>();
	}

	public void removeClientNodeAndInformOtherMembers(ClientNode node) throws Exception {
		if (listOfConnectedClients.contains(node)) {
			listOfConnectedClients.remove(node);
			// TODO how to unbind the connection from the chatroom
			broadcastMessageInChatroom(String.format("%s has left chatroom %s", node.getName(), this.chatroomId));
			return;
		}
		throw new Exception(String.format("Client %s is not a member of chatroom %s", node.getName(), this.chatroomId));
	}

	public void addClientNodeAndNotifyMembersOfChatroom(ClientNode node) throws Exception {
		if (!listOfConnectedClients.contains(node)) {
			listOfConnectedClients.add(node);
			this.chatroomSocket.bind(node.getConnection().getRemoteSocketAddress());
			broadcastMessageInChatroom(String.format("%s has joined chatroom %s", node.getName(), this.chatroomId));
			return;
		}
		throw new Exception(String.format("Client %s already added to chatroom %s", node.getName(), this.chatroomId));
	}

	// TODO @Amber 19/10/17 - assuming for now that this is how chat also
	// happens
	public void broadcastMessageInChatroom(String message) throws IOException {
		for (ClientNode client : listOfConnectedClients) {
			// TODO review whether this is the correct way to do this
			socketPrintStream = new PrintStream(client.getConnection().getOutputStream());
			socketPrintStream.print(message);
		}
	}

	public ServerSocket getChatroomSocket() {
		return this.chatroomSocket;
	}

	public ConcurrentSkipListSet<ClientNode> getListOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}
}
