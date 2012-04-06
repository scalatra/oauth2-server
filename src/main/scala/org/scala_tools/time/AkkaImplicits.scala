package org.scala_tools.time

import akka.util.{ FiniteDuration, Duration ⇒ AkkaDuration }
import java.util.concurrent.TimeUnit
import org.joda.time.Duration

trait AkkaImplicits {

  implicit def forceAkkaDuration(builder: DurationBuilder): AkkaDuration =
    durationForceAkkaDuration(builder.underlying.toStandardDuration)

  implicit def durationForceAkkaDuration(builder: Duration): AkkaDuration = {
    builder.getMillis match {
      case Long.MaxValue ⇒ AkkaDuration.Inf
      case Long.MinValue ⇒ AkkaDuration.MinusInf
      case 0             ⇒ new FiniteDuration(0, TimeUnit.NANOSECONDS)
      case v             ⇒ AkkaDuration(v, TimeUnit.MILLISECONDS)
    }
  }

}