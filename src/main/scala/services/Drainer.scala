package services

import org.slf4j.LoggerFactory
import com.heroku.logfmt.Logfmt
import scala.collection.JavaConverters._

object Drainer {

  def logger = LoggerFactory.getLogger(Drainer.getClass)
  lazy val redisService = RedisService()

  def drain(in: String) {
    parse(in).foreach { line =>
      println("LINE:" + line)
      val parsedLine = Logfmt.parse(line.toCharArray).asScala.toMap.mapValues(new String(_))
      println("PARSED_LINE:" + parsedLine)
      forRequestId(parsedLine).map { entry =>
        println("ENTRY:" + entry)
        val requestId = entry._1
        val pairs = entry._2.filterKeys(_.startsWith("measure."))
        println("PAIRS:" + pairs)
        toRedis(requestId, pairs)
      }
    }
  }

  def key(requestId: String): String = {
    "REQUEST_ID:" + requestId
  }

  def fromRedis(requestId: String): Map[String, String] = {
    redisService.withRedis { redis =>
      val data = redis.hgetAll(key(requestId))
      if (data == null) Map.empty
      else data.asScala.toMap
    }
  }

  def toRedis(requestId: String, pairs: Map[String, String]) {
    if (!pairs.isEmpty) {
      redisService.withRedis { redis =>
        redis.hmset(key(requestId), pairs.asJava)
        redis.expire(key(requestId), sys.env.get("REQUESTS_EXPIRE_SEC").map(_.toInt).getOrElse(30 * 60))
      }
    }
  }

  def forRequestId(data: Map[String, String]): Option[(String, Map[String, String])] = {
    data.get("request_id").map(requestId => (requestId, data))
  }

  def parse(in: String): Iterator[String] = {
    def loop(unparsed: Iterator[Char], parsed: Iterator[String]): Iterator[String] = {
      if (unparsed.isEmpty) parsed
      else {
        val (head, tail) = unparsed.span(_ != ' ')
        val length = head.mkString.toInt
        val chunk = tail.slice(1, 1 + length).mkString
        loop(tail, parsed ++ Iterator(chunk))
      }
    }
    loop(in.toIterator, Iterator.empty)
  }

}
