/**
 * Created with IntelliJ IDEA.
 * User: ericfode
 * Date: 7/26/13
 * Time: 10:30 AM
 * To change this template use File | Settings | File Templates.
 */
import org.slf4j.LoggerFactory
import com.heroku.logfmt.Logfmt
import scala.collection.JavaConverters._
import scala.annotation.tailrec
import java.text.SimpleDateFormat
import services.Spitball
import spray.json._
import models._
import controllers.Formatters._

Spitball
