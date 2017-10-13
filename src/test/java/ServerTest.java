package test.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.mockito.Mockito;

import jdk.nashorn.internal.ir.RuntimeNode.Request;
import main.java.ChatroomServer;

public class ServerTest {

	@Test
	public void testIsValidRequest() throws IOException {
		String request = "JoinChatroom";
		ChatroomServer server = new ChatroomServer();
		assertTrue("JoinChatroom is a valid input", server.requestedAction(mockClientSocket(request)) != null);

		request = "Party";
		assertTrue("Party is an invalid input", server.requestedAction(mockClientSocket(request)) == null);

	}

	public Socket mockClientSocket(String desiredInput) throws IOException {
		Socket mockClientSocket = Mockito.mock(Socket.class);
		try {
			Mockito.when(mockClientSocket.getInputStream())
					.thenReturn(new ByteArrayInputStream(Request.valueOf(desiredInput).toString().getBytes()));
			return mockClientSocket;
		} catch (Exception e) {
			return null;
		}
	}
}
