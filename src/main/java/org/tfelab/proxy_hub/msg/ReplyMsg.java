package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSONable;

public class ReplyMsg extends Msg {

	private JSONable body;

	public ReplyMsg(String clientId) {
		super(clientId);
		this.setType(Type.Reply);
	}

	public ReplyMsg(String clientId, JSONable body) {
		super(clientId);
		this.setType(Type.Reply);
		this.body = body;
	}

	public void setBody(JSONable body) {
		this.body = body;
	}

	public JSONable getBody() {
		return this.body;
	}
}
