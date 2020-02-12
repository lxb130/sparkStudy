package com.lxb

import kafka.serializer.StringDecoder
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.kafka.KafkaUtils
import org.apache.spark.streaming.{Seconds, StreamingContext}

object KafkaSparkstreamingCommand {
  def main(args: Array[String]): Unit = {
    //    Logger.getRootLogger.setLevel(Level.WARN)
    var paramArray = Array[String]("local[*]", "localhost", "ss_kafka_conmuser_3", "spark_streaming_5", "1")
    val Array(master, zkQuorm, groupid, topics, numTheards) = paramArray
    var conf = new SparkConf()
    conf.setAppName("KafkaSparkstreaming")
//    conf.setMaster(master)
    var ssc = new StreamingContext(conf, Seconds(10))

    var topicMap = topics.split(",").map((_, numTheards.toInt)).toMap

    val kafkaParams = Map(
      "bootstrap.servers" -> "localhost:9092",
      "group.id" -> groupid,
      "zookeeper.connect" -> zkQuorm,

      //"auto.offset.reset" -> "latest",//从最新的开始消费，如果送指定消费offsets者需要去掉此行
      "enable.auto.commit" -> "true",
      "cuto.commit.interval.ms" -> "1000"
    )


    //通过receiver api方式构建DStream
    val lines = KafkaUtils.createStream[String, String, StringDecoder, StringDecoder](ssc,
      kafkaParams, topicMap, StorageLevel.MEMORY_AND_DISK_SER).map(_._2)
    /**
      * 4、进行rdd相关的算子操作
      */
    //通指定时间间隔的数据进行wordcount处理
    val wordCount = lines.flatMap(_.split(" ")).map((_, 1)).reduceByKey(_ + _)
    //将计算结果进行逐条输出操作

    wordCount.foreachRDD(rdd => {
      rdd.foreach(x => println("********************" + x))
    })


    ssc.start()
    ssc.awaitTermination()
  }

}
