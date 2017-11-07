package main.java;

public enum ServerResponse {
	/*
	 * To be used with String.format(msg, args[])
	 */

	JOIN("JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n" + "ROOM_REF: %s\n" + "JOIN_ID: %s\n"), LEAVE(
			"LEFT_CHATROOM: %s\n" + "JOIN_ID: %s\n"), CHAT(
					"CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE: %s\n\n"), DISCONNECT(
							"DISCONNECT: %s\n" + "PORT: %s\n" + "CLIENT_NAME: %s\n"), HELO(
									"HELO %s\n" + "IP: %s\n" + "Port: %s\n" + "StudentID: %s\n"), ERROR(
											"ERROR_CODE: %s\n" + "ERROR_DESCRIPTION: %s\n");

	private String value;

	ServerResponse(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
