package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;

import java.io.Serializable;

public class AskMsg extends Msg {

	private Type askType;
	private String token;

	private Serializable body;

	public AskMsg(String clientId, Type type) {
		super(clientId);
		this.setType(Msg.Type.Ask);
		this.askType = type;
	}

	public static enum Type {
		RequireLogin,
		NodeInfo,
		Pon,
		ChangeUserAndSecrets
	}

	public Type getAskType() {
		return askType;
	}

	public void setBody(Serializable body) {
		this.body = body;
	}

	public Serializable getBody() {
		return body;
	}
}
