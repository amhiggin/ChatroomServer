package main.java;

public class ClientThread extends Thread {

	private int chatRoomNumber;

	public ClientThread(int chatRoomRequested) {
		this.chatRoomNumber = chatRoomRequested;
	}

	public int getChatRoomNumber() {
		return this.chatRoomNumber;
	}

	@Override
	public void run() {
		// TODO
	}

}
