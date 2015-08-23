package net.vinote.smart.socket.protocol.p2p.server;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class RemoteModel implements Serializable {
	private String name;
	private List<String> list = new ArrayList<String>();
	{
		list.add("zjw");
		list.add("zj1w");
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
		list.add(name);
	}

	public List<String> getList() {
		return list;
	}

	public void setList(List<String> list) {
		this.list = list;
	}

}
