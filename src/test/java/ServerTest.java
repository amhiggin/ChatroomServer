package test.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.mockito.Mockito;

import main.java.ChatroomServer;
import main.java.ClientNode;

public class ServerTest {

	@Test
	public void testIsValidRequest() {
		String request = TestConstants.JOIN_REQUEST;
		try {
			assertTrue("JOIN_CHATROOM is a valid input",
					ChatroomServer.requestedAction(mockClientSocket(request)) != null);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}

		request = "PARTY";
		try {
			assertTrue("PARTY is not a valid input", ChatroomServer.requestedAction(mockClientSocket(request)) == null);
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	@Test
	public void testExtractClientInfoFromRequestOperatesCorrectly() {
		String request = TestConstants.JOIN_REQUEST;
		String fullRequest = String.format(request, Integer.valueOf(1), "client a");

		Socket mockClientSocket = mockClientSocket(fullRequest);
		ClientNode mockClientNode = new ClientNode(mockClientSocket, "client a", "1");
		try {
			assertTrue("Client node info was parsed correctly from join request",
					matches(ChatroomServer.extractClientInfo(mockClientSocket), mockClientNode));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	private boolean matches(ClientNode extractClientInfo, ClientNode mockClientNode) throws IOException {
		if (extractClientInfo.getChatroomId() != mockClientNode.getChatroomId()) {
			return false;
		} else if (extractClientInfo.getConnection().getInputStream() != mockClientNode.getConnection()
				.getInputStream()) {
			return false;
		} else if (extractClientInfo.getName() != mockClientNode.getName()) {
			return false;
		}
		return true;
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
