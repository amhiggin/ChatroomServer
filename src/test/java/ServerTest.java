package test.java;

import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.Socket;

import org.junit.Test;
import org.mockito.Mockito;

import main.java.ChatroomServer;
import main.java.ClientNode;
import main.java.ClientRequest;

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
	public void testExtractClientInfoFromRequestOperatesCorrectly() throws Exception {
		ChatroomServer.initialiseServer("23456");

		// JOIN request
		String request = TestConstants.JOIN_REQUEST;
		String fullRequest = String.format(request, Integer.valueOf(1), "client a");
		Socket mockClientSocket = mockClientSocket(fullRequest);
		ClientNode mockClientNode = new ClientNode(mockClientSocket, "client a", "1", 0);
		assertTrue("Client node info was parsed correctly from join request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.JOIN_CHATROOM), mockClientNode));

		// CHAT request: doesn't test that the message is constructed correctly
		request = TestConstants.CHAT_REQUEST;
		fullRequest = String.format(request, Integer.valueOf(1), ChatroomServer.clientId.get(), "client a",
				"Hello hello\n\n");
		mockClientSocket = mockClientSocket(fullRequest);
		mockClientNode = new ClientNode(mockClientSocket, "client a", "1", ChatroomServer.clientId.get());
		assertTrue("Client node info was parsed correctly from chat request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.CHAT), mockClientNode));

		// LEAVE request
		request = TestConstants.LEAVE_REQUEST;
		fullRequest = String.format(request, Integer.valueOf(1), ChatroomServer.clientId.get(), "client a");
		mockClientSocket = mockClientSocket(fullRequest);
		mockClientNode = new ClientNode(mockClientSocket, "client a", "1", ChatroomServer.clientId.get());
		assertTrue("Client node info was parsed correctly from leave request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.LEAVE_CHATROOM), mockClientNode));

		// DISCONNECT request
		request = TestConstants.DISCONNECT_REQUEST;
		fullRequest = String.format(request, "client a");
		mockClientSocket = mockClientSocket(fullRequest);
		mockClientNode = new ClientNode(mockClientSocket, "client a", null, -1);
		assertTrue("Client node info was parsed correctly from disconnect request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.DISCONNECT), mockClientNode));
	}

	private boolean clientNodesMatch(ClientNode extractClientInfo, ClientNode mockClientNode) throws IOException {
		if (extractClientInfo.getConnection().getInputStream() != mockClientNode.getConnection().getInputStream()) {
			return false;
		}
		if (!(extractClientInfo.getName().equals(mockClientNode.getName()))) {
			return false;
		}
		if ((!(extractClientInfo.getChatroomId() == null)) && (!(mockClientNode.getChatroomId() == null))) {
			if (!(extractClientInfo.getChatroomId().equals(mockClientNode.getChatroomId()))) {
				return false;
			}
		} else {
			if (extractClientInfo.getChatroomId() != mockClientNode.getChatroomId()) {
				return false;
			}
		}
		if (!extractClientInfo.getJoinId().equals(mockClientNode.getJoinId())) {
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
