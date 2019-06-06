package org.amit.TwitterSentimentAnalysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

//import com.test.schema.ContactType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.streaming.Duration;
import org.apache.spark.streaming.Durations;
import org.apache.spark.streaming.api.java.JavaDStream;
import org.apache.spark.streaming.api.java.JavaInputDStream;
import org.apache.spark.streaming.api.java.JavaPairDStream;
import org.apache.spark.streaming.api.java.JavaStreamingContext;
import org.apache.spark.streaming.kafka010.ConsumerStrategies;
import org.apache.spark.streaming.kafka010.KafkaUtils;
import org.apache.spark.streaming.kafka010.LocationStrategies;

import scala.Tuple2;

/**
 * Modified on 5/06/19.
 */
public class PopularHashTag {
	public static void main(String[] argv) throws Exception {

		// Configure Spark to connect to Kafka running on local machine
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, "127.0.0.1:9092");
		kafkaParams.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		kafkaParams.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG,
				"org.apache.kafka.common.serialization.StringDeserializer");
		kafkaParams.put(ConsumerConfig.GROUP_ID_CONFIG, "group1");
		kafkaParams.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");// latest,earliest
		kafkaParams.put(ConsumerConfig.ENABLE_AUTO_COMMIT_CONFIG, true);

		// Configure Spark to listen messages in topic test
		Collection<String> topics = Arrays.asList("AmitTopic");

		SparkConf conf = new SparkConf().setMaster("local[6]").setAppName("SparkKafka10WordCount");

		// Read messages in batch of 30 seconds
		JavaStreamingContext jssc = new JavaStreamingContext(conf, Durations.seconds(60));

		// Start reading messages from Kafka and get DStream
		final JavaInputDStream<ConsumerRecord<String, String>> stream = KafkaUtils.createDirectStream(jssc,
				LocationStrategies.PreferConsistent(),
				ConsumerStrategies.<String, String>Subscribe(topics, kafkaParams));

		// Read value of each message from Kafka and return it
		JavaDStream<String> lines = stream.map(x -> x.value());

		// Break every message into words and return list of words
		JavaDStream<String> words = lines.flatMap(x -> Arrays.asList(x.split(" ")).iterator())
				.filter(x -> x.contains("#"));

		// Take every word and return Tuple with (word,1)
		JavaPairDStream<String, Integer> wordMap = words.mapToPair(x -> new Tuple2<String, Integer>(x, 1));

		JavaPairDStream<String, Integer> hashTagTotals = wordMap.reduceByKeyAndWindow((a, b) -> a + b,
				new Duration(60000));

		JavaPairDStream<Integer, String> swappedPair = hashTagTotals.mapToPair(x -> x.swap());

		JavaPairDStream<Integer, String> sortedStream = swappedPair.transformToPair(x -> x.sortByKey(false));

		sortedStream.foreachRDD(x -> {
			x.collect();
			List<Tuple2<Integer, String>> topList = x.take(10);
			System.out.println(String.format("\nPopular topics in last 60 seconds (%s total):", x.count()));
			for (Tuple2<Integer, String> pair : topList) {
				System.out.println(String.format("%s (%s tweets)", pair._2(), pair._1()));
			}
		});

		jssc.start();
		jssc.awaitTermination();
	}
}
