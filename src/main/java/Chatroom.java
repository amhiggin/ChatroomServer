package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.util.ArrayList;
import java.util.List;

public class Chatroom {

	private List<ClientNode> listOfConnectedClients;
	private int chatroomId;
	private ServerSocket chatroomSocket;

	private PrintStream socketPrintStream = null;
	private BufferedReader socketInputStream = null;

	public Chatroom(int id) throws IOException {
		this.chatroomId = id;
		this.chatroomSocket = new ServerSocket(id); // TODO make this better
		this.listOfConnectedClients = new ArrayList<ClientNode>();
	}

	public void removeClientNode(ClientNode node) throws Exception {
		if (listOfConnectedClients.contains(node)) {
			listOfConnectedClients.remove(node);
			// TODO how to unbind the connection from the chatroom
			broadcastMessageInChatroom(String.format("%s has left chatroom %s", node.getName(), this.chatroomId));
			return;
		}
		throw new Exception(String.format("Client %s is not a member of chatroom %s", node.getName(), this.chatroomId));
	}

	public void addClientNode(ClientNode node) throws Exception {
		if (!listOfConnectedClients.contains(node)) {
			listOfConnectedClients.add(node);
			this.chatroomSocket.bind(node.getConnection().getRemoteSocketAddress());
			broadcastMessageInChatroom(String.format("%s has joined chatroom %s", node.getName(), this.chatroomId));
			return;
		}
		throw new Exception(String.format("Client %s already added to chatroom %s", node.getName(), this.chatroomId));
	}

	// TODO @Amber - see if this can be used to send messages in general, in the
	// chatroom
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

	public List<ClientNode> getListOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public Integer getChatroomId() {
		return this.chatroomId;
	}
}
