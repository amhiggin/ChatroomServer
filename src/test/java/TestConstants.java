package test.java;

public class TestConstants {

	/*
	 * NOTE: should use String.format(msg, args[]) to specify %s in constants
	 */
	public static final String JOIN_REQUEST = "JOIN_CHATROOM: %s\n" + "CLIENT_IP: 0.0.0.0\n" + "PORT: 0\n"
			+ "CLIENT_NAME: %s";

	public static final String JOINED_CHATROOM = "JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n"
			+ "ROOM_REF: %s\n" + "JOIN_ID: %s";

	public TestConstants() {
		// TODO Auto-generated constructor stub
	}

}
