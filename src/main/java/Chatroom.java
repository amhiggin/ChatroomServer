package main.java;

import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.ConcurrentSkipListSet;

public class Chatroom {

	private ConcurrentSkipListSet<ClientNode> listOfConnectedClients;
	private String chatroomId;

	public Chatroom(String id) throws IOException {
		this.chatroomId = id;
		this.listOfConnectedClients = new ConcurrentSkipListSet<ClientNode>();
	}

	public void removeClientNodeAndInformOtherMembers(ClientNode node) throws Exception {
		if (this.listOfConnectedClients.contains(node)) {
			this.listOfConnectedClients.remove(node);
			broadcastMessageInChatroom(String.format("Client %s has left the chatroom", node.getName()));
		}
	}

	public void addNewClientToChatroomAndNotifyMembers(ClientNode node) throws Exception {
		if (!listOfConnectedClients.contains(node)) {
			listOfConnectedClients.add(node);
			broadcastMessageInChatroom(String.format(ServerResponse.JOIN.getValue(), this.chatroomId,
					Constants.SERVER_IP, ChatroomServer.getServerPort(), this.chatroomId, node.getJoinId()));
			return;
		}
		throw new Exception(String.format("Client %s already added to chatroom %s", node.getName(), this.chatroomId));
	}

	// Synchronized to account for completion of message sending before allowing
	// client to leave chatroom
	public synchronized void broadcastMessageInChatroom(String message) throws IOException {
		for (ClientNode client : listOfConnectedClients) {
			PrintStream socketPrintStream = new PrintStream(client.getConnection().getOutputStream());
			socketPrintStream.print(message);
		}
	}

	public ConcurrentSkipListSet<ClientNode> getSetOfConnectedClients() {
		return this.listOfConnectedClients;
	}

	public String getChatroomId() {
		return this.chatroomId;
	}
}
