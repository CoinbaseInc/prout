package lib

import com.madgag.git._
import lib.Config.RepoConfig
import org.eclipse.jgit.lib.ObjectReader
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.treewalk.TreeWalk
import play.api.libs.json.JsResult

object ConfigFinder {
  
  val ProutConfigFileName = ".prout.json"
  
  private val configFilter: TreeWalk => Boolean = w => {
    w.isSubtree || w.getNameString == ProutConfigFileName
  }

  /**
   *
   * @return treewalk that only returns prout config files
   */
  def configTreeWalk(c: RevCommit)(implicit reader: ObjectReader): TreeWalk = walk(c.getTree)(configFilter)

  def configIdMapFrom(c: RevCommit)(implicit reader: ObjectReader) = configTreeWalk(c).map { tw =>
    val configPath = tw.slashPrefixedPath
    configPath.reverse.dropWhile(_ != '/').reverse -> tw.getObjectId(0)
  }.toMap

  def config(c: RevCommit)(implicit reader: ObjectReader): RepoConfig = {
    val checkpointsByNameByFolder: Map[String, JsResult[ConfigFile]] = configIdMapFrom(c).mapValues(Config.readConfigFrom)
    println(s"Checkpoints By Name, By Folder : $checkpointsByNameByFolder")
    RepoConfig(checkpointsByNameByFolder)
  }
}
