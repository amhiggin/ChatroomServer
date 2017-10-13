package main.java;

public enum Request {
	HelloText("HELO text\n"), JoinChatroom("JOIN_CHATROOM"), Chat("CHAT"), LeaveChatroom("LEAVE_CHATROOM"), KillService(
			"KILL_SERVICE"), Disconnect("DISCONNECT");

	private String value;

	Request(final String value) {
		this.value = value;
	}

	public String getValue() {
		return this.value;
	}
}
