package io.backchat.oauth2

import com.mongodb.casbah._
import org.scala_tools.time._
import org.joda.time.base.{ AbstractDateTime, AbstractInstant, AbstractPartial }
import org.joda.time.field.AbstractReadableInstantFieldProperty
import java.util.Date
import akka.util._
import java.util.concurrent.TimeUnit
import org.joda.time._
import format.DateTimeFormatter
import io.backchat.inflector.InflectorImports
import net.liftweb.json.JsonAST.JValue

object OAuth2Imports extends InflectorImports with com.mongodb.casbah.Imports with commons.Imports with query.Imports {

  // All the code below is to avoid akka and scala time stepping on each others' durations
  // As a bonus there is a single import statement to add in every file instead of 5-6
  type Duration = akka.util.Duration
  val Duration = akka.util.Duration
  type Chronology = org.joda.time.Chronology
  type DateTime = org.joda.time.DateTime
  type DateTimeFormat = org.joda.time.format.DateTimeFormat
  type DateTimeZone = org.joda.time.DateTimeZone
  type Interval = org.joda.time.Interval
  type LocalDate = org.joda.time.LocalDate
  type LocalDateTime = org.joda.time.LocalDateTime
  type LocalTime = org.joda.time.LocalTime
  type Period = org.joda.time.Period
  type Partial = org.joda.time.Partial
  val DateTime = org.scala_tools.time.StaticDateTime
  val DateTimeFormat = org.scala_tools.time.StaticDateTimeFormat
  val DateTimeZone = org.scala_tools.time.StaticDateTimeZone
  val Interval = org.scala_tools.time.StaticInterval
  val LocalDate = org.scala_tools.time.StaticLocalDate
  val LocalDateTime = org.scala_tools.time.StaticLocalDateTime
  val LocalTime = org.scala_tools.time.StaticLocalTime
  val Period = org.scala_tools.time.StaticPeriod
  val Partial = org.scala_tools.time.StaticPartial

  implicit def RichDate(d: Date): RichDate = new org.scala_tools.time.RichDate(d)

  implicit def dt2rdt(dt: org.joda.time.DateTime) = new RichDateTime(dt)
  implicit def RichAbstractDateTime(dt: AbstractDateTime): RichAbstractDateTime = new RichAbstractDateTime(dt)
  implicit def RichAbstractInstant(in: AbstractInstant): RichAbstractInstant = new RichAbstractInstant(in)
  implicit def RichAbstractPartial(pt: AbstractPartial): RichAbstractPartial = new RichAbstractPartial(pt)
  implicit def RichAbstractReadableInstantFieldProperty(pty: AbstractReadableInstantFieldProperty): RichAbstractReadableInstantFieldProperty =
    new RichAbstractReadableInstantFieldProperty(pty)
  implicit def RichChronology(ch: Chronology): RichChronology = new RichChronology(ch)
  implicit def RichDateMidnight(dm: DateMidnight): RichDateMidnight = new RichDateMidnight(dm)
  implicit def RichDateTimeFormatter(fmt: DateTimeFormatter): RichDateTimeFormatter = new RichDateTimeFormatter(fmt)
  implicit def RichDateTimeProperty(pty: DateTime.Property): RichDateTimeProperty = new RichDateTimeProperty(pty)
  implicit def RichDateTimeZone(zone: DateTimeZone): RichDateTimeZone = new RichDateTimeZone(zone)
  implicit def RichDuration(dur: org.joda.time.Duration): RichDuration = new RichDuration(dur)
  implicit def RichInstant(in: Instant): RichInstant = new RichInstant(in)
  implicit def RichLocalDate(ld: LocalDate): RichLocalDate = new RichLocalDate(ld)
  implicit def RichLocalDateProperty(pty: LocalDate.Property): RichLocalDateProperty = new RichLocalDateProperty(pty)
  implicit def RichLocalDateTime(dt: LocalDateTime): RichLocalDateTime = new RichLocalDateTime(dt)
  implicit def RichLocalDateTimeProperty(pty: LocalDateTime.Property): RichLocalDateTimeProperty = new RichLocalDateTimeProperty(pty)
  implicit def RichLocalTime(lt: LocalTime): RichLocalTime = new RichLocalTime(lt)
  implicit def RichLocalTimeProperty(pty: LocalTime.Property): RichLocalTimeProperty = new RichLocalTimeProperty(pty)
  implicit def RichPartial(pt: Partial): RichPartial = new RichPartial(pt)
  implicit def RichPartialProperty(pty: Partial.Property): RichPartialProperty = new RichPartialProperty(pty)
  implicit def RichPeriod(per: Period): RichPeriod = new RichPeriod(per)
  implicit def RichReadableDateTime(dt: ReadableDateTime): RichReadableDateTime = new RichReadableDateTime(dt)
  implicit def RichReadableInstant(in: ReadableInstant): RichReadableInstant = new RichReadableInstant(in)
  implicit def RichReadableInterval(in: ReadableInterval): RichReadableInterval = new RichReadableInterval(in)
  implicit def RichReadablePartial(rp: ReadablePartial): RichReadablePartial = new RichReadablePartial(rp)
  implicit def RichReadablePeriod(per: ReadablePeriod): RichReadablePeriod = new RichReadablePeriod(per)
  implicit def akkadur2dur(dur: akka.util.Duration): org.joda.time.Duration = new org.joda.time.Duration(dur.toMillis)

  trait Classifier[C] {
    type R
    def convert(d: FiniteDuration): R
  }

  object span
  implicit object spanConvert extends Classifier[span.type] {
    type R = FiniteDuration
    def convert(d: FiniteDuration) = d
  }

  object fromNow
  implicit object fromNowConvert extends Classifier[fromNow.type] {
    type R = Deadline
    def convert(d: FiniteDuration) = Deadline.now + d
  }

  implicit def intToDurationInt(n: Int) = new DurationInt(n)
  implicit def longToDurationLong(n: Long) = new DurationLong(n)
  implicit def doubleToDurationDouble(d: Double) = new DurationDouble(d)

  implicit def pairIntToDuration(p: (Int, TimeUnit)) = Duration(p._1, p._2)
  implicit def pairLongToDuration(p: (Long, TimeUnit)) = Duration(p._1, p._2)
  implicit def durationToPair(d: Duration) = (d.length, d.unit)

  /*
     * avoid reflection based invocation by using non-duck type
     */
  class IntMult(i: Int) {
    def *(d: Duration) = d * i
  }
  implicit def intMult(i: Int) = new IntMult(i)

  class LongMult(l: Long) {
    def *(d: Duration) = d * l
  }
  implicit def longMult(l: Long) = new LongMult(l)

  class DoubleMult(f: Double) {
    def *(d: Duration) = d * f
  }
  implicit def doubleMult(f: Double) = new DoubleMult(f)

  implicit def dateTimeOrdered(dt: DateTime) = {
    new Ordered[DateTime] {
      def compare(that: DateTime) = dt.compareTo(dt)
    }
  }

  implicit def string2RicherString(s: String) = new OAuthString(s)
  implicit def jvalue2RicherJValue(j: JValue) = new OAuthJValue(j)
}