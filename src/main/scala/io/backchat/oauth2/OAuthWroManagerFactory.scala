//package io.backchat.oauth2
//
//import ro.isdc.wro.manager.factory.BaseWroManagerFactory
//import ro.isdc.wro.model.resource.processor.factory.SimpleProcessorsFactory
//import ro.isdc.wro.extensions.processor.css.LessCssProcessor
//import ro.isdc.wro.model.resource.processor.impl.ExtensionsAwareProcessorDecorator
//import ro.isdc.wro.model.resource.processor.impl.css._
//import com.google.javascript.jscomp.CompilationLevel
//import ro.isdc.wro.extensions.processor.js.{ UglifyJsProcessor, GoogleClosureCompressorProcessor, CoffeeScriptProcessor }
//import ro.isdc.wro.model.resource.processor.impl.js.{ JSMinProcessor, SemicolonAppenderPreProcessor }
//import akka.actor.ActorSystem
//
//class OAuthWroManagerFactory extends BaseWroManagerFactory {
//
//  override def newProcessorsFactory() = {
//    val factory = new SimpleProcessorsFactory
//    factory.addPreProcessor(new CssDataUriPreProcessor())
//    factory.addPreProcessor(new CssUrlRewritingProcessor())
//    factory.addPreProcessor(new CssImportPreProcessor)
//    factory.addPreProcessor(new JawrCssMinifierProcessor)
//    //    if (OAuth2Extension.isDevelopment || OAuth2Extension.isTest) factory.addPreProcessor(new JSMinProcessor)
//    factory.addPreProcessor(new JSMinProcessor)
//    factory.addPreProcessor(ExtensionsAwareProcessorDecorator.decorate(new LessCssProcessor()).addExtension("less"))
//    factory.addPreProcessor(ExtensionsAwareProcessorDecorator.decorate(new CoffeeScriptProcessor()).addExtension("coffee"))
//    //    if (!(OAuth2Extension.isDevelopment || OAuth2Extension.isTest)) factory.addPostProcessor(new UglifyJsProcessor)
//    factory.addPostProcessor(new UglifyJsProcessor)
//    factory
//  }
//}
