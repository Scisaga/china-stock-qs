package org.tfelab.proxy_hub.common;

import org.tfelab.json.JSONable;

import java.io.Serializable;

public class UserSecret implements Serializable {

	private static final long serialVersionUID = 1L;
	public String user;
	public String secret;

	public UserSecret(String user, String secret) {
		this.user = user;
		this.secret = secret;
	}
}
