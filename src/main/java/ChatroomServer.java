package main.java;

import java.io.IOException;
import java.net.ServerSocket;
import java.sql.Connection;
import java.util.ArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatroomServer {

	private static AtomicInteger numThreads;
	private static ArrayList<Thread> threadList;

	public static void main(String[] args) {
		try {
			initialiseServer();

			while (true) {
				try {
					Connection clientConnection = acceptIncomingConnection();
					String typeRequest = classifyRequestFromClient(clientConnection);
					ClientThread clientThread = spawnNewClientThread(typeRequest);
					clientThread.run();
				} catch (Exception e) {
					// Exception handling
				}
			}
		} catch (Exception e) {
			// TODO error handing
		} finally {
			shutdown();
		}
	}

	static String classifyRequestFromClient(Connection clientConnection) throws RequestNotFoundException {
		// TODO
		return null;

	}

	private static void shutdown() {
		try {

		} catch (Exception e) {
			// figure out what went wrong and deal with it
		}
	}

	private static ClientThread spawnNewClientThread(String typeRequest) {
		if (isValidConnection(typeRequest)) {
			// TODO - may not be correct architecture
		}
		return null;
	}

	public static boolean isValidConnection(String requestType) {
		Request request = Request.valueOf(requestType);
		switch (request) {
		case Join:
			return true;
		case Chat:
			return true;
		case Leave:
			return true;
		default:
			return false;
		}
	}

	private static Connection acceptIncomingConnection() {
		verifyConnectionIsAllowed();
		return null;
	}

	private static void verifyConnectionIsAllowed() {
		// TODO Auto-generated method stub

	}

	private static void initialiseServer() throws IOException {
		initialiseThreadVariables();

		ServerSocket socket = new ServerSocket(15432);
		System.out.println("Server listening on port 15432");
		// Initialise port to listen on
		// Do any necessary initialisation
	}

	private static void initialiseThreadVariables() {
		numThreads = new AtomicInteger(0);
		threadList = new ArrayList<Thread>();
	}

}
