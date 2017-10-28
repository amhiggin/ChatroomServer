package test.java;

import static org.junit.Assert.assertTrue;

import java.util.Arrays;

import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import main.java.ChatroomServer;
import main.java.ClientNode;
import main.java.ClientRequest;
import main.java.ClientThread;

public class ClientThreadTest {

	public TestConstants constants = new TestConstants();

	@BeforeClass
	public static void setUp() throws Exception {
		ChatroomServer.initialiseServer(TestConstants.TEST_PORT);
	}

	@AfterClass
	public static void tearDown() {
		ChatroomServer.shutdown();
	}

	@Test
	public void testNullClientNodeHandledCorrectly() {
		ClientNode node = null;
		ClientThread thread = new ClientThread(node, null, TestConstants.mockClientKillServiceRequest);
		thread.run();
	}

	@Test
	public void testEmptyClientNodeHandledCorrectly() {
		ClientNode node = new ClientNode(null, null, null, null);
		ClientThread thread = new ClientThread(node, ClientRequest.KILL_SERVICE,
				TestConstants.mockClientKillServiceRequest);
		thread.run();
	}

	@Test
	public void testPopulatedClientRequestNodesHandledCorrectly() throws Exception {
		// First test if the join thread is handled correctly
		ClientNode node = new ClientNode(constants.mockJoinClientSocket, TestConstants.CLIENT_A, "1", 1);
		ClientThread thread = new ClientThread(node, ClientRequest.JOIN_CHATROOM, TestConstants.mockClientJoinRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for join request");
		}

		// Now see if that same node can chat
		node = new ClientNode(constants.mockChatClientSocket, TestConstants.CLIENT_A, "1", 1);
		thread = new ClientThread(node, ClientRequest.CHAT, TestConstants.mockClientChatRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for chat request");
		}

		// Now see if that thread can leave the chatroom node = new
		node = new ClientNode(constants.mockLeaveClientSocket, TestConstants.CLIENT_A, "1", 1);
		thread = new ClientThread(node, ClientRequest.LEAVE_CHATROOM, TestConstants.mockClientLeaveRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for leave request");
		}

		// Now see that the server retains a record of the client
		assertTrue("Client record still kept with server after leaving chatroom",
				ChatroomServer.getAllConnectedClients().contains(node));

		// Now see if after leaving, the "leave" message is still sent to the
		// client
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for leave request after already leaving, but not disconnecting");
		}
	}

	@Test
	public void testIfDisconnectingClientWorksCorrectly() throws Exception {
		// First test if the join thread is handled correctly
		ClientNode node = new ClientNode(constants.mockJoinClientSocket, TestConstants.CLIENT_A, "1", 1);
		ClientThread thread = new ClientThread(node, ClientRequest.JOIN_CHATROOM, TestConstants.mockClientJoinRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for join request");
		}

		// Now test if the same client can leave the chatroom
		node = new ClientNode(constants.mockDisconnectClientSocket, TestConstants.CLIENT_A, "1", 1);
		thread = new ClientThread(node, ClientRequest.DISCONNECT, TestConstants.mockClientDisconnectRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for disconnect request");
		}

		assertTrue("Client node no longer exists in server records",
				!ChatroomServer.getAllConnectedClients().contains(node));
	}

	@Test
	public void testHelloRequest() {
		ClientNode node = new ClientNode(constants.mockHelloClientSocket, null, null, -1);
		ClientThread thread = new ClientThread(node, ClientRequest.HELO, TestConstants.mockClientHeloRequest);
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for hello request");
		}
	}

	@Test
	public void testErrorHandlingOfInvalidRequest() {
		ClientNode node = new ClientNode(constants.mockHelloClientSocket, "Toto was here", "Hello there friend", -15);
		ClientThread thread = new ClientThread(node, null, Arrays.asList("Toto was here"));
		try {
			thread.run();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for hello request");
		}
	}

}
