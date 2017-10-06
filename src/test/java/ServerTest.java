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
		String request = "Join";
		ChatroomServer server = new ChatroomServer();
		assertTrue("Join is a valid input", server.requestedAction(mockClientSocket(request)) != null);

		request = "Party";
		assertTrue("Party is an invalid input", server.requestedAction(mockClientSocket(request)) == null);

	}

	public Socket mockClientSocket(String desiredInput) throws IOException {
		Socket mockClientSocket = Mockito.mock(Socket.class);
		try {
			Mockito.when(mockClientSocket.getInputStream())
					.thenReturn(new ByteArrayInputStream(desiredInput.getBytes()));
			return mockClientSocket;
		} catch (Exception e) {
			return null;
		}
	}
}
