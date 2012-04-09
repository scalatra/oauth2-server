package io.backchat.oauth2

import java.util.Date
import org.joda.time.DateTime
import net.liftweb.json.ext.JodaTimeSerializers
import net.liftweb.json.{ Serializer, DateFormat, DefaultFormats }
import java.io.File

class OAuth2Formats extends DefaultFormats {

  override val dateFormat = new DateFormat {
    def format(d: Date) = new DateTime(d).toString(Iso8601DateNoMillis)

    def parse(s: String) = DateFormats.parse(s).map(_.toDate)
  }

  override val customSerializers =
    new json.AkkaDurationSerializer().asInstanceOf[Serializer[_]] ::
      new json.AkkaUuidSerializer().asInstanceOf[Serializer[_]] ::
      JodaTimeSerializers.all.asInstanceOf[List[Serializer[_]]]
}