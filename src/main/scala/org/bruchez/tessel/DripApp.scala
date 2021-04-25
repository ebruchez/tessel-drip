package org.bruchez.tessel

import org.bruchez.tessel.Util._

import scala.async.Async._
import scala.concurrent.Future
import scala.concurrent.duration._
import scala.scalajs.concurrent.JSExecutionContext.Implicits.queue
import scala.scalajs.js
import scala.scalajs.js.Dynamic.{global ⇒ g}
import scala.util.{Failure, Success, Try}

//noinspection TypeAnnotation
object DripApp extends js.JSApp {

  val IrrigationDuration = 10.minutes
  val RainThresholdMm    = 10

  val Test = false

  val ProdHourUtc            = 12  // 12:00 GMT which is 5:00 UTC
  val ProdMinuteUtc          = 0

  val TestHourUtc            = 20
  val TestMinuteUtc          = 14

  val HourUtc   = if (Test) TestHourUtc   else ProdHourUtc
  val MinuteUtc = if (Test) TestMinuteUtc else ProdMinuteUtc

  val HealthStatusCron = "0 3,15,21 * * *"

  val IFTTTKey           = APIKeys.IFTTTKey
  val DarkSkyKey         = APIKeys.DarkSkyKey // https://darksky.net/dev/account

  val WeatherLatitude    = 37.5606821
  val WeatherLongitude   = -122.2518543

  val RelayChannel       = Relay.Channel2
  val LedChannel         = 2
  val IFTTTEventName     = "DripEvent"

  case class WeatherDetails(highC: Double, lowC: Double, willRain: Boolean)

  sealed trait DripAction { val name: String }
  case object OnAction      extends { val name = "on"      } with DripAction
  case object OffAction     extends { val name = "off"     } with DripAction
  case object SkipAction    extends { val name = "skip"    } with DripAction
  case object WeatherAction extends { val name = "weather" } with DripAction
  case object StatusAction  extends { val name = "status"  } with DripAction

  val darkSkyUrl =
    s"https://api.darksky.net/forecast/$DarkSkyKey/$WeatherLatitude,$WeatherLongitude?units=si&exclude=minutely,hourly,alerts,flags"

  def notificationUrl(eventName: String, message: Option[String]) =
    s"https://maker.ifttt.com/trigger/$IFTTTEventName/with/key/$IFTTTKey?value1=$eventName&value2=${message.getOrElse("")}"

  async {

    LEDBlinker.start()

    await(notifyIFTTT(StatusAction, Some(s"starting Tessel ${OS.hostname()} at ${new js.Date()} with node version ${g.process.version}")))
    await(sendHealthStatus())

    val relay = await(Relay.useF(Tessel.port.A))

    def toggleRelayAndLed(onOff: OnOff) = async {
      await(relay.setStateF(RelayChannel, onOff).toTry) match {
        case Success(_) if onOff == On  ⇒ Tessel.led(LedChannel).on()
        case Success(_) if onOff == Off ⇒ Tessel.led(LedChannel).off()
        case Failure(_) ⇒
      }
    }

    def irrigateProcess(reason: String) = async {
      await(toggleRelayAndLed(On))
      await(notifyIFTTT(OnAction, Some(s"for duration $IrrigationDuration because $reason")))
      await(delay(IrrigationDuration))
      await(toggleRelayAndLed(Off))
      await(notifyIFTTT(OffAction, None))
    }

    def irrigateProcessCheckWeather() = async {
      await(tryWeather) match {
        case Success(w @ WeatherDetails(_, _, willRain)) if ! willRain ⇒
          await(notifyIFTTT(WeatherAction, Some(s"$w")))
          await(irrigateProcess(s"no rain is expected"))
        case Success(w @ WeatherDetails(_, _, _)) ⇒
          await(notifyIFTTT(WeatherAction, Some(s"$w")))
          await(notifyIFTTT(SkipAction, Some(s"skipping because rain is expected")))
        case Failure(t) ⇒
          await(irrigateProcess(s"getting weather information failed with ${t.getMessage}"))
      }
    }

    await(toggleRelayAndLed(Off))

    def pad2(i: Int) =
      if (i < 10)
        "0" + i
      else
        i.toString

    def timeString(hours: Int, minutes: Int) =
      s"${pad2(hours)}:${pad2(minutes)} UTC"

    await(notifyIFTTT(StatusAction, Some(s"scheduling jobs: health status (`$HealthStatusCron`); irrigation (${timeString(HourUtc, MinuteUtc)})")))

    NodeSchedule.scheduleJob(HealthStatusCron, sendHealthStatus _)
    NodeSchedule.scheduleJob(js.Dynamic.literal(hour = HourUtc, minute = MinuteUtc),  irrigateProcessCheckWeather _)
  }

  def retrieveWeatherDetails(json: js.Dynamic) = {
    val dayForecast = json.daily.data.selectDynamic("0")

    val highC    = dayForecast.temperatureMax.asInstanceOf[Double]
    val lowC     = dayForecast.temperatureMin.asInstanceOf[Double]
    val willRain = dayForecast.icon.asInstanceOf[String] == "rain"

    WeatherDetails(highC, lowC, willRain)
  }

  def tryWeather: Future[Try[WeatherDetails]] = async {
    await(NodeFetch(darkSkyUrl, null).toFuture.toTry) match {
      case Success(res) ⇒
        await(res.asInstanceOf[NodeFetchResponse].json().toFuture.toTry) match {
          case Success(json) ⇒
            Try(retrieveWeatherDetails(json.asInstanceOf[js.Dynamic]))
          case Failure(t) ⇒
            Failure(t)
        }
      case Failure(t) ⇒
        Failure(t)
    }
  }

  def notifyIFTTT(event: DripAction, message: Option[String]) = async {

    println(s"calling IFTTT for `$event`: $message")

    await(NodeFetch(notificationUrl(event.name, message), null).toFuture.toTry) match {
      case Success(res) ⇒ println(s"got response from IFTTT: ${res.status}")
      case Failure(t)   ⇒ println(s"failure calling IFTTT: ${t.getMessage}")
    }
  }

  def sendHealthStatus() = async {

    if (! js.isUndefined(g.gc))
      g.gc()

    val v8Stats = V8.getHeapStatistics().asInstanceOf[js.Dictionary[Int]].toMap

    val statsString = v8Stats map {
      case
        (
          k @ (
            "heap_size_limit"            |
            "total_heap_size"            |
            "total_physical_size"        |
            "used_heap_size"             |
            "total_heap_size_executable" |
            "total_available_size"
          ),
          v
        ) ⇒
        // NOTE: No decimal separators show (https://github.com/scala-js/scala-js/issues/1619)
        // f"$k: $v%,d bytes"

        def formatInt(value: Int) = value.toString.reverse.grouped(3).to[List].map(_.reverse).reverse mkString ","

        s"$k: ${formatInt(v)} bytes"

      case (k, v) ⇒
        s"$k: $v"
    } mkString ", "

    await(notifyIFTTT(StatusAction, Some(s"V8 stats: $statsString; uptime: ${OS.uptime()} seconds")))
  }

  def main() = ()
}
