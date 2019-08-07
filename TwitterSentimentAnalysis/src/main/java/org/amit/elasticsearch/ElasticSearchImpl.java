package org.amit.elasticsearch;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import org.amit.model.Twitter;
import org.apache.http.HttpHost;
import org.elasticsearch.ElasticsearchException;
import org.elasticsearch.action.index.IndexRequest;
import org.elasticsearch.action.index.IndexResponse;
import org.elasticsearch.client.RestClient;
import org.elasticsearch.client.RestHighLevelClient;

import com.fasterxml.jackson.databind.ObjectMapper;

public class ElasticSearchImpl {
	// The config parameters for the connection
	private static final String HOST = "localhost";
	private static final int PORT_ONE = 9200;
	private static final int PORT_TWO = 9201;
	private static final String SCHEME = "http";
	
	private static final String INDEX = "twitter";
	private static final String TYPE = "_doc";

	private static RestHighLevelClient restHighLevelClient;
	private static ObjectMapper objectMapper = new ObjectMapper();

	/**
	 * Implemented Singleton pattern here so that there is just one connection at a
	 * time.
	 * 
	 * @return RestHighLevelClient
	 */
	private static synchronized RestHighLevelClient makeConnection() {

		if (restHighLevelClient == null) {
			restHighLevelClient = new RestHighLevelClient(
					RestClient.builder(new HttpHost(HOST, PORT_ONE, SCHEME), new HttpHost(HOST, PORT_TWO, SCHEME)));
		}

		return restHighLevelClient;
	}

	private static synchronized void closeConnection() throws IOException {
		restHighLevelClient.close();
		restHighLevelClient = null;
	}
	
	public static Twitter insertPerson(Twitter twitter) {
		Map<String, Object> dataMap = new HashMap<String, Object>();
		System.out.println(twitter.getUpdatedOn());
		dataMap.put("TrendingKeyword", twitter.getTrendingKeyword());
		dataMap.put("hashTaglists", twitter.getTopHashTagsWithCount());
		dataMap.put("hashtags", twitter.getTopHashTags());
		dataMap.put("UpdatedDate", twitter.getUpdatedOn());
		IndexRequest indexRequest = new IndexRequest(INDEX, TYPE, twitter.getTwitterId()).source(dataMap);
		try {
			makeConnection();
			IndexResponse response = restHighLevelClient.index(indexRequest);
			closeConnection();
		} catch (ElasticsearchException e) {
			System.out.println(e.getDetailedMessage());
		} catch (java.io.IOException ex) {
			System.out.println(ex.getLocalizedMessage());
		}
		
		return twitter;
	}
	
	public static void main(String[] args) {
		
		Twitter twitter= new Twitter();
		insertPerson(twitter);
		
	}
	
	
	
}
