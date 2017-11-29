package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSON;
import org.tfelab.json.JSONable;

import java.io.Serializable;

public abstract class Msg implements JSONable<Msg>, Serializable {

	private static final long serialVersionUID = 1L;
	private String clientId;
	private Type type;

	public Msg(String clientId) {
		this.clientId = clientId;
	}

	public String getClientId() {
		return clientId;
	}

	public void setClientId(String clientId) {
		this.clientId = clientId;
	}

	public void setType(Type type) {
		this.type = type;
	}

	public Type getType() {
		return this.type;
	}

	public static enum Type {
		Login, Ask, Ping, Reply
	}

	public String toJSON() {
		return JSON.toJson(this);
	}
}
