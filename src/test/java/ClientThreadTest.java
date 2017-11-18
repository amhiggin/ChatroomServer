package test.java;

import static org.junit.Assert.assertTrue;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import main.java.ChatroomServer;
import main.java.ClientThread;

public class ClientThreadTest {

	public TestConstants constants = new TestConstants();

	@Before
	public void setUp() throws Exception {
		ChatroomServer.initialiseServer(TestConstants.TEST_PORT);
	}

	@After
	public void tearDown() {
		ChatroomServer.shutdown();
	}

	@Test(expected = Exception.class)
	public void testNullClientSocketHandledCorrectly() {
		ClientThread thread = new ClientThread(null);
		thread.run();
	}

	@Test(expected = Exception.class)
	public void testInvalidRequestHandledCorrectly() {
		ClientThread thread = new ClientThread(constants.mockClientSocket("GOODBYE"));
		thread.start();
	}

	@Test(expected = Exception.class)
	public void testNullClientSocketInputStreamHandledCorrectly() {
		ClientThread thread = new ClientThread(constants.mockClientSocket(null));
		thread.start();
	}

	@Test
	public void testIfDisconnectingClientWorksCorrectly() throws Exception {
		// First test if the join thread is handled correctly
		ClientThread thread = new ClientThread(constants.mockJoinClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for join request");
		}

		// Now test if the same client can leave the chatroom
		thread = new ClientThread(constants.mockDisconnectClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for disconnect request");
		}

		assertTrue("Client node no longer exists in server records",
				!ChatroomServer.getAllConnectedClients().contains(constants.mockJoinClientConnectionObject));
	}

	// public RequestNode(String clientName, String chatroomId, List<String>
	// receivedFromClient,
	// ClientRequest requestType) {
	@Test
	public void testPopulatedClientRequestNodesHandledCorrectly() throws Exception {
		// First test if the join thread is handled correctly
		ClientThread thread = new ClientThread(constants.mockJoinClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for join request");
		}

		// Now see if that same node can chat
		thread = new ClientThread(constants.mockChatClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for chat request");
		}

		// Now see if that thread can leave the chatroom node = new
		thread = new ClientThread(constants.mockLeaveClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for leave request");
		}

		// Now see that the server retains a record of the client
		assertTrue("Client record still kept with server after leaving chatroom",
				ChatroomServer.getAllConnectedClients().contains(constants.mockJoinClientConnectionObject));

		// Now see if after leaving, the "leave" message is still sent to the
		// client
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for leave request after already leaving, but not disconnecting");
		}
	}

	@Test
	public void testHelloRequest() {
		ClientThread thread = new ClientThread(constants.mockHeloClientSocket);
		try {
			thread.start();
		} catch (Exception e) {
			Assert.fail("Fully executed thread for hello request");
		}
	}

}
