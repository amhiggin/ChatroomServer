package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentSkipListMap;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatroomServer {

	private static final int SERVER_SOCKET = 15432;
	private static AtomicInteger numThreads;
	private static ConcurrentSkipListMap<Socket, ClientThread> threadList;
	private static ServerSocket serverSocket;

	public static void main(String[] args) {
		try {
			initialiseServer();
			while (true) {
				try {
					handleIncomingConnection();
				} catch (Exception e) {
					System.out.println(String.format("Error handling connection: %s", e));
				}
			}
		} catch (Exception e) {
			System.out.println(String.format("Error while initialising server: %s", e));
		} finally {
			shutdown();
		}
	}

	private static void shutdown() {
		try {
			// TODO figure out what needs to happen to close the server
		} catch (Exception e) {
			// TODO if something goes wrong, need to be able to exit
			// Also display a message to our user to inform there was an issue
		}
	}

	private static ClientThread spawnNewClientThreadIfAppropriate(Request requestedAction, Socket clientSocket) {
		switch (requestedAction) {
		case Join:
			return new ClientThread(0);
		case Chat:
			// Want to find the thread in our list, corresponding to this
			// List of threads should be indexed by socket
			return threadList.get(clientSocket);
		case Leave:
			// Want to find the thread in our list, corresponding to this
			return null;
		default:
			return null;
		}
	}

	public static Request requestedAction(Socket clientSocket) throws IOException {
		String requestType = parseClientRequest(clientSocket);
		try {
			return Request.valueOf(requestType);
		} catch (Exception e) {
			return null;
		}
	}

	private static void handleIncomingConnection() throws Exception {
		Socket clientSocket = serverSocket.accept();
		Request requestedAction = requestedAction(clientSocket);
		if (requestedAction == null) {
			throw new Exception("Invalid connection request");
		}
		// it is a valid connection
		ClientThread clientThread = spawnNewClientThreadIfAppropriate(requestedAction, clientSocket);
		recordThreadChangeWithServer(clientThread, requestedAction, clientSocket);

		// TODO - what the client does at this point is dependent on what client
		// is communicating
		clientThread.run();
	}

	private static void recordThreadChangeWithServer(ClientThread clientThread, Request requestedAction,
			Socket clientSocket) {
		// This method should update the arraylist and the number of threads
		// running
		if (requestedAction.equals(Request.Join) && !threadList.containsKey(clientSocket)) {
			numThreads.getAndIncrement();
			threadList.put(clientSocket, clientThread);
		} else if (requestedAction.equals(Request.Leave)) {
			// it is a leave request
			threadList.remove(clientThread);
		}
		// If a chat request, no need to change the threads
	}

	private static String parseClientRequest(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		// Assume that the request is in the first line
		// TODO check that the splitting is correct
		String clientSentence = inFromClient.readLine();
		String[] request = clientSentence.split("_", 1);
		return request[0];
	}

	private static void initialiseServer() throws IOException {
		initialiseThreadVariables();
		serverSocket = new ServerSocket(SERVER_SOCKET);
		System.out.println(String.format("Server listening on port %s", SERVER_SOCKET));
	}

	private static void initialiseThreadVariables() {
		numThreads = new AtomicInteger(0);
		threadList = new ConcurrentSkipListMap<Socket, ClientThread>();
	}

	public AtomicInteger getNumThreads() {
		return numThreads;
	}

	public ConcurrentSkipListMap<Socket, ClientThread> getListOfThreads() {
		return threadList;
	}

}
