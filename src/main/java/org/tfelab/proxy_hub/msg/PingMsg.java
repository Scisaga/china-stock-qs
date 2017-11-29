package org.tfelab.proxy_hub.msg;

import org.tfelab.json.JSON;

public class PingMsg extends Msg{

	public PingMsg(String clientId) {
		super(clientId);
	}
}
