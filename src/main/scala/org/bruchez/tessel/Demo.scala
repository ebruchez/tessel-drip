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
object Demo extends js.JSApp {

  val IrrigationDuration    = 5.minutes
  val RainThresholdMm       = 10

  val IFTTTKey              = APIKeys.IFTTTKey
  val WeatherUndergroundKey = APIKeys.WeatherUndergroundKey
  val DarkSkyKey            = APIKeys.DarkSkyKey // https://darksky.net/dev/account

  val WeatherLatitude       = 37.5606821
  val WeatherLongitude      = -122.2518543

  val RelayChannel          = Relay.Channel2
  val LedChannel            = 2
  val IFTTTEventName        = "DripEvent"

  val relay = RelayMono.use(Tessel.port.A)

  sealed trait DripAction { val name: String }
  case object OnAction      extends { val name = "on"  }     with DripAction
  case object OffAction     extends { val name = "off" }     with DripAction
  case object SkipAction    extends { val name = "skip" }    with DripAction
  case object WeatherAction extends { val name = "weather" } with DripAction
  case object StatusAction  extends { val name = "status" }  with DripAction

  def weatherUndergroundUrl =
    s"http://api.wunderground.com/api/$WeatherUndergroundKey/forecast/q/94404.json"

  def darkSkyUrl =
    s"https://api.darksky.net/forecast/$DarkSkyKey/$WeatherLatitude,$WeatherLongitude?units=si&exclude=minutely,hourly,alerts,flags"

  def notificationUrl(eventName: String, message: Option[String]) =
    s"https://maker.ifttt.com/trigger/$IFTTTEventName/with/key/$IFTTTKey?value1=$eventName&value2=${message.getOrElse("")}"

  case class WeatherDetails(highC: Double, lowC: Double, willRain: Boolean)

  def weatherDetailsFromWeatherUndergroundJson(json: js.Dynamic) = {
    val dayForecast = json.forecast.simpleforecast.forecastday.selectDynamic("0")

    val highC  = dayForecast.high.celsius.asInstanceOf[Double]
    val lowC   = dayForecast.low.celsius.asInstanceOf[Double]
    val rainMm = dayForecast.qpf_allday.mm.asInstanceOf[Double]

    WeatherDetails(highC, lowC, rainMm >= RainThresholdMm)
  }

  def weatherDetailsFromDarkSkyJson(json: js.Dynamic) = {
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
            Try(weatherDetailsFromDarkSkyJson(json.asInstanceOf[js.Dynamic]))
          case Failure(t) ⇒
            Failure(t)
        }
      case Failure(t) ⇒
        Failure(t)
    }
  }

  def notifyIFTTT(event: DripAction, message: Option[String]) = async {
    await(NodeFetch(notificationUrl(event.name, message), null).toFuture.toTry) match {
      case Success(res) ⇒ println(s"got response from IFTTT: ${res.status}")
      case Failure(t)   ⇒ println(s"failure calling IFTTT: ${t.getMessage}")
    }
  }

  def turnOn() = async {
    println("Turning on")
    await(relay.setStateF(RelayChannel, On).toTry) match {
      case Success(_) ⇒ Tessel.led(LedChannel).on()
      case Failure(_) ⇒
    }
  }

  def turnOff() = async {
    println("Turning off")
    await(relay.setStateF(RelayChannel, Off).toTry) match {
      case Success(_) ⇒ Tessel.led(LedChannel).off()
      case Failure(_) ⇒
    }
  }

  def irrigateProcess(reason: String) = async {
    await(turnOn())
    await(notifyIFTTT(OnAction, Some(s"for duration $IrrigationDuration because $reason")))
    await(delay(IrrigationDuration))
    await(turnOff())
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

  def sendHealthStatus() = async {

    val v8Stats = V8.getHeapStatistics().asInstanceOf[js.Dictionary[Int]].toMap

    // Could also use JSON.stringify()
    val statsString = v8Stats map { case (k, v) ⇒ s"$k: $v" } mkString ", "

    await(notifyIFTTT(StatusAction, Some(s"V8 stats: $statsString; uptime: ${OS.uptime()}")))
  }

  def main(): Unit = async {

    LEDBlinker.start()

    await(notifyIFTTT(StatusAction, Some(s"starting Tessel ${OS.hostname()} at ${new js.Date()} with node version ${g.process.version}")))
    await(sendHealthStatus())
    await(relay.onReadyF())
    await(turnOff())
    await(notifyIFTTT(StatusAction, Some(s"scheduling jobs")))

    NodeSchedule.scheduleJob("0 3,15,21 * * *", sendHealthStatus _)
    NodeSchedule.scheduleJob(js.Dynamic.literal(hour = 12, minute = 0),  irrigateProcessCheckWeather _)  // 12:00 GMT which is 5:00 PDT
  }
}
