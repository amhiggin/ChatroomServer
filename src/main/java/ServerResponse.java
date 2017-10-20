package main.java;

public enum ServerResponse {
	JOIN("JOINED_CHATROOM: %s\n" + "SERVER_IP: %s\n" + "PORT: %s\n" + "ROOM_REF: %s\n" + "JOIN_ID: %s"), LEAVE(
			"LEFT_CHATROOM: %s\n" + "JOIN_ID: %s"), CHAT(
					"CHAT: %s\n" + "JOIN_ID: %s\n" + "CLIENT_NAME: %s\n" + "MESSAGE: %s\n\n");

	private String value;

	ServerResponse(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}

}
