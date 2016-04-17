package org.bruchez.tessel

import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("node-schedule", JSImport.Namespace)
object NodeSchedule extends js.Object {

  def scheduleJob(schedule: js.Any, cb: js.Function): Unit = js.native

//  implicit class NodeScheduleOps(val s: NodeSchedule) extends AnyVal {
//    def scheduleJob(schedule: js.Object, cb: () ⇒ Any): Unit = s.scheduleJob(schedule, cb)
//  }
}