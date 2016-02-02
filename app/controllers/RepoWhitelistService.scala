package controllers

import java.util.concurrent.atomic.AtomicReference

import com.madgag.scalagithub.GitHub._
import com.madgag.scalagithub.model.{Repo, RepoId}
import com.typesafe.scalalogging.LazyLogging
import lib.Bot
import lib.ConfigFinder.ProutConfigFileName
import play.api.Logger
import play.api.Play.current
import play.api.libs.concurrent.Akka

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration._

case class RepoWhitelist(allKnownRepos: Set[RepoId], publicRepos: Set[RepoId])

object RepoWhitelistService extends LazyLogging {
  implicit val github = Bot.github

  lazy val repoWhitelist = new AtomicReference[Future[RepoWhitelist]](getAllKnownRepos)

  def whitelist(): Future[RepoWhitelist] = repoWhitelist.get()

  val permissionsThatCanPush = Set("admin", "push")

  private def hasProutConfigFile(repo: Repo): Future[Boolean] = for {
    tree <- repo.trees2.getRecursively(s"heads/${repo.default_branch}")
  } yield tree.tree.exists(_.path.endsWith(ProutConfigFileName))

  private def getAllKnownRepos: Future[RepoWhitelist] = for { // check this to see if it always expends quota...
    allRepos <- github.listRepos(sort="pushed", direction = "desc").all()
    proutRepos <- Future.traverse(allRepos.filter(_.permissions.exists(_.push))) { repo =>
      hasProutConfigFile(repo).map(hasConfig => if (hasConfig) Some(repo) else None)
    }.map(_.flatten.toSet)
  } yield RepoWhitelist(proutRepos.map(_.repoId), proutRepos.filterNot(_.`private`).map(_.repoId))


  def start() {
    Logger.info("Starting background repo fetch")
    Akka.system.scheduler.schedule(1.second, 60.seconds) {
      repoWhitelist.set(getAllKnownRepos)
      github.checkRateLimit().foreach(resp => logger.info(resp.rateLimit.statusOpt))
    }
  }

}
