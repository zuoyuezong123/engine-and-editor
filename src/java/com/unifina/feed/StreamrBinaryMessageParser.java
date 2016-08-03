package com.unifina.feed;

import com.unifina.data.StreamrBinaryMessage;
import com.unifina.feed.map.MapMessage;
import grails.converters.JSON;

import java.util.Date;
import java.util.Map;

public class StreamrBinaryMessageParser implements MessageParser<StreamrBinaryMessage, MapMessage> {

	@Override
	public MapMessage parse(StreamrBinaryMessage raw) {
		if (raw.getContentType()==StreamrBinaryMessage.CONTENT_TYPE_JSON) {
			String s = raw.toString();
			Map json = (Map) JSON.parse(s);
			return new MapMessage(new Date(raw.getTimestamp()), new Date(), json);
		}
		else throw new RuntimeException("Unknown content type: "+raw.getContentType());
	}

}
