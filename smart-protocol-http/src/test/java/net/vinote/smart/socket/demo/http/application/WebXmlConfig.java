package net.vinote.smart.socket.demo.http.application;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;

import net.vinote.smart.socket.logger.RunLogger;

import com.sun.java.xml.ns.javaee.FilterMappingType;
import com.sun.java.xml.ns.javaee.FilterType;
import com.sun.java.xml.ns.javaee.ListenerType;
import com.sun.java.xml.ns.javaee.ServletMappingType;
import com.sun.java.xml.ns.javaee.ServletType;
import com.sun.java.xml.ns.javaee.WebAppType;

public class WebXmlConfig {
	private List<ServletType> servletList = new ArrayList<ServletType>();
	private List<ServletMappingType> servletMappingList = new ArrayList<ServletMappingType>();
	private List<FilterType> filterList = new ArrayList<FilterType>();
	private List<FilterMappingType> filterMappingList = new ArrayList<FilterMappingType>();
	private List<ListenerType> listenerList = new ArrayList<ListenerType>();

	public WebXmlConfig(String file) throws JAXBException {
		JAXBContext ctx = JAXBContext.newInstance(WebAppType.class);
		Unmarshaller um = ctx.createUnmarshaller();
		@SuppressWarnings("unchecked")
		JAXBElement<WebAppType> stu = (JAXBElement<WebAppType>) um
				.unmarshal(new File(file));
		List<JAXBElement<?>> list = stu.getValue()
				.getDescriptionAndDisplayNameAndIcon();

		for (JAXBElement<?> ele : list) {
			Object obj = ele.getValue();
			if (obj instanceof FilterType) {
				FilterType type = (FilterType) obj;
				filterList.add(type);
			} else if (obj instanceof FilterMappingType) {
				FilterMappingType type = (FilterMappingType) obj;
				filterMappingList.add(type);
			} else if (obj instanceof ServletType) {
				ServletType servlet = (ServletType) obj;
				servletList.add(servlet);
			} else if (obj instanceof ServletMappingType) {
				ServletMappingType type = (ServletMappingType) obj;
				servletMappingList.add(type);
			} else if (obj instanceof ListenerType) {
				ListenerType type = (ListenerType) obj;
				listenerList.add(type);
			} else {
				RunLogger.getLogger().log(Level.SEVERE,
						ele.getName().toString());
			}
		}
	}

	public final List<ServletType> getServletList() {
		return servletList;
	}

	public final List<ServletMappingType> getServletMappingList() {
		return servletMappingList;
	}

	public final List<FilterType> getFilterList() {
		return filterList;
	}

	public final List<FilterMappingType> getFilterMappingList() {
		return filterMappingList;
	}

	public final List<ListenerType> getListenerList() {
		return listenerList;
	}

}
