package services;

import java.net.URI
import org.slf4j.LoggerFactory
import redis.clients.jedis._
import scala.Some
import org.apache.commons.pool.impl.GenericObjectPool.Config
import java.util.concurrent.atomic.AtomicInteger

object RedisService {
  val redisUrl = sys.env.get("REDIS_URL").map(uri => new URI(uri)).getOrElse(sys.error("REDIS_URL undefined"))

  val subscribeThreadCounter = new AtomicInteger(0)

  def apply(): RedisService = new RedisService(redisUrl)

  def apply(uri: URI) = new RedisService(uri)
}

class RedisService(val redisUrl: URI) {

  val log = LoggerFactory.getLogger("Redis")

  val redisPassword: Option[String] = Option(redisUrl.getUserInfo).map(_.split(":").apply(1))

  lazy val redisPool: JedisPool = createRedisPool()

  def redisConnection(): Jedis = {
    val redis = new Jedis(redisUrl.getHost, redisUrl.getPort)
    for (p <- redisPassword) {
      redis.auth(p)
    }
    log.info("redisConnection()")
    redis
  }

  def createRedisPool(): JedisPool = {
    val config = new Config()
    config.testOnBorrow = true
    redisPassword match {
      case Some(password) => new JedisPool(config, redisUrl.getHost, redisUrl.getPort, Protocol.DEFAULT_TIMEOUT, password)
      case None => new JedisPool(config, redisUrl.getHost, redisUrl.getPort, Protocol.DEFAULT_TIMEOUT, null)
    }
  }

  def withRedis[T](thunk: Jedis => T): T = {
    val resource = redisPool.getResource
    try {
      val t = thunk(resource)
      redisPool.returnResource(resource.asInstanceOf[BinaryJedis])
      t
    } catch {
      case e: Exception =>
        redisPool.returnBrokenResource(resource.asInstanceOf[BinaryJedis])
        throw e
    }
  }

}