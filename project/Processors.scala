/**
 * A SBT Plugin for wro4j (http://code.google.com/p/wro4j/)
 *
 * Copyright 2012 David Heidrich
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import _root_.ro.isdc.wro.extensions.processor.css._
import _root_.ro.isdc.wro.extensions.processor.css.{YUICssCompressorProcessor, SassCssProcessor, LessCssProcessor}
import _root_.ro.isdc.wro.model.resource.processor.decorator._
import _root_.ro.isdc.wro.model.resource.processor.decorator.{ProcessorDecorator, ExtensionsAwareProcessorDecorator, CopyrightKeeperProcessorDecorator}
import _root_.ro.isdc.wro.model.resource.processor.impl.js.{SemicolonAppenderPreProcessor, JSMinProcessor}
import _root_.ro.isdc.wro.model.resource.processor.{ResourcePreProcessor, ResourcePostProcessor}
import _root_.ro.isdc.wro.util.provider.ConfigurableProviderSupport
import collection.immutable.HashMap
import _root_.ro.isdc.wro.model.resource.processor.impl.css._
import _root_.ro.isdc.wro.model.resource.processor.impl.js.{JSMinProcessor, SemicolonAppenderPreProcessor}
import scala.collection.JavaConverters._
import _root_.ro.isdc.wro.model.resource.processor.{ResourcePostProcessor, ResourcePreProcessor}
import ro.isdc.wro.extensions.processor.js
import js.{CoffeeScriptProcessor, UglifyJsProcessor}
import js.{UglifyJsProcessor, CoffeeScriptProcessor}

/**
 * Provides Common wro4j Processors for daily use :)
 *
 * You may create your own provider and extend this class
 * (overwrite [[com.bowlingx.sbt.plugins.wro4j.Processors.processors]])
 */
class OAuth2Processors extends ConfigurableProviderSupport {

  object AdditionalProviders {
    val COPYRIGHT_KEEPER = "copyrightMin"
  }

  import AdditionalProviders._

  override def providePreProcessors() = mapAsJavaMapConverter(processors).asJava

  override def providePostProcessors() = mapAsJavaMapConverter(
    processors.map(v => v._1 -> new ProcessorDecorator(v._2).asInstanceOf[ResourcePostProcessor])).asJava

  /**
   * A list of usefull default Providers
   * @return
   */
  protected def processors: HashMap[String, ResourcePreProcessor] = {
    HashMap(
      CssUrlRewritingProcessor.ALIAS -> new CssUrlRewritingProcessor(),
      CssImportPreProcessor.ALIAS -> new CssImportPreProcessor(),
      SemicolonAppenderPreProcessor.ALIAS -> new SemicolonAppenderPreProcessor(),
      LessCssProcessor.ALIAS -> ExtensionsAwareProcessorDecorator.decorate(new LessCssProcessor()).addExtension("less"),
      CssDataUriPreProcessor.ALIAS -> new CssDataUriPreProcessor(),
      COPYRIGHT_KEEPER -> CopyrightKeeperProcessorDecorator.decorate(new JSMinProcessor()),
      UglifyJsProcessor.ALIAS_UGLIFY -> new UglifyJsProcessor(),
      CoffeeScriptProcessor.ALIAS -> ExtensionsAwareProcessorDecorator.decorate(new CoffeeScriptProcessor()).addExtension("coffee"),
      SassCssProcessor.ALIAS -> ExtensionsAwareProcessorDecorator.decorate(new SassCssProcessor()).addExtension("sass").addExtension("scss"),
      CssDataUriPreProcessor.ALIAS -> new CssDataUriPreProcessor(),
      JawrCssMinifierProcessor.ALIAS -> new JawrCssMinifierProcessor(),
      JSMinProcessor.ALIAS -> new JSMinProcessor(),
      CssMinProcessor.ALIAS -> new CssMinProcessor
    )
  }
}