package org.amit.TwitterSentimentAnalysis;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

//import com.test.schema.ContactType;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.spark.SparkConf;
import org.apache.spark.api.java.JavaPairRDD;
import org.apache.spark.api.java.function.FlatMapFunction;
import org.apache.spark.api.java.function.Function;
import org.apache.spark.api.java.function.Function2;
import org.apache.spark.api.java.function.PairFunction;
import org.apache.spark.api.java.function.VoidFunction;
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
 * Created by sunilpatil on 1/11/17.
 */
public class PopularHashTag {
	public static void main(String[] argv) throws Exception {

		// Configure Spark to connect to Kafka running on local machine
		Map<String, Object> kafkaParams = new HashMap<>();
		kafkaParams.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,
				"127.0.0.1:9092");
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
		JavaDStream<String> lines = stream.map(new Function<ConsumerRecord<String, String>, String>() {
			@Override
			public String call(ConsumerRecord<String, String> kafkaRecord) throws Exception {
				return kafkaRecord.value();
			}
		});

		// Break every message into words and return list of words
		JavaDStream<String> words = lines.flatMap(new FlatMapFunction<String, String>() {
			@Override
			public Iterator<String> call(String line) throws Exception {
				return Arrays.asList(line.split(" ")).iterator();
			}
		}).filter(new Function<String, Boolean>() {
			public Boolean call(String x) {
				return x.contains("#");
			}
		});

		// words.print();

		// JavaDStream<String> topcount60=words.map(word->(word,1));

		// Take every word and return Tuple with (word,1)
		JavaPairDStream<String, Integer> wordMap = words.mapToPair(new PairFunction<String, String, Integer>() {
			@Override
			public Tuple2<String, Integer> call(String word) throws Exception {
				return new Tuple2<>(word, 1);
			}
		});

		JavaPairDStream<String, Integer> hashTagTotals = wordMap
				.reduceByKeyAndWindow(new Function2<Integer, Integer, Integer>() {
					@Override
					public Integer call(Integer a, Integer b) {
						return a + b;
					}
				}, new Duration(60000));

		JavaPairDStream<Integer, String> swappedPair = hashTagTotals.mapToPair(x -> x.swap());

		JavaPairDStream<Integer, String> sortedStream = swappedPair
				.transformToPair(new Function<JavaPairRDD<Integer, String>, JavaPairRDD<Integer, String>>() {
					@Override
					public JavaPairRDD<Integer, String> call(JavaPairRDD<Integer, String> jPairRDD) throws Exception {
						return jPairRDD.sortByKey(false);
					}
				});

		// sortedStream.print();
		// sortedStream.dstream().saveAsTextFiles("hdfs://192.168.129.124:8020/Mahout/",
		// "happiest10.txt");

		sortedStream.foreachRDD(new VoidFunction<JavaPairRDD<Integer, String>>() {

			@Override
			public void call(JavaPairRDD<Integer, String> rdd) throws Exception {
				rdd.collect();
				List<Tuple2<Integer, String>> topList = rdd.take(10);
				System.out.println(String.format("\nPopular topics in last 60 seconds (%s total):", rdd.count()));
				for (Tuple2<Integer, String> pair : topList) {
					System.out.println(String.format("%s (%s tweets)", pair._2(), pair._1()));
				}
			}
		});

		jssc.start();
		jssc.awaitTermination();
	}
}
