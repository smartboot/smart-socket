package net.vinote.smart.socket.demo.http.application;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;

import net.vinote.smart.socket.demo.http.filter.SmartServletModel;

public class DeveloperServerApplication extends AbstractServletContext {

	public void registerServlets(Map<String, HttpServlet> map)
			throws ServletException {
		for (Entry<String, HttpServlet> entry : map.entrySet()) {
			servletList.put(entry.getKey(),
					new SmartServletModel(null, entry.getValue(), null, true));
		}
	}

	
	public void init0(String serverPath) {
	}

	
	public void destory0() {
		// TODO Auto-generated method stub

	}

}
