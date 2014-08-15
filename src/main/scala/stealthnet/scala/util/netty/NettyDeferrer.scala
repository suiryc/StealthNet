package stealthnet.scala.util.netty

import io.netty.channel.ChannelHandlerContext
import scala.concurrent.{ExecutionContext, Future}
import scala.reflect.ClassTag
import scala.util.Try

/**
 * Netty deferrer.
 *
 * Helps deferring a `Future` execution by submitting it to a netty event
 * executor.
 */
object NettyDeferrer {

  def defer[T](ctx: ChannelHandlerContext, future: Future[T])(onComplete: Try[T] => Unit) {
    future.onComplete(onComplete)(ExecutionContext.fromExecutorService(ctx.executor()))
  }

  def defer[T](ctx: ChannelHandlerContext, future: Future[_])(onComplete: Try[T] => Unit)(implicit tag: ClassTag[T]) {
    defer(ctx, future.mapTo[T])(onComplete)
  }

}
