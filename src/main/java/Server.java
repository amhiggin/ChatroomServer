package main.java;

import java.sql.Connection;

public class Server {

	boolean active;

	public static void main(String[] args) {
		initialiseServer();

		while (true) {
			Connection clientConnection = acceptConnection();
			dealWithClientConnectionAppropriately(clientConnection);
		}

		killServer();
	}

	private static void killServer() {
	}

	private static void dealWithClientConnectionAppropriately(Connection clientConnection) {
	}

	private static Connection acceptConnection() {
		Thread newClientThread = spawnNewThread();
	}

	private void Thread

	spawnNewThread() {

	}

	private static void initialiseServer() {

	}

}
