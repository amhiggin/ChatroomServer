package main.java;

import java.sql.Connection;

public class ChatroomServer {

	public static void main(String[] args) {
		initialiseServer();

		while (true) {
			Connection clientConnection = acceptIncomingConnection();
			ClientThread clientThread = spawnNewClientThread(clientConnection);
		}

	}

	private static ClientThread spawnNewClientThread(Connection clientConnection) {
		// TODO Auto-generated method stub
		return null;
	}

	private static Connection acceptIncomingConnection() {
		// TODO Auto-generated method stub

	}

	private static void initialiseServer() {
		// TODO Auto-generated method stub

	}

}
