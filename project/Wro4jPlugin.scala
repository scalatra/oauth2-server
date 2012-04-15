//import sbt._
//import Keys._
//
//object Wro4jPlugin extends Plugin {
//
//  object Wro4jKeys {
//    val minimize = SettingKey[Boolean]("minimize", "Turns on the minimization by applying compressor")
//    val targetGroups = SettingKey[Seq[String]]("target-groups", "Comma separated value of the group names from wro.xml to process. If none is provided, all groups will be processed.")
//    val ignoreMissingResources = SettingKey[Boolean]("ignore-missing-resources", "Ignore missing resources")
//    val wroFile = SettingKey[File]("wro-file", "The path to the wro model file.")
//    val contextFolder = SettingKey[File]("context-folder", "Folder used as a root of the context relative resources.")
//    val destinationFolder = SettingKey[File]("destination-folder", "Where to store the processed result. By default uses the folder named [wro] under the context path.")
//    val preprocessors = SettingKey[Seq[String]]("pre-processors", "Comma separated list of processors")
//    val wro4j = TaskKey[Seq[File]]("wro4j", "Run the web resource optimizer")
//  }
//}
//
