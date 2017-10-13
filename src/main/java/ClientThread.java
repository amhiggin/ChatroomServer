package main.java;

import java.net.Socket;

public class ClientThread extends Thread {

	private int chatRoomNumber;
	private Socket socket;
	private Request action;

	public ClientThread(int chatRoomRequested, Request requestedAction, Socket clientSocket) {
		// TODO error handling
		this.chatRoomNumber = chatRoomRequested;
		this.action = requestedAction;
		this.socket = clientSocket;
	}

	public int getChatRoomNumber() {
		return this.chatRoomNumber;
	}

	@Override
	public void run() {
		// TODO - what should be done when the thread is run
	}

}
