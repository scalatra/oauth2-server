import sbt._
import Keys._
import net.liftweb.json._
import JsonDSL._

/*_*/
object RequireJsPlugin extends Plugin {
  object RequireJsKeys {
    val optimize = TaskKey[Seq[File]]("optimize", "Compile and optimize script source files.")
    val rjs = SettingKey[File]("rjs", "The r.js file to use for optimizing the source files.")
    val createBuildProfile = TaskKey[File]("create-build-profile", "Generate the build profile to use when generating sources")
    val requireJs = TaskKey[Seq[File]]("require-js", "Compile and optimize script source files and deploy to the webapp.")
    val buildProfileFile = SettingKey[File]("build-profile-file", "The build profile base file for the require.js optimizer.")
    val buildProfile = SettingKey[JValue]("build-profile", "The build profile for the require.js optimizer.")
    val buildProfileGenerated = SettingKey[File]("build-profile-generated", "The generated build profile for the require.js optimizer")
    val webApp = SettingKey[File]("webapp-dir", "The directory to copy the files to.")
    val baseUrl = SettingKey[String]("base-url", "The base url for the require js script files")
    val mainConfigFile = SettingKey[Option[File]]("main-config-file", "The main config file for require-js")
    val nodeBin = SettingKey[String]("node-bin", "The location of the node binary")
  }

  import RequireJsKeys._

  private def optimizeTask =
    (target in requireJs,
     rjs in requireJs,
     nodeBin in requireJs,
     includeFilter in requireJs,
     excludeFilter in requireJs,
     createBuildProfile in requireJs, streams) map { (tgt, rjsf, node, incl, excl, bp, log) =>
      val t = tgt.getAbsoluteFile
      if (!t.exists()) IO.createDirectory(t.getAbsoluteFile)
      val cmd = node + " " + rjsf.getAbsolutePath + " -o " + bp.getAbsolutePath
      cmd ! log.log
      tgt.descendentsExcept(incl, excl).get
    }

  private def cleanBuildCache(buildCache: File, fallback: Seq[File], webapp: File) {
    if (!buildCache.exists()) {
      IO.delete(fallback filter (_.exists()))
    } else {
      IO.delete(IO.readLines(buildCache) filterNot (_ == webapp.getAbsolutePath) map file)
    }
  }

  private def copyToWebApp =
    (optimize in requireJs, target in requireJs, webApp in requireJs) map { (files, tgt, webapp) =>
      val buildCache = (tgt / ".." / "requirejs.files.txt").getAbsoluteFile
      val toCopy = files x rebase(tgt.getAbsoluteFile, webapp.getAbsoluteFile)
      val copiedFiles = toCopy.map(_._2.getAbsoluteFile).filterNot(_.getPath == webapp.getAbsolutePath)
      cleanBuildCache(buildCache, copiedFiles, webapp)
      copyAndCacheResult(toCopy, buildCache, webapp.getAbsoluteFile)
      copiedFiles
    }


  def copyAndCacheResult(toCopy: scala.Seq[(File, File)], buildCache: File, webapp: File) {
    IO.copy(toCopy, overwrite = true)
    IO.writeLines(buildCache, toCopy.map(_._2.getAbsolutePath).filterNot(_ == webapp.getAbsolutePath))
  }

  def generateBuildProfile =
    (buildProfileFile in requireJs,
     buildProfile in requireJs,
     buildProfileGenerated in requireJs,
     baseUrl in requireJs,
     mainConfigFile in requireJs,
     sourceDirectory in requireJs,
     target in requireJs,
     streams) map { (bpf, bp, gen, bd, mc, src, tgt, s) =>
      val txt = if (bpf.exists) IO.read(bpf) else ""
      val n = if (txt.startsWith("(")) txt.substring(1) else txt
      val b = if (n.trim().endsWith(")")) n.substring(0, n.length - 1) else n
      val fileJson = if (bpf.exists) Some(parse(b)) else None
      val merged = fileJson map (_ merge bp) getOrElse bp
      val json = merged merge (
        ("appDir" -> src.getAbsolutePath) ~
        ("baseUrl" -> bd) ~
        ("dir" -> IO.relativize(gen.getParentFile, tgt.getAbsoluteFile).getOrElse("requirejs"))
      )
      val oj = mc map (m => json merge (("mainConfigFile" -> m.getAbsolutePath): JValue)) getOrElse json
      IO.write(gen, "(%s)\n" format pretty(render(oj)), append = false)
      gen
    }

  private def requireJsSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(requireJsSettings0 ++ Seq(
      webApp in requireJs <<= (sourceDirectory in c)(_ / "webapp"),
      sourceDirectory in requireJs <<= (sourceDirectory in c)(_ / "requirejs"),
      rjs in requireJs <<= (baseDirectory in c)(_ / "project" / "tools" / "r.js"),
      nodeBin in requireJs := ("which node" !!).trim,
      buildProfileFile in requireJs <<= (baseDirectory in c)(_ / "project" / "requirejs.build.js"),
      buildProfile in requireJs := JNothing,
      buildProfileGenerated in requireJs <<= (target in c)(_ / "requirejs.build.js"),
      target in requireJs <<= (target in c)(_ / "requirejs"),
      baseUrl in requireJs := "js",
      mainConfigFile in requireJs <<= (sourceDirectory in requireJs, baseUrl in requireJs)((a, b) => Some(a / b / "main.js")),
      includeFilter in requireJs := "*",
      excludeFilter in requireJs := "build.txt" || (".*" - ".") || "_*" || HiddenFileFilter,
      watchSources in requireJs <<= (unmanagedSources in requireJs)
    )) ++ Seq(
      watchSources <++= (unmanagedSources in requireJs in c),
      compile in c <<= (compile in c).dependsOn(requireJs in c)
    )

  private def cleanTask =
    (target in requireJs, webApp in requireJs, sourceDirectory in requireJs, unmanagedSources in requireJs) map { (tgt, webapp, src, files) =>
      val buildCache = (tgt / ".." / "requirejs.files.txt").getAbsoluteFile
      val toDelete = files x rebase(src.getAbsoluteFile, webapp.getAbsoluteFile)
      cleanBuildCache(buildCache, toDelete.map(_._2.getAbsoluteFile), webapp)
    }

  def requireJsSettings: Seq[Setting[_]] = requireJsSettingsIn(Compile) ++ requireJsSettingsIn(Test)

  private def requireJsSettings0: Seq[Setting[_]] = Seq(
    unmanagedSources in requireJs <<= (sourceDirectory in requireJs, streams) map { (dir, _) => dir.get },
    createBuildProfile in requireJs <<= generateBuildProfile,
    optimize in requireJs <<= optimizeTask,
    clean in requireJs <<= cleanTask,
    requireJs <<= copyToWebApp
  )


}
/*_*/