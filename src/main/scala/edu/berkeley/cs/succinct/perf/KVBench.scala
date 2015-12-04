package edu.berkeley.cs.succinct.perf

import java.io.FileWriter

import edu.berkeley.cs.succinct.kv.SuccinctKVRDD
import org.apache.spark.rdd.RDD
import org.apache.spark.storage.StorageLevel
import org.apache.spark.{SparkConf, SparkContext}

import scala.io.Source
import scala.reflect.ClassTag
import scala.util.Random
import scala.util.matching.Regex

object KVBench {

  // Constants
  val WARMUP_COUNT: Int = 20
  val MEASURE_COUNT: Int = 100
  val ACCESS_LEN: Int = 1024
  val numRepeats: Int = 1

  // Queries
  var regex: Seq[String] = Seq("<script>.*</script>", "Motorola.*((XPC|MPC)[0-9]+[0-9a-z]+)",
    "William [A-Za-z]+ Clinton", "1-[0-9][0-9][0-9]-[0-9][0-9][0-9]-[0-9][0-9][0-9][0-9]",
    "[a-z0-9_\\.]+\\.stanford\\.edu")
  var words: Array[String] = _
  var wordsWarmup: Array[String] = _
  var wordsMeasure: Array[String] = _
  var keys: Array[Long] = _
  var keysWarmup: Array[Long] = _
  var keysMeasure: Array[Long] = _

  // Output path
  var outPath: String = _

  def sampleArr[T: ClassTag](input: Array[T], sampleSize: Int): Array[T] = {
    Array.fill(sampleSize)(input(Random.nextInt(input.length)))
  }

  def get(rdd: RDD[(Long, Array[Byte])], key: Long): Array[Byte] = {
    val res = rdd.filter(kv => kv._1 == key).collect()
    if (res.length == 0) {
      throw new ArrayIndexOutOfBoundsException(s"Invalid key = $key")
    }
    if (res.length > 1) {
      throw new IllegalArgumentException(s"Got ${res.length} values for key = $key")
    }
    res(0)._2
  }

  def access(rdd: RDD[(Long, Array[Byte])], key: Long): Array[Byte] = {
    val res = rdd.filter(kv => kv._1 == key).map(t => java.util.Arrays.copyOfRange(t._2, 0, ACCESS_LEN)).collect()
    if (res.length == 0) {
      throw new ArrayIndexOutOfBoundsException(s"Invalid key = $key")
    }
    if (res.length > 1) {
      throw new IllegalArgumentException(s"Got ${res.length} values for key = $key")
    }
    res(0)
  }

  def search(rdd: RDD[(Long, Array[Byte])], query: Array[Byte]): RDD[Long] = {
    rdd.filter(t => new String(t._2).contains(new String(query))).map(_._1)
  }

  def regex(rdd: RDD[(Long, Array[Byte])], query: String): RDD[Long] = {
    val rex = new Regex(query)
    rdd.filter(t => rex.findAllMatchIn(new String(t._2)).nonEmpty).map(_._1)
  }

  def benchSparkRDD(rdd: RDD[(Long, Array[Byte])]): Unit = {
    val storageLevel = rdd.getStorageLevel match {
      case StorageLevel.DISK_ONLY => "disk"
      case StorageLevel.MEMORY_ONLY => "mem"
      case _ => "undf"
    }

    println(s"Benchmarking Spark RDD $storageLevel get...")

    // Warmup
    keysWarmup.foreach(k => {
      val length = get(rdd, k).length
      println(s"$k\t$length")
    })

    // Measure
    val outGet = new FileWriter(outPath + "/spark-" + storageLevel + "-get")
    keysMeasure.foreach(k => {
      val startTime = System.currentTimeMillis()
      val length = get(rdd, k).length
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outGet.write(s"$k\t$length\t$totTime\n")
    })
    outGet.close()

    println(s"Benchmarking Spark RDD $storageLevel access...")

    // Warmup
    keysWarmup.foreach(k => {
      val length = access(rdd, k).length
      println(s"$k\t$length")
    })

    // Measure
    val outAccess = new FileWriter(outPath + "/spark-" + storageLevel + "-access")
    keysMeasure.foreach(k => {
      val startTime = System.currentTimeMillis()
      val length = access(rdd, k).length
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outAccess.write(s"$k\t$length\t$totTime\n")
    })
    outAccess.close()

    println(s"Benchmarking Spark RDD $storageLevel search...")

    // Warmup
    wordsWarmup.foreach(w => {
      val count = search(rdd, w.getBytes("utf-8")).count()
      println(s"$w\t$count")
    })

    // Measure
    val outSearch = new FileWriter(outPath + "/spark-" + storageLevel + "-search")
    wordsMeasure.foreach(w => {
      val startTime = System.currentTimeMillis()
      val count = search(rdd, w.getBytes("utf-8")).count()
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outSearch.write(s"$w\t$count\t$totTime\n")
    })
    outSearch.close()
  }

  def benchRegexSparkRDD(rdd: RDD[(Long, Array[Byte])]): Unit = {
    val storageLevel = rdd.getStorageLevel match {
      case StorageLevel.DISK_ONLY => "disk"
      case StorageLevel.MEMORY_ONLY => "mem"
      case _ => "undf"
    }

    val outRegex = new FileWriter(outPath + "/spark-" + storageLevel + "-regex")
    println("Benchmarking Spark RDD regex")
    regex.foreach(r => {
      var time = 0.0
      var count = 0.0
      for (i <- 1 to numRepeats) {
        val startTime = System.currentTimeMillis()
        val results = regex(rdd, r)
        count += results.count()
        val endTime = System.currentTimeMillis()
        val totTime = endTime - startTime
        time += totTime
      }
      count = count / numRepeats
      time = time / numRepeats
      outRegex.write(s"$r\t$count\t$time")
    })
  }

  def benchSuccinctRDD(rdd: SuccinctKVRDD[Long]): Unit = {
    println("Benchmarking Succinct RDD get...")

    println("Benchmarking Succinct RDD get...")
    keysWarmup.foreach(k => {
      val length = rdd.get(k).length
      println(s"$k\t$length")
    })

    // Measure
    val outGet = new FileWriter(outPath + "/succinct-get")
    keysMeasure.foreach(k => {
      val startTime = System.currentTimeMillis()
      val length = rdd.get(k).length
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outGet.write(s"$k\t$length\t$totTime\n")
    })
    outGet.close()

    println("Benchmarking Succinct RDD access...")
    keysWarmup.foreach(k => {
      val length = rdd.extract(k, 0, ACCESS_LEN).length
      println(s"$k\t$length")
    })

    // Measure
    val outAccess = new FileWriter(outPath + "/succinct-access")
    keysMeasure.foreach(k => {
      val startTime = System.currentTimeMillis()
      val length = rdd.extract(k, 0, ACCESS_LEN).length
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outAccess.write(s"$k\t$length\t$totTime\n")
    })
    outAccess.close()

    println("Benchmarking Succinct RDD search...")

    // Warmup
    wordsWarmup.foreach(w => {
      val count = rdd.search(w).count()
      println(s"$w\t$count")
    })

    // Measure
    val outSearch = new FileWriter(outPath + "/succinct-search")
    wordsMeasure.foreach(w => {
      val startTime = System.currentTimeMillis()
      val count = rdd.search(w).count()
      val endTime = System.currentTimeMillis()
      val totTime = endTime - startTime
      outSearch.write(s"$w\t$count\t$totTime\n")
    })
    outSearch.close()

    val outRegex = new FileWriter(outPath + "/succinct-regex")
    println("Benchmarking Spark RDD regex")
    regex.foreach(r => {
      var time = 0.0
      var count = 0.0
      for (i <- 1 to numRepeats) {
        val startTime = System.currentTimeMillis()
        val results = rdd.regexSearch(r)
        count += results.count()
        val endTime = System.currentTimeMillis()
        val totTime = endTime - startTime
        time += totTime
      }
      count = count / numRepeats
      time = time / numRepeats
      outRegex.write(s"$r\t$count\t$time")
    })

  }

  def main(args: Array[String]) {
    if (args.length < 4) {
      System.err.println("Usage: KVBench <raw-data> <succinct-data> <partitions> <queries-path> <output-path>")
      System.exit(1)
    }

    val dataPath = args(0)
    val succinctDataPath = args(1)
    val partitions = args(2).toInt
    val queryPath = args(3)
    outPath = args(4)

    words = Source.fromFile(queryPath).getLines().toArray

    val sparkConf = new SparkConf().setAppName("KVBench")
    val ctx = new SparkContext(sparkConf)

    val kvRDD = ctx.textFile(dataPath)
      .zipWithIndex()
      .map(t => (t._2, t._1.getBytes))
      .repartition(partitions)

    val kvRDDDisk = kvRDD.persist(StorageLevel.DISK_ONLY)
    val count = kvRDDDisk.count()
    println("Number of entries = " + kvRDDDisk.count())

    keys = Random.shuffle((0 to 9999)
      .map(i => Math.abs(Random.nextLong()) % count))
      .toArray

    // Create queries
    keysWarmup = sampleArr(keys, WARMUP_COUNT)
    keysMeasure = sampleArr(keys, MEASURE_COUNT)
    wordsWarmup = sampleArr(words, WARMUP_COUNT)
    wordsMeasure = sampleArr(words, MEASURE_COUNT)

    benchSparkRDD(kvRDDDisk)
    kvRDDDisk.unpersist(true)

    val kvRDDMem = kvRDD.persist(StorageLevel.MEMORY_ONLY)
    println("Number of entries = " + kvRDDMem.count())

    benchSparkRDD(kvRDDMem)
    kvRDDMem.unpersist(true)

    val kvRDDSuccinct = SuccinctKVRDD[Long](ctx, succinctDataPath, StorageLevel.MEMORY_ONLY).cache()
    println("Number of entries = " + kvRDDSuccinct.count())

    benchSuccinctRDD(kvRDDSuccinct)
  }
}
