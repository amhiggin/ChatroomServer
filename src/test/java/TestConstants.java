package test.java;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.net.Socket;
import java.util.Arrays;
import java.util.List;

import org.mockito.Mockito;

public class TestConstants {

	public static final String HELLO_HELLO = "Hello hello\n\n";
	public static final String CLIENT_A = "client a";
	// NOTE: should use String.format(msg, args[]) to specify %s in constants
	public static final String JOIN_REQUEST = "JOIN_CHATROOM: %s\n" + "CLIENT_IP: 0.0.0.0\n" + "PORT: 0\n"
			+ "CLIENT_NAME: %s";
	public static final String LEAVE_REQUEST = "LEAVE_CHATROOM: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s";
	public static final String JOINED_CHATROOM = "JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n"
			+ "ROOM_REF: %s\n" + "JOIN_ID: %s";
	public static final String CHAT_REQUEST = "CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE: %s";
	public static final String DISCONNECT_REQUEST = "DISCONNECT: 0\n" + "PORT: 0\n" + "CLIENT_NAME: %s";
	public static final String HELO_REQUEST = "HELO %s";

	public static final List<String> mockClientJoinRequest = Arrays.asList("JOIN_CHATROOM: 1", "CLIENT_IP: 0.0.0.0",
			"PORT: 0", "CLIENT_NAME: client a");
	public static final List<String> mockClientChatRequest = Arrays.asList("CHAT: 1", "JOIN_ID: 1",
			"CLIENT_NAME: client a", "MESSAGE: Hello hello\n\n");
	public static final List<String> mockClientLeaveRequest = Arrays.asList("LEAVE_CHATROOM: 1", "JOIN_ID: 1",
			"CLIENT_NAME: client a");
	public static final List<String> mockClientDisconnectRequest = Arrays.asList("DISCONNECT: 0", "PORT: 0",
			"CLIENT_NAME: client a");
	public static final List<String> mockClientKillServiceRequest = Arrays.asList("KILL SERVICE\n");
	public static final List<String> mockClientHeloRequest = Arrays.asList("HELO Hello hello\n\n");
	public static final String TEST_PORT = "23457";

	public Socket mockJoinClientSocket;
	public Socket mockChatClientSocket;
	public Socket mockHelloClientSocket;
	public Socket mockLeaveClientSocket;
	public Socket mockDisconnectClientSocket;
	public Socket mockKillServiceClientSocket;

	public TestConstants() {
		this.mockJoinClientSocket = mockClientSocket(String.format(JOIN_REQUEST, Integer.valueOf(1), CLIENT_A));
		this.mockChatClientSocket = mockClientSocket(
				String.format(CHAT_REQUEST, Integer.valueOf(1), Integer.valueOf(1), CLIENT_A, HELLO_HELLO));
		this.mockLeaveClientSocket = mockClientSocket(
				String.format(LEAVE_REQUEST, Integer.valueOf(1), Integer.valueOf(1), CLIENT_A));
		this.mockDisconnectClientSocket = mockClientSocket(String.format(DISCONNECT_REQUEST, CLIENT_A));
		this.mockHelloClientSocket = mockClientSocket(HELLO_HELLO);
	}

	public static Socket mockClientSocket(String request) {
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
