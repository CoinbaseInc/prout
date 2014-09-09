package lib

import java.util.concurrent.TimeUnit

import com.github.nscala_time.time.Imports._
import com.madgag.git._
import lib.LabelledState._
import org.eclipse.jgit.revwalk.{RevCommit, RevWalk}
import org.kohsuke.github.GHPullRequest
import play.api.Logger
import play.api.libs.concurrent.Akka
import play.api.Play.current
import play.twirl.api.Html
import scala.collection.immutable.Seq
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

sealed trait PullRequestDeploymentStatus {
  def labelFor(site: Site) = getClass.getSimpleName.dropRight(1) + "-on-" + site.label
}

object PullRequestDeploymentStatus {
  val all = Set[PullRequestDeploymentStatus](Seen, Pending, Overdue)

  def fromLabels(labels: Set[String], site: Site): Option[PullRequestDeploymentStatus] =
    PullRequestDeploymentStatus.all.find(s => labels(s.labelFor(site)))
}

sealed trait NotSeenOnSite extends PullRequestDeploymentStatus

case object Seen extends PullRequestDeploymentStatus

case object Pending extends NotSeenOnSite

case object Overdue extends NotSeenOnSite


case class DeploymentProgressSnapshot(repoSnapshot: RepoSnapshot, siteSnapshot: SiteSnapshot) {
  private implicit val system = Akka.system

  private def doAfterSmallDelay(f: => Unit): Unit = {
    system.scheduler.scheduleOnce(concurrent.duration.Duration(1, TimeUnit.SECONDS))(f)
  }

  val OverdueThreshold = 15.minutes

  val WorthyOfCommentWindow = 6.hours

  def goCrazy(): Future[Seq[PullRequestSiteCheck]] = Future.traverse(repoSnapshot.mergedPullRequests)(handlePR)

  def handlePR(pr : GHPullRequest): Future[PullRequestSiteCheck] = Future {
    Logger.trace(s"handling ${pr.getNumber}")
    val issueHack = repoSnapshot.repo.getIssue(pr.getNumber)
    val labelledState = issueHack.labelledState(_ => true)
    val existingStateOpt = PullRequestDeploymentStatus.fromLabels(labelledState.applicableLabels, siteSnapshot.site)

    def messageOptFor(prsc: PullRequestSiteCheck) = {
      val boo: PartialFunction[PullRequestDeploymentStatus, Html] = {
        case Seen =>
          views.html.ghIssues.seen(prsc)
        case Overdue =>
          views.html.ghIssues.overdue(prsc)
      }

      boo.lift(prsc.currentState).map(_.body.replace("\n", ""))
    }

    val prsc = PullRequestSiteCheck(pr, siteSnapshot, repoSnapshot.gitRepo)

    existingStateOpt match {
      case Some(Seen) => Logger.trace(s"Ignoring previously Seen PR ${pr.getNumber} currently : $prsc")
      case Some(prsc.currentState) => Logger.trace(s"Ignoring unchanged PR ${pr.getNumber} currently : $prsc")
      case _ =>
        Logger.debug(pr.getNumber+" "+messageOptFor(prsc).toString)

        // update labels before comments - looks better on pull request page
        labelledState.updateLabels(Set(prsc.label))

        if (prsc.timeSinceMerge < WorthyOfCommentWindow) {
          for (message <- messageOptFor(prsc)) {
            doAfterSmallDelay {
              pr.comment(message)
            }
          }
        }
    }

    prsc
  }

  def isVisibleOnSite(pr: GHPullRequest): Boolean = {
    implicit val w: RevWalk = new RevWalk(repoSnapshot.gitRepo)
    val prCommit: RevCommit = pr.getHead.getSha.asObjectId.asRevCommit
    val siteCommit: RevCommit = siteSnapshot.commitId.get.asRevCommit

    val isVisible = w.isMergedInto(prCommit,siteCommit)

    Logger.info(s"prCommit=${prCommit.name()} siteCommit=${siteCommit.name()} isVisible=$isVisible")
    isVisible
  }
}
