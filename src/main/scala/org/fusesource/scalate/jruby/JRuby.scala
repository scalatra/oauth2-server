package org.fusesource.scalate.jruby

import org.fusesource.scalate.util.Log
import java.io.{ StringWriter, File }
import scala.collection
import org.jruby.RubyInstanceConfig
import org.jruby.embed.{ LocalContextScope, ScriptingContainer }
import collection.JavaConverters._

/**
 * A simple interface to the jruby interpreter
 */
class JRuby(loadPaths: List[File]) extends Log {

  var container = new ScriptingContainer(LocalContextScope.SINGLETON)
  container.setLoadPaths(loadPaths.map(_.getPath).asJava)
  container.setCompileMode(RubyInstanceConfig.CompileMode.JIT)

  RubyInstanceConfig.FASTEST_COMPILE_ENABLED = true
  RubyInstanceConfig.FASTSEND_COMPILE_ENABLED = true
  RubyInstanceConfig.INLINE_DYNCALL_ENABLED = true

  def run(scriptlet: String*): Either[(Throwable, String), AnyRef] = this.synchronized {
    val errors = new StringWriter
    try {
      container.setErrorWriter(errors)
      Right(container.runScriptlet(scriptlet.mkString("\n")))
    } catch {
      case e: Throwable ⇒
        Left((e, errors.toString))
    }
  }

  def put(name: String, value: AnyRef) = container.put(name, value)

}