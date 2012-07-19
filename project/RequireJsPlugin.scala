import sbt._
import Keys._

/*_*/
object RequireJsPlugin extends Plugin {
  object RequireJsKeys {
    val requirejs = TaskKey[Seq[File]]("require-js", "Compile and optimize script source files.")
    val buildProfile = SettingKey[File]("require-js-build-profile", "The build profile for the require.js optimizer.")
  }

  import RequireJsKeys._

  private def optimizeTask = (baseDirectory in requirejs,  buildProfile in requirejs, streams) map { (base, bp, log) =>
    ("project/build-backbone " + bp.getAbsolutePath) ! log.log
    Seq.empty[File]
  }

  def requireJsSettingsIn(c: Configuration): Seq[Setting[_]] =
    inConfig(c)(requireJsSettings0 ++ Seq(
      sourceDirectory in requirejs <<= (sourceDirectory in c)(_ / "requirejs"),
      buildProfile in requirejs <<= (baseDirectory in c)(_ / "project" / "requirejs.build.js"),
      watchSources in requirejs <<= (unmanagedSources in requirejs)
    )) ++ Seq(
      watchSources <++= (unmanagedSources in requirejs in c),
      compile in c <<= (compile in c).dependsOn(requirejs in c)
    )

  def requireJsSettings: Seq[Setting[_]] = requireJsSettingsIn(Compile) ++ requireJsSettingsIn(Test)

  def requireJsSettings0: Seq[Setting[_]] = Seq(
    unmanagedSources in requirejs <<= (sourceDirectory in requirejs, streams) map { (dir, _) => dir.get },
    requirejs <<= optimizeTask
  )
}
/*_*/