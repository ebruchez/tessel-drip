package org.bruchez.tessel

import scala.concurrent.{Future, Promise}
import scala.scalajs.js
import scala.scalajs.js.annotation.JSImport

/**
  * Facades for Tessel 2's relay-mono module.
  *
  * https://github.com/tessel/relay-mono/blob/master/index.js
  */

@js.native
@JSImport("relay-mono", JSImport.Namespace)
object RelayMono extends js.Object {
  def use(port: Port, callback: js.Function2[Error, Relay, _] = null): Relay = js.native
}

@js.native
trait Relay extends js.Object with EventEmitter {
  def setState(channel: Int, state: Boolean, callback: js.Function2[Error, js.UndefOr[Boolean], _] = null): Unit = js.native
  def turnOn  (channel: Int, delay: Int,     callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def turnOff (channel: Int, delay: Int,     callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def toggle  (channel: Int,                 callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
  def getState(channel: Int,                 callback: js.Function2[Error, Boolean, _] = null): Unit = js.native
}

object Relay {

  sealed trait Channel { val value: Int }
  case object Channel1 extends { val value = 1 } with Channel
  case object Channel2 extends { val value = 2 } with Channel

  def useF(port: Port): Future[Relay] = {
    val p = Promise[Relay]()
    RelayMono.use(port, (err: Error, relay: Relay) ⇒ {

      if (! js.isUndefined(err) && (err ne null)) {
        p.failure(new RuntimeException(err.message))
      } else {
        p.success(relay)
      }
    })

    p.future
  }

  implicit class RelayOps(val relay: Relay) extends AnyVal {

    // NOTE: The `ready` event is dispatched by `use()` with `setImmediate()`. If this is not called immediately after
    // calling `use()`, then `onReadyF` will block forever. So use with caution and prefer using `useF()` above.
    def onReadyF(): Future[Unit] = {
      val p = Promise[Unit]()
      relay.on("ready", (err: Error) ⇒ {
        if (! js.isUndefined(err) && (err ne null)) {
          p.failure(new RuntimeException(err.message))
        } else {
          p.success(())
        }
      })

      p.future
    }


//    def onLatch(listener: (Int, Boolean) ⇒ Any) = relay.on("latch", listener)

    def setStateF(channel: Channel, state: OnOff): Future[OnOff] = {

      val p = Promise[OnOff]()

      // Unclear why the state passed can be `undefined` at times
      relay.setState(channel.value, state.value, (err: Error, state: js.UndefOr[Boolean]) ⇒ {
        if (! js.isUndefined(err))
          p.failure(new RuntimeException(err.message))
        else
          p.success(state.toOption map OnOff.fromBoolean getOrElse Off)
      })

      p.future
    }

    def stateF(channel: Channel): Future[OnOff] = {

      // TODO: remove code duplication with setState
      val p = Promise[OnOff]()

      relay.getState(channel.value, (err: Error, state: Boolean) ⇒ {
        if (! js.isUndefined(err))
          p.failure(new RuntimeException(err.message))
        else
          p.success(OnOff.fromBoolean(state))
      })

      p.future
    }
  }
}
