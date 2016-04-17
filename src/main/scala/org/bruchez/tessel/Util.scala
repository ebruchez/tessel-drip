package org.bruchez.tessel

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.{ExecutionContext, Future, Promise}
import scala.scalajs.js.timers._
import scala.util.{Failure, Success, Try}

object Util {

  def delay(delay: FiniteDuration): Future[Unit] = {
    val p = Promise[Unit]()
    setTimeout(delay) {
      p.success(())
    }
    p.future
  }

  implicit class FutureOps[T](val f: Future[T]) extends AnyVal {
    def toTry(implicit executor: ExecutionContext): Future[Try[T]] =
      f map Success.apply recover PartialFunction(Failure.apply)
  }

}
