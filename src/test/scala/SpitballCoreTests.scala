import redis.clients.jedis.exceptions.JedisDataException
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

class SpitballCoreTests extends Specification with Mockito {

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
      redis.lrange("REQUEST_ID:request", 0, -1) returns testStrings.asJava
      val values = spit.fromRedis("request")
      there was one(redis).lrange("REQUEST_ID:request", 0, -1)


      values.length equals 2
      values(1).name equals "test2"
      values(1).time equals 42
      values(1).value equals "val2"
    }

    "v1 api should return expected json" in {
      val (redis,spit) = buildMocks
      redis.lrange("REQUEST_ID:request", 0, -1) returns testStrings.asJava
      val kvs = spit.getV1("request")
      kvs("test") equals "val"
      kvs("test2") equals "val2"
    }

    "toMeasureList should convert a map of metrics to a list of measures" in {
      val (redis,spit) = buildMocks
      val map = Map("measure.time" -> "20", "measure.awesome" -> "9001")
      val measures = spit.toMeasures(map, 100)
      measures.length equals 2
      measures(0).name equals "measure.time"
      measures(0).time equals 100
      measures(0).value equals "20"
    }
    "key should chomp delemeters before and after request_id" in {
      val (redis,spit) = buildMocks
      spit.redisKey("test") equals "REQUEST_ID:test"
    }

    "A real log line should be able to be parsed" in {
      val (redis,spit) = buildMocks
      val line = "74 <174>1 2012-07-22T00:00:00+00:00 request_id=derp measure.things=231"
      spit.drain(line)
      there was one(redis).rpush("REQUEST_ID:derp",
       """{"name":"measure.things","value":"231","time":1342940400000}""")
    }
    "processLine should process a line correctly with rid" in {
      val (redis,spit) = buildMocks
      val line = "rid='req,req2' measure.status='derp' measure.cows='lots'"
      spit.processLine(LogLine(100,line))
      there was one(redis).rpush("REQUEST_ID:req",
        """{"name":"measure.cows","value":"'lots'","time":100}""",
        """{"name":"measure.status","value":"'derp'","time":100}""")
       there was one(redis).rpush("REQUEST_ID:req2",
        """{"name":"measure.cows","value":"'lots'","time":100}""",
        """{"name":"measure.status","value":"'derp'","time":100}""")
    }
    "processLine should process a line correctly with request_id" in {
      val (redis,spit) = buildMocks
      val line = "rid='req,req2' measure.status='derp' measure.cows='lots'"
      spit.processLine(LogLine(100,line))
      there was one(redis).rpush("REQUEST_ID:req",
        """{"name":"measure.cows","value":"'lots'","time":100}""",
        """{"name":"measure.status","value":"'derp'","time":100}""")
      there was one(redis).rpush("REQUEST_ID:req2",
        """{"name":"measure.cows","value":"'lots'","time":100}""",
        """{"name":"measure.status","value":"'derp'","time":100}""")
    }

    "get should deal gracefully with old data fromat" in {
      val (redis,spit) = buildMocks
      redis.lrange(anyString,anyInt,anyInt) throws new JedisDataException("broken")
      redis.hgetAll("REQUEST_ID:test") returns Map("key" -> "value", "key2" -> "value2").asJava
      val vals = spit.get("test")
      there was one(redis).lrange("REQUEST_ID:test",0,-1)
      there was one(redis).hgetAll("REQUEST_ID:test")

      vals(0) shouldEqual  Measure("key","value",0)
      vals(1) shouldEqual  Measure("key2","value2", 0)

    }

    "getV1 redis should deal gracefully with old data fromat" in {
      val (redis,spit) = buildMocks
      redis.lrange(anyString,anyInt,anyInt) throws new JedisDataException("broken")
      redis.hgetAll("REQUEST_ID:test") returns Map("key" -> "value", "key2" -> "value2").asJava
      val vals = spit.getV1("test")
      there was one(redis).lrange("REQUEST_ID:test",0,-1)
      there was one(redis).hgetAll("REQUEST_ID:test")

      vals("key") shouldEqual  "value"
      vals("key2") shouldEqual "value2"

    }
  }
}