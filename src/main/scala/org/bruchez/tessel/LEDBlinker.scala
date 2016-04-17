package org.bruchez.tessel

import scala.concurrent.duration._
import scala.scalajs.js
import scala.scalajs.js.timers.SetIntervalHandle

object LEDBlinker {

  private var ledOn = false
  private var handle: Option[SetIntervalHandle] = None

  def start(): Unit = {
    if (handle.isEmpty)
      handle = Some(
        js.timers.setInterval(1.second) {
          if (ledOn)
            Tessel.led(3).off()
          else
            Tessel.led(3).on()

          ledOn = ! ledOn
        }
      )
  }

  def stop(): Unit = {
    handle foreach js.timers.clearInterval
    handle = None
  }
}