package main.java;

import java.net.Socket;

public class ClientThread extends Thread {

	private int chatRoomNumber;
	private Socket clientSocket;

	public ClientThread(Socket clientSocket, int chatRoomRequested) {
		this.clientSocket = clientSocket;
		this.chatRoomNumber = chatRoomRequested;
	}

	public int getChatRoomNumber() {
		return this.chatRoomNumber;
	}

	public Socket getClientSocket() {
		return this.clientSocket;
	}

	@Override
	public void run() {
		// TODO
	}

}
