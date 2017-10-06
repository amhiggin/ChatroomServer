package test.java;

import static org.junit.Assert.assertTrue;

import org.junit.Test;

import main.java.ChatroomServer;

public class ServerTest {

	@Test
	public void testClassifyRequestFromClient() {
		// TODO
	}

	@Test(expected = Exception.class)
	public void testIsValidConnection() {
		ChatroomServer server = new ChatroomServer();

		String request = "Join";
		assertTrue("Join is a valid request", server.isValidConnection(request));

		request = "Party";
		server.isValidConnection(request);
	}

}
