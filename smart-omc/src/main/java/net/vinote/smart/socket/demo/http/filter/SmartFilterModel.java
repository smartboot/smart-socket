package net.vinote.smart.socket.demo.http.filter;

import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.Filter;

import net.vinote.smart.socket.demo.http.application.URLPatternType;

public class SmartFilterModel {
	private Map<String, URLPatternType> patternMap;
	private Filter filter;

	public SmartFilterModel(Map<String, URLPatternType> patternMap,
			Filter filter) {
		this.filter = filter;
		this.patternMap = patternMap;
	}

	public final Filter getFilter() {
		return filter;
	}

	public final Map<String, URLPatternType> getPatterns() {
		return patternMap;
	}

	public int matches(String url) {
		int index = -1;
		for (Entry<String, URLPatternType> entry : patternMap.entrySet()) {
			switch (entry.getValue()) {
			case AllMatcher:
				if (url.startsWith(entry.getKey())) {
					index = entry.getKey().length();
				}
				break;
			case CatalogMatcher:
			case ExtensionMatcher:
				if (url.matches(entry.getKey())) {
					index = url.length();
				}
				break;
			default:
				throw new IllegalStateException("unsupport state "
						+ entry.getValue());
			}
		}
		return index;
	}
}
