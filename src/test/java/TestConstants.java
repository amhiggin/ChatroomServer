package test.java;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

import main.java.ClientConnectionObject;

public class TestConstants {

	public static final String HELLO_HELLO = "Hello hello";
	public final String CLIENT_A = "client a";
	// NOTE: should use String.format(msg, args[]) to specify %s in constants
	public static final String JOIN_REQUEST = "JOIN_CHATROOM: %s\n" + "CLIENT_IP: 0.0.0.0\n" + "PORT: 0\n"
			+ "CLIENT_NAME: %s";
	public static final String LEAVE_REQUEST = "LEAVE_CHATROOM: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s";
	public static final String JOINED_CHATROOM = "JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n"
			+ "ROOM_REF: %s\n" + "JOIN_ID: %s";
	public static final String CHAT_REQUEST = "CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE: %s";
	public static final String DISCONNECT_REQUEST = "DISCONNECT: 0\n" + "PORT: 0\n" + "CLIENT_NAME: %s";
	public static final String HELO_REQUEST = "HELO %s";

	public final List<String> mockClientJoinRequest = Arrays.asList("JOIN_CHATROOM: 1", "CLIENT_IP: 0.0.0.0", "PORT: 0",
			"CLIENT_NAME: client a");
	public final List<String> mockClientChatRequest = Arrays.asList("CHAT: 1", "JOIN_ID: 1", "CLIENT_NAME: client a",
			"MESSAGE: Hello hello\n\n");
	public static final List<String> mockClientLeaveRequest = Arrays.asList("LEAVE_CHATROOM: 1", "JOIN_ID: 1",
			"CLIENT_NAME: client a");
	public static final List<String> mockClientDisconnectRequest = Arrays.asList("DISCONNECT: 0", "PORT: 0",
			"CLIENT_NAME: client a");
	public static final List<String> mockClientKillServiceRequest = Arrays.asList("KILL SERVICE\n");
	public static final List<String> mockClientHeloRequest = Arrays.asList("HELO Hello hello");
	public static final String TEST_PORT = "23457";

	public ClientConnectionObject mockJoinClientConnectionObject;
	public ClientConnectionObject mockChatClientConnectionObject;
	public ClientConnectionObject mockHelloClientConnectionObject;
	public ClientConnectionObject mockLeaveClientConnectionObject;
	public ClientConnectionObject mockDisconnectClientConnectionObject;
	public ClientConnectionObject mockKillClientConnectionObject;
	public Socket mockJoinClientSocket;
	public Socket mockLeaveClientSocket;
	public Socket mockChatClientSocket;
	public Socket mockHeloClientSocket;
	public Socket mockDisconnectClientSocket;

	public TestConstants() {
		int id = 0;

		// JOIN
		this.mockJoinClientSocket = mockClientSocket(String.format(JOIN_REQUEST, Integer.valueOf(1), CLIENT_A));
		this.mockJoinClientConnectionObject = getConnectionObject(mockJoinClientSocket, id);

		// CHAT
		this.mockChatClientSocket = mockClientSocket(
				String.format(CHAT_REQUEST, Integer.valueOf(1), Integer.valueOf(1), CLIENT_A, HELLO_HELLO));
		this.mockChatClientConnectionObject = getConnectionObject(this.mockChatClientSocket, id++);

		// LEAVE
		this.mockLeaveClientSocket = mockClientSocket(
				String.format(LEAVE_REQUEST, Integer.valueOf(1), Integer.valueOf(1), CLIENT_A));
		this.mockLeaveClientConnectionObject = getConnectionObject(this.mockLeaveClientSocket, id++);

		// DISCONNECT
		this.mockDisconnectClientSocket = mockClientSocket(String.format(DISCONNECT_REQUEST, CLIENT_A));
		this.mockDisconnectClientConnectionObject = getConnectionObject(this.mockDisconnectClientSocket, id++);

		// HELLO
		this.mockHeloClientSocket = mockClientSocket(HELLO_HELLO);
		this.mockHelloClientConnectionObject = getConnectionObject(this.mockHeloClientSocket, id++);
	}

	private ClientConnectionObject getConnectionObject(Socket mockClientSocket, int id) {
		try {
			return new ClientConnectionObject(mockClientSocket, new PrintWriter(mockClientSocket.getOutputStream()),
					new BufferedInputStream(mockClientSocket.getInputStream()), id);
		} catch (IOException e) {
			System.out.println("Failed to create connection object for mock socket " + id);
			e.printStackTrace();
		}
		return null;
	}

	public Socket mockClientSocket(String request) {
		Socket mockClientSocket = Mockito.mock(Socket.class);
		try {
			Mockito.when(mockClientSocket.getInputStream()).thenReturn(new ByteArrayInputStream(request.getBytes()));
			Mockito.when(mockClientSocket.getOutputStream()).thenReturn(new ByteArrayOutputStream());
			return mockClientSocket;
		} catch (Exception e) {
			return null;
		}
	}

}
