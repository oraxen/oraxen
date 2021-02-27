package org.playuniverse.snowypine.helper;

import java.util.regex.Pattern;

public class URLHelper {

	public static final Pattern URL_PATTERN = Pattern.compile("^(https?:\\/\\/)?([\\w\\Q$-_+!*'(),%\\E]+\\.)+(\\w{2,63})(:\\d{1,4})?([\\w\\Q/$-_+!*'(),%\\E]+\\.?[\\w])*\\/?$");
	
	private URLHelper() {}

	public static boolean isUrl(String url) {
		return URL_PATTERN.matcher(url).matches();
	}
	
}
