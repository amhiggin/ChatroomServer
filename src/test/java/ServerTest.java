package test.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.mockito.Mockito;

import main.java.ChatroomServer;

public class ServerTest {

	@Test
	public void testIsValidRequest() throws IOException {
		String request = TestConstants.JOIN_REQUEST;
		assertTrue("JOIN_CHATROOM is a valid input", ChatroomServer.requestedAction(mockClientSocket(request)) != null);

		request = "PARTY";
		assertTrue("PARTY is not a valid input", ChatroomServer.requestedAction(mockClientSocket(request)) == null);
	}

	private Socket mockClientSocket(String request) {
		Socket mockClientSocket = Mockito.mock(Socket.class);
		try {
			Mockito.when(mockClientSocket.getInputStream()).thenReturn(new ByteArrayInputStream(request.getBytes()));
			return mockClientSocket;
		} catch (Exception e) {
			return null;
		}
	}
}
