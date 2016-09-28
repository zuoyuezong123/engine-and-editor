package com.unifina.feed.cassandra;

import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Row;
import com.datastax.driver.core.Session;
import com.unifina.data.StreamrBinaryMessage;
import com.unifina.domain.data.Stream;
import com.unifina.feed.map.MapMessage;
import com.unifina.service.CassandraService;
import grails.converters.JSON;
import grails.util.Holders;
import org.codehaus.groovy.grails.web.json.JSONObject;

import java.io.Closeable;
import java.io.IOException;
import java.util.Date;
import java.util.Iterator;

public class CassandraHistoricalIterator implements Iterator<MapMessage>, Closeable {

	private final Stream stream;
	private final Integer partition;
	private final Date startDate;
	private final Date endDate;

	private Session session;

	private ResultSet resultSet;

	private static final int PREFETCH_WHEN_REMAINING = 500;
	private static final int FETCH_SIZE = 5000;

	public CassandraHistoricalIterator(Stream stream, Integer partition, Date startDate, Date endDate) {
		this.stream = stream;
		this.partition = partition;
		this.startDate = startDate;
		this.endDate = endDate;
		connect();
	}

	/**
	 * Returns a session with the Streamr Cassandra cluster.
	 */
	protected Session getSession() {
		return Holders.getApplicationContext().getBean(CassandraService.class).getSession();
	}


	private void connect() {
		session = getSession();
		session.getCluster().getConfiguration().getQueryOptions().setFetchSize(FETCH_SIZE);


		// Get timestamp limits as offsets, then execute query using offsets
		Row firstOffsetRow = session.execute("SELECT kafka_offset FROM stream_timestamps WHERE stream = ? AND stream_partition = ? AND ts >= ? ORDER BY ts ASC LIMIT 1", stream.getId(), partition, startDate).one();
		if (firstOffsetRow == null) {
			return;
		}
		Long firstOffset = firstOffsetRow.getLong("kafka_offset");
		Long lastOffset = session.execute("SELECT kafka_offset FROM stream_timestamps WHERE stream = ? AND stream_partition = ? AND ts <= ? ORDER BY ts DESC LIMIT 1", stream.getId(), partition, endDate).one().getLong("kafka_offset");

		resultSet = session.execute("SELECT payload FROM stream_events WHERE stream = ? AND stream_partition = ? AND kafka_offset >= ? and kafka_offset <= ? ORDER BY kafka_offset ASC", stream.getId(), partition, firstOffset, lastOffset);
	}

	@Override
	public boolean hasNext() {
		return !resultSet.isExhausted();
	}

	@Override
	public MapMessage next() {
		Row row = resultSet.one();

		// Async-fetch more rows if not many left
		if (resultSet.getAvailableWithoutFetching() == PREFETCH_WHEN_REMAINING && !resultSet.isFullyFetched()) {
			resultSet.fetchMoreResults(); // this is asynchronous
		}

		StreamrBinaryMessage msg = new StreamrBinaryMessage(row.getBytes("payload"));
		if (msg.getContentType() == StreamrBinaryMessage.CONTENT_TYPE_JSON) {
			return new MapMessage(new Date(msg.getTimestamp()), new Date(msg.getTimestamp()), (JSONObject) JSON.parse(msg.toString()));
		}
		else {
			throw new RuntimeException("Received payload in unknown format: "+msg.toString());
		}
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException("Unsupported!");
	}

	@Override
	public void close() throws IOException {
		// Don't close the session, because it's reusable
	}
}