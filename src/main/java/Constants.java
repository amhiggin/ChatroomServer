package main.java;

public class Constants {
	public static final String STUDENT_ID = "13327954";
	public static final String SERVER_IP = null;

	/*
	 * The following Constants should be used with String.format(msg, args[])
	 */
	public static final String JOINED_CHATROOM = "JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n"
			+ "ROOM_REF: %s\n" + "JOIN_ID: %s";

	public static final String DISCONNECT_RESPONSE = "DISCONNECT: %s\n" + "PORT: %s\n" + "CLIENT_NAME: %s";

	public static final String LEFT_CHATROOM_RESPONSE = "LEFT_CHATROOM: %s\n" + "JOIN_ID: %s";

}
