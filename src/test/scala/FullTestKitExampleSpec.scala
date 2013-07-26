import scala.collection.JavaConverters._
import spray.json._
import models._
import org.specs2.mutable.Specification
import redis.clients.jedis.Jedis
import spray.testkit.Specs2RouteTest
import spray.routing.HttpService
import services.{RedisService, Spitball}
import org.specs2.mock._
import java.util.Date
import controllers.Formatters._

class FullTestKitExampleSpec extends Specification with Mockito {

  // connect the DSL to the test ActorSystem

  "The service" should {
    def buildMocks = {
      val redis = mock[Jedis]
      val redisService = mock[RedisService]
      redisService.withRedis(anyFunction1[Jedis, Any]).answers {
        t =>
          t.asInstanceOf[(Jedis => Any)](redis)
      }

      val spit = new Spitball(redisService)
      (redis,spit)
    }


    val time: Long = 42
    val testMeasures = List(Measure("test", "val", time), Measure("test2", "val2", time))
    val testStrings = testMeasures.map(_.toJson.toString()).toSeq

    "check our mocking" in {
      {
        val (redis,spit) = buildMocks
        redis.get("test") returns "foo"
        redis.get("test")
        there was one(redis).get("test")

      }
    }

    "toRedis should add a map of values to redis" in {
      val (redis,spit) = buildMocks
      spit.toRedis("1", testMeasures.toSeq)

      there was one(redis).rpush("REQUEST_ID:1", testStrings: _*)
    }

    "fromRedis should return a set of Measures" in {
      val (redis,spit) = buildMocks
      redis.lrange("request", 0, -1) returns testStrings.asJava
      val values = spit.fromRedis("request")
      there was atLeastOne(redis).lrange("request", 0, -1)

      values.length equals 2
      values(1).name equals "test2"
      values(1).time equals 42
      values(1).value equals "val2"
    }

    "v1 api should return expected json" in {
      val (redis,spit) = buildMocks
      redis.lrange("request", 0, -1) returns testStrings.asJava
      val kvs = spit.getV1("request")
      kvs("test") equals "val"
      kvs("test2") equals "val2"
    }

    "toMeasureList should convert a map of metrics to a list of measures" in {
      val (redis,spit) = buildMocks
      val map = Map("measure.time" -> "20", "measure.awesome" -> "9001")
      val measures = spit.toMeasureList(map, LogLine("derp", 100))
      measures.length equals 2
      measures(0).name equals "measure.time"
      measures(0).time equals 100
      measures(0).value equals "20"
    }
  }
}