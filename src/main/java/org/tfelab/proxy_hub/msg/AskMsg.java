package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSON;

public class AskMsg extends Msg {

	private Type askType;
	private String token;

	public AskMsg(String clientId, Type type) {
		super(clientId);
		this.setType(Msg.Type.Ask);
		this.askType = type;
	}

	public static enum Type {
		Require_Login
	}

	public Type getAskType() {
		return askType;
	}
}
