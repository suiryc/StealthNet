package stealthnet.scala.util.netty

import io.netty.util.concurrent.{Future => nettyFuture}
import io.netty.util.concurrent.GenericFutureListener
import scala.concurrent.{Future, Promise}
import scala.util.{Failure, Success}
import scala.concurrent.Future
import scala.language.implicitConversions

/** Helper to convert netty Future to scala one. */
object NettyFuture {

  implicit def nettyToScala[T](future: nettyFuture[T]): Future[T] = {
    val r = Promise[T]

    future.addListener(new GenericFutureListener[nettyFuture[T]] {
      override def operationComplete(future: nettyFuture[T]) {
        if (future.isSuccess)
          r.success(future.get)
        if (!future.isSuccess)
          r.failure(future.cause)
      }
    })

    r.future
  }

}
