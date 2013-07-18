package services

import org.slf4j.LoggerFactory
import com.heroku.logfmt.Logfmt
import scala.collection.JavaConverters._

object Spitball {

  private lazy val logger = LoggerFactory.getLogger(Spitball.getClass)
  private lazy val redisService = RedisService()

  def get(requestId: String) = fromRedis(requestId)

  def drain(logs: String) {
    parse(logs).foreach { line =>
      val parsedLine = Logfmt.parse(line.toCharArray).asScala.toMap.mapValues(new String(_))
      forRequestId(parsedLine).map { entry =>
        splitRequestID(entry).map { entry =>
          val requestId = entry._1
          val pairs = entry._2.filterKeys(_.startsWith("measure."))
          toRedis(requestId, pairs)
        }
      }
    }
  }


  private def parse(in: String): Iterator[String] = {
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

  private def forRequestId(data: Map[String, String]): Option[(String, Map[String, String])] = {
    data.get("request_id").orElse(data.get("rid")).map(requestId => (requestId, data))
  }

  private def splitRequestID(data: (String, Map[String,String])) : Seq[(String, Map[String,String])] = for{
    request_id <- data._1.split(',').toSeq
   } yield (request_id,data._2)

  private def key(requestId: String): String = {
    "REQUEST_ID:" + requestId
  }

  private def fromRedis(requestId: String): Map[String, String] = {
    redisService.withRedis { redis =>
      val data = redis.hgetAll(key(requestId))
      if (data == null) Map.empty
      else data.asScala.toMap
    }
  }

  private def toRedis(requestId: String, pairs: Map[String, String]) {
    if (!pairs.isEmpty) {
      redisService.withRedis { redis =>
        redis.hmset(key(requestId), pairs.asJava)
        redis.expire(key(requestId), sys.env.get("REQUESTS_EXPIRE_SEC").map(_.toInt).getOrElse(10 * 60))
        logger.info(s"redis.save saved.request_id=$requestId saved.pairs=${pairs.keys.size}")
      }
    }
  }
}
