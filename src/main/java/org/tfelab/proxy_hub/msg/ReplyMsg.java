package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSONable;

public class ReplyMsg extends Msg {

	private JSONable body;

	public ReplyMsg(String clientId) {
		super(clientId);
	}

	public void setBody(JSONable body) {
		this.body = body;
	}

	public JSONable getBody() {
		return this.body;
	}
}
