package com.lxb

import org.apache.log4j.{Level, Logger}
import org.apache.spark.SparkConf
import org.apache.spark.storage.StorageLevel
import org.apache.spark.streaming.{Seconds, StreamingContext}
import org.apache.spark.streaming.kafka.KafkaUtils

object KafkaRecciver {
  def main(args: Array[String]): Unit = {
    Logger.getLogger("org.apache.spark").setLevel(Level.ERROR)
    Logger.getLogger("org.eclipse.jetty.server").setLevel(Level.OFF)
    val conf = new SparkConf().setAppName("SparkFlumeNGWordCount").setMaster("local[*]")
    val ssc = new StreamingContext(conf, Seconds(10))
    ssc.checkpoint("/checkpoint")
    //创建kafka对象   生产者 和消费者
    //模式1 采取的是 receiver 方式  reciver 每次只能读取一条记录
    val topic = Map("spark_streaming_5" -> 1)
    //直接读取的方式  由于kafka 是分布式消息系统需要依赖Zookeeper
    val data = KafkaUtils.createStream(ssc, "127.0.0.1:2181", "test-consumer-group", topic, StorageLevel.MEMORY_AND_DISK)
    //数据累计计算
    val updateFunc =(curVal:Seq[Int],preVal:Option[Int])=>{
      //进行数据统计当前值加上之前的值
      var total = curVal.sum
      //最初的值应该是0
      var previous = preVal.getOrElse(0)
      //Some 代表最终的返回值
      Some(total+previous)
    }
    val result = data.map(_._2).flatMap(_.split(" ")).map(word=>(word,1)).updateStateByKey(updateFunc).print()
    //启动ssc
    ssc.start()
    ssc.awaitTermination()

  }
}