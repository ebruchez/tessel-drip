package org.bruchez.tessel

import org.scalajs.dom.experimental.{RequestInit, Response, _}

import scala.scalajs.js
import scala.scalajs.js.Promise
import scala.scalajs.js.annotation.JSImport

@js.native
@JSImport("node-fetch", JSImport.Namespace)
object NodeFetch extends js.Function2[RequestInfo, RequestInit, js.Promise[Response]] {
  def apply(arg1: RequestInfo, arg2: RequestInit): Promise[Response] = js.native
}

@js.native
trait NodeFetchResponse extends Response {
  def buffer(): js.Promise[Buffer] // "buffer() is a node-fetch only API"
}
