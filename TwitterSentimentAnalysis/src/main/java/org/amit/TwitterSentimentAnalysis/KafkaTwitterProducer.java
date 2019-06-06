package org.amit.TwitterSentimentAnalysis;

import java.util.Arrays;
import java.util.Properties;
import java.util.concurrent.LinkedBlockingQueue;

import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.Producer;
import org.apache.kafka.clients.producer.ProducerRecord;

import twitter4j.FilterQuery;
import twitter4j.HashtagEntity;
import twitter4j.StallWarning;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.StatusListener;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.conf.ConfigurationBuilder;

/**
 * A Kafka Producer that gets tweets on certain keywords from twitter datasource
 * and publishes to a kafka topic
 * 
 * Arguments: <comsumerKey> <consumerSecret> <accessToken> <accessTokenSecret>
 * <topic-name> <keyword_1> ... <keyword_n> <comsumerKey> - Twitter consumer key
 * <consumerSecret> - Twitter consumer secret <accessToken> - Twitter access
 * token <accessTokenSecret> - Twitter access token secret <topic-name> - The
 * kafka topic to subscribe to <keyword_1> - The keyword to filter tweets
 * <keyword_n> - Any number of keywords to filter tweets
 * 
 * More discussion at stdatalabs.blogspot.com
 * 
 * @author Sachin Thirumala
 * @Modified Amit Mishra
 */

public class KafkaTwitterProducer {
	public static void main(String[] args) throws Exception {
		final LinkedBlockingQueue<Status> queue = new LinkedBlockingQueue<Status>(1000);


		String consumerKey = "vQbjeTbZINRv6U1pl683cK9AR";// args[0].toString();
		String consumerSecret = "WWZDixUaNfcDYC7yal1R2i7seq2bwwLOnGxSvvdMTp8y83Z6GT"; // args[1].toString();
		String accessToken = "771398523313086466-4WeHgdsoKBPt15Gdqk9m20SxWwhFwem"; // args[2].toString();
		String accessTokenSecret = "XY1RksJJMRoQO1xJuw6Mbpi62mCNHNdQ1gy28B1ZJrID6";// args[3].toString();
		String topicName = "AmitTopic"; // args[4].toString();
		String[] arguments = "Modi".split(" "); // args.clone(); Iphone,Iphone,Modi
		String[] keyWords = Arrays.copyOfRange(arguments, 0, arguments.length);

		// Set twitter oAuth tokens in the configuration
		ConfigurationBuilder cb = new ConfigurationBuilder();
		cb.setDebugEnabled(true).setOAuthConsumerKey(consumerKey).setOAuthConsumerSecret(consumerSecret)
				.setOAuthAccessToken(accessToken).setOAuthAccessTokenSecret(accessTokenSecret);

		// Create twitterstream using the configuration
		TwitterStream twitterStream = new TwitterStreamFactory(cb.build()).getInstance();
		StatusListener listener = new StatusListener() {

			public void onStatus(Status status) {
				queue.offer(status);
			}

			public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
				System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
			}

			public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
				System.out.println("Got track limitation notice:" + numberOfLimitedStatuses);
			}

			public void onScrubGeo(long userId, long upToStatusId) {
				System.out.println("Got scrub_geo event userId:" + userId + "upToStatusId:" + upToStatusId);
			}

			public void onStallWarning(StallWarning warning) {
				System.out.println("Got stall warning:" + warning);
			}

			public void onException(Exception ex) {
				ex.printStackTrace();
			}
		};
		twitterStream.addListener(listener);

		// Filter keywords
		FilterQuery query = new FilterQuery().track(keyWords);
		twitterStream.filter(query);

		// Thread.sleep(5000);

		// Add Kafka producer config settings
		Properties props = new Properties();
		props.put("metadata.broker.list", "127.0.0.1:9092");
		props.put("bootstrap.servers", "127.0.0.1:9092");
		props.put("acks", "all");
		props.put("retries", 0);
		props.put("batch.size", 16384);
		props.put("linger.ms", 1);
		props.put("buffer.memory", 33554432);

		props.put("key.serializer", "org.apache.kafka.common.serialization.StringSerializer");
		props.put("value.serializer", "org.apache.kafka.common.serialization.StringSerializer");

		Producer<String, String> producer = new KafkaProducer<String, String>(props);
		int j = 0;

		// poll for new Tweets in the queue. If new Tweets are added, send them to the topic
		while (true) {
			Status ret = queue.poll();

			if (ret == null) {
				Thread.sleep(100);
			} else {
				for (HashtagEntity hashtage : ret.getHashtagEntities()) {
					//System.out.println(ret.getUser().getName()+"-> "+ret.getText()+": "+hashtage.getText());
					 //System.out.println("Tweet:" + ret);
					//System.out.println("Hashtag: " + hashtage.getText());
					// producer.send(new ProducerRecord<String, String>(
					// topicName, Integer.toString(j++), hashtage.getText()));
					producer.send(new ProducerRecord<String, String>(topicName,Integer.toString(j++), ret.getText()));

				}
			}
		}
	}
}