package main.java;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.atomic.AtomicInteger;

public class ChatroomServer {

	private static final int SERVER_SOCKET = 15432; // TODO possibly update
	private static AtomicInteger numThreads;
	private static ConcurrentSkipListSet<Socket> activeClientList;
	private static ServerSocket serverSocket;
	private static Boolean terminateServer;

	public static void main(String[] args) {
		try {
			initialiseServer();
			while (true && !terminateServer.equals(Boolean.TRUE)) {
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
			recordClientChangeWithServer(Request.KillService, null);
			serverSocket.close();
		} catch (Exception e) {
			// TODO if something goes wrong, need to be able to exit
			e.printStackTrace();
		}
	}

	private static ClientThread spawnNewClientThread(Request requestedAction, Socket clientSocket) throws Exception {
		if (requestedAction.equals(Request.JoinChatroom) || requestedAction.equals(Request.HelloText)) {
			return new ClientThread(0, requestedAction, clientSocket);
		}
		throw new Exception("Can't spawn new thread if not a join or hello request");
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
		if (requestedAction.equals(Request.JoinChatroom) || requestedAction.equals(Request.HelloText)) {
			ClientThread clientThread = spawnNewClientThread(requestedAction, clientSocket);
			clientThread.run();
		}
		recordClientChangeWithServer(requestedAction, clientSocket);
	}

	private static void recordClientChangeWithServer(Request requestedAction, Socket clientSocket) throws IOException {
		if (requestedAction.equals(Request.JoinChatroom) && !activeClientList.contains(clientSocket)) {
			activeClientList.add(clientSocket);
			numThreads.getAndIncrement();
		} else if (requestedAction.equals(Request.LeaveChatroom)) {
			activeClientList.remove(clientSocket);
			numThreads.getAndDecrement();
		} else if (requestedAction.equals(Request.KillService)) {
			for (Socket socket : activeClientList) {
				socket.close();
				activeClientList.remove(socket);
			}
		}
		// If a chat or hello request, no need to alter the active connections
	}

	private static String parseClientRequest(Socket clientSocket) throws IOException {
		BufferedReader inFromClient = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()));
		// FIXME (potentially) Assume that the request is in the first line
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
		activeClientList = new ConcurrentSkipListSet<Socket>();
		terminateServer = Boolean.FALSE;
	}

	public AtomicInteger getNumThreads() {
		return numThreads;
	}

	public ConcurrentSkipListSet<Socket> getListOfThreads() {
		return activeClientList;
	}

}
