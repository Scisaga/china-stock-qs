package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSON;

public class LoginMsg extends Msg {

	private String secrets;

	public LoginMsg(String clientId) {
		super(clientId);
		this.setType(Type.Login);
	}

	public String getSecrets() {
		return secrets;
	}

	public void setSecrets(String secrets) {
		this.secrets = secrets;
	}
}