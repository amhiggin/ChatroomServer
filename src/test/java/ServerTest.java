package test.java;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.net.Socket;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import main.java.ChatroomServer;
import main.java.ClientNode;
import main.java.ClientRequest;

public class ServerTest {

	private TestConstants constants = new TestConstants();

	@BeforeClass
	public static void setUp() throws Exception {
		ChatroomServer.initialiseServer(TestConstants.TEST_PORT);
	}

	@AfterClass
	public static void tearDown() {
		ChatroomServer.shutdown();
	}

	@Test
	public void testIsValidRequest() throws Exception {
		String request = TestConstants.JOIN_REQUEST;
		assertTrue("JOIN_CHATROOM is a valid input",
				ChatroomServer.requestedAction(TestConstants.mockClientSocket(request)) != null);

		request = "PARTY";
		assertTrue("PARTY is not a valid input",
				ChatroomServer.requestedAction(TestConstants.mockClientSocket(request)) == null);
	}

	@Test
	public void testExtractClientInfoFromRequestOperatesCorrectly() throws Exception {

		// JOIN request
		Socket mockClientSocket = constants.mockJoinClientSocket;
		ClientNode mockClientNode = new ClientNode(mockClientSocket, "client a", "1", 0);
		assertTrue("Client node info was parsed correctly from join request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.JOIN_CHATROOM), mockClientNode));

		// CHAT request: doesn't test that the message is constructed correctly
		mockClientSocket = constants.mockChatClientSocket;
		mockClientNode = new ClientNode(mockClientSocket, "client a", "1", ChatroomServer.clientId.get());
		assertTrue("Client node info was parsed correctly from chat request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.CHAT), mockClientNode));

		// LEAVE request
		mockClientSocket = constants.mockLeaveClientSocket;
		mockClientNode = new ClientNode(mockClientSocket, "client a", "1", ChatroomServer.clientId.get());
		assertTrue("Client node info was parsed correctly from leave request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.LEAVE_CHATROOM), mockClientNode));

		// DISCONNECT request
		mockClientSocket = constants.mockDisconnectClientSocket;
		mockClientNode = new ClientNode(mockClientSocket, "client a", null, -1);
		assertTrue("Client node info was parsed correctly from disconnect request", clientNodesMatch(
				ChatroomServer.extractClientInfo(mockClientSocket, ClientRequest.DISCONNECT), mockClientNode));
	}

	@Test
	public void testAddClientToServerTwiceIsntPermitted() throws Exception {
		ClientNode node = new ClientNode(constants.mockJoinClientSocket, TestConstants.CLIENT_A, "1", 1);
		ChatroomServer.addClientRecordToServer(node);
		assertTrue("There is one node stored in the server", ChatroomServer.getAllConnectedClients().size() == 1);

		ChatroomServer.addClientRecordToServer(node);
		assertTrue("Duplicate client node isn't added to server", ChatroomServer.getAllConnectedClients().size() == 1);
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

}
