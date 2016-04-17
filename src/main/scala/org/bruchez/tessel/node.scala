package org.bruchez.tessel

import scala.scalajs.js
import scala.scalajs.js.annotation.{JSBracketAccess, JSImport}

/**
  * Facades for a few Node.js APIs.
  */

@js.native
trait Error extends js.Object {
  def message: String = js.native
}

@js.native
trait EventEmitter extends js.Object {
  def on                (typ: String, listener: js.Function): EventEmitter = js.native
  def addListener       (typ: String, listener: js.Function): EventEmitter = js.native
  def removeListener    (typ: String, listener: js.Function): EventEmitter = js.native
  def once              (typ: String, listener: js.Function): EventEmitter = js.native

  def removeAllListeners(typ: String)                       : EventEmitter = js.native

  def listeners         (typ: String)                       : js.Array[js.Function] = js.native
  def emit              (typ: String, args: js.Any*)        : Boolean = js.native
}

@js.native
trait HeapStatistics extends js.Object {
  val totalHeapSize          : Int = js.native
  val totalHeapSizeExecutable: Int = js.native
  val totalPhysicalSize      : Int = js.native
  val totalAvailableSize     : Int = js.native
  val usedHeapSize           : Int = js.native
  val heapSizeLimit          : Int = js.native
}

@js.native
@JSImport("v8", JSImport.Namespace)
object V8 extends js.Object {
  def getHeapStatistics(): HeapStatistics = js.native
  def setFlagsFromString(flags: String): Unit = js.native
  // NOTE: Node 6 has `getHeapSpaceStatistics()`.
}

@js.native
trait Times extends js.Object {
  val user            : Int     = js.native
  val nice            : Int     = js.native
  val sys             : Int     = js.native
  val idle            : Int     = js.native
  val irq             : Int     = js.native
}

@js.native
trait CPU extends js.Object {
  val model           : String  = js.native
  val speed           : Int     = js.native
  val times           : Times   = js.native
}

@js.native
trait Address extends js.Object {
  val address         : String  = js.native
  val netmask         : String  = js.native
  val family          : String  = js.native
  val mac             : String  = js.native
  val internal        : Boolean = js.native
}

@js.native
@JSImport("os", JSImport.Namespace)
object OS extends js.Object {
  def EOL()               : String                           = js.native
  def arch()              : String                           = js.native
  def cpus()              : js.Array[CPU]                    = js.native
  def endianness()        : String                           = js.native
  def freemem()           : Int                              = js.native
  def homedir()           : String                           = js.native
  def hostname()          : String                           = js.native
  def loadavg()           : js.Array[Double]                 = js.native
  def networkInterfaces() : js.Dictionary[js.Array[Address]] = js.native
  def platform()          : String                           = js.native
  def release()           : String                           = js.native
  def tmpdir()            : String                           = js.native
  def totalmem()          : Int                              = js.native
  def `type`()            : String                           = js.native
  def uptime()            : Int                              = js.native
}


@js.native
class Buffer(array: js.Array[_]) extends js.Object {
  @JSBracketAccess
  def apply(index: Int): Int = js.native

  @JSBracketAccess
  def update(index: Int, v: Int): Unit = js.native

  def length                              : Int    = js.native
  def slice(start: Int, end: Int = length): Buffer = js.native
  def copy(target: Buffer, targetStart: Int = 0, sourceStart: Int = 0, sourceEnd: Int = length): Int = js.native
}