//package wro4j
//
//import ro.isdc.wro.extensions.processor.css.LessCssProcessor
//import ro.isdc.wro.extensions.processor.js.{UglifyJsProcessor, CoffeeScriptProcessor}
//import ro.isdc.wro.manager.factory.standalone.{DefaultStandaloneContextAwareManagerFactory, StandaloneWroManagerFactory}
//import ro.isdc.wro.model.factory.WroModelFactory
//import ro.isdc.wro.model.group.Group
//import ro.isdc.wro.model.resource.processor.factory.SimpleProcessorsFactory
//import ro.isdc.wro.model.resource.processor.impl.css.{JawrCssMinifierProcessor, CssImportPreProcessor, CssUrlRewritingProcessor, CssDataUriPreProcessor}
//import ro.isdc.wro.model.resource.processor.impl.ExtensionsAwareProcessorDecorator
//import ro.isdc.wro.model.resource.processor.impl.js.JSMinProcessor
//import ro.isdc.wro.model.resource.processor.{ResourcePostProcessor, ResourcePreProcessor}
//import ro.isdc.wro.model.resource.Resource
//import ro.isdc.wro.model.WroModel
//import sbt._
//import Keys._
//import ro.isdc.wro.manager.factory.{WroManagerFactory, BaseWroManagerFactory}
//
//object Plugin extends Plugin {
//
//  object Wro4jKeys {
//    val minimize = SettingKey[Boolean]("minimize", "Wether to minimize the files or not")
//    val preProcessors = SettingKey[Seq[ResourcePreProcessor]]("pre-processors", "The preprocessors for wro4j to use")
//    val postProcessors = SettingKey[Seq[ResourcePostProcessor]]("post-processors", "The postprocessors for wro4j to use")
//    val groups = SettingKey[Seq[Group]]("groups", "The resource groups to use with wro4j")
//    val managerFactory = SettingKey[WroManagerFactory]("manager-factory", "The wro4j manager factory to use with the compiler")
//    val wro4j = TaskKey[Seq[File]]("wro4j", "Run the web resource optimizer")
//  }
//
//  private case class Wro4jPluginContext(
//    preProcessors: Seq[ResourcePreProcessor],
//    postProcessors: Seq[ResourcePostProcessor],
//    groups: Seq[Group])
//
//  private class Wro4jPluginStandaloneManagerFactory(ctxt: Wro4jPluginContext) extends DefaultStandaloneContextAwareManagerFactory {
//
//    override def newProcessorsFactory() = {
//      val factory = new SimpleProcessorsFactory
//      ctxt.preProcessors foreach factory.addPreProcessor
//      ctxt.postProcessors foreach factory.addPostProcessor
//      factory
//    }
//
//    override def newModelFactory() = new WroModelFactory {
//      import collection.JavaConverters._
//      def destroy() {}
//
//      def create() = {
//        val model = new WroModel
//        ctxt.groups foreach model.addGroup
//        model
//      }
//    }
//  }
//
//  private class Compiler(factory: WroManagerFactory) {
//    def compile() {
//
//    }
//  }
//}
//
