package services

import org.slf4j.LoggerFactory
import com.heroku.logfmt.Logfmt
import scala.collection.JavaConverters._
import scala.annotation.tailrec
import java.text.SimpleDateFormat
import spray.json._
import models._
import controllers.Formatters._

object Spitball {
  private lazy val spit = new Spitball(RedisService())
  def apply() = spit
}

class Spitball(val redisService: RedisService) {

  private lazy val logger = LoggerFactory.getLogger(Spitball.getClass)

  val datep = new SimpleDateFormat("yyyy-MM-dd'T'hh:mm:ssZ")

  def get(requestId: String): Seq[Measure] = fromRedis(requestId)


  def getV1(requestId: String): Map[String, String] = {
    fromRedis(requestId).foldRight(Map[String, String]()) {
      (measure, agg) =>
        agg + (measure.name -> measure.value)
    }
  }

  def drain(logs: String) {
    parse(logs).foreach(processLine(_))
  }

  def processLine(line:LogLine){
    val parsedLine = Logfmt.parse(line.line.toCharArray).asScala.toMap.mapValues(new String(_))
    filterRequests(parsedLine).map {  pairs =>
      splitRequestID(pairs).map {
        case (request_id,metricPairs) =>
          toRedis(request_id, toMeasures(metricPairs, line.time))
      }
    }
  }
  def toMeasures(parsedLine: Map[String, String], time: Long): Seq[Measure] = {
    val filtered = parsedLine.filterKeys(_.startsWith("measure."))
    filtered.map {
      case (metric, value) =>
        Measure(metric, value, time)
    }.toSeq
  }


  private def parse(in: String): Iterator[LogLine] = {
    @tailrec
    def loop(unparsed: Iterator[Char], parsed: Iterator[LogLine]): Iterator[LogLine] = {
     if (unparsed.isEmpty) parsed
      else {
        val (head, tail) = unparsed.span(_ != ' ')
        val length = head.mkString.toInt
        val chunk = tail.slice(1, 1 + length).mkString
        val time = datep.parse(chunk.split(' ')(1)).getTime
        loop(tail, parsed ++ Iterator(LogLine(time, chunk)))
      }
    }
    loop(in.toIterator, Iterator.empty)
  }

  private def filterRequests(data: Map[String, String]): Option[(String, Map[String, String])] = {
   data.get("request_id").orElse(data.get("rid")).map(requestId => (requestId, data))
  }

  private def splitRequestID(data: (String, Map[String,String])) : Seq[(String, Map[String,String])] = for{
    request_id <- data._1.split(',').toSeq
   } yield (request_id.replaceAll("""["']""",""),data._2)

  def redisKey(requestId: String): String = {
    "REQUEST_ID:" + requestId
  }

  def fromRedis(requestId: String): Seq[Measure] = {
    redisService.withRedis {
      redis =>
        val data = redis.lrange(requestId, 0, -1)
        val measures = data.asScala.map {
          value =>
            value.asJson.convertTo[Measure]
        }
        return measures
    }
  }

  def toRedis(requestId: String, measures: Seq[Measure]) {
    if (!measures.isEmpty) {
      redisService.withRedis {
        redis =>
          val jsonMeasures = measures.map {
            kv =>
              kv.toJson.toString()
          }
          redis.rpush(redisKey(requestId), jsonMeasures.toSeq: _*)
          redis.expire(redisKey(requestId), sys.env.get("REQUESTS_EXPIRE_SEC").map(_.toInt).getOrElse(10 * 60))
          logger.info(s"redis.save saved.request_id=$requestId saved.pairs=${measures.size}")
      }
    }
  }
}

