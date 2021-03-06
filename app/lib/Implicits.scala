/*
 * Copyright 2014 The Guardian
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package lib

import java.time.ZonedDateTime
import java.util.concurrent.TimeUnit

import com.github.nscala_time.time.Imports._
import org.joda.time.DateTime

import scala.concurrent.duration.FiniteDuration

object Implicits {

  implicit def javaZone2JodaDateTimeZone(zoneId: java.time.ZoneId): org.joda.time.DateTimeZone =
    if (zoneId.getId == "Z") DateTimeZone.UTC else DateTimeZone.forID(zoneId.getId)

  implicit def javaZonedDateTime2JodaDateTime(zonedDateTime: java.time.ZonedDateTime): DateTime =
    new DateTime(zonedDateTime.toInstant.toEpochMilli, zonedDateTime.getZone)

  implicit def jodaInstant2javaInstant(instant: org.joda.time.Instant): java.time.Instant =
    java.time.Instant.ofEpochMilli(instant.getMillis)

  implicit def jodaDateTimeZone2javaZoneId(dateTimeZone: org.joda.time.DateTimeZone): java.time.ZoneId =
    java.time.ZoneId.of(dateTimeZone.getID)

  implicit def jodaDateTime2JavaZonedDateTime(dateTime: DateTime): java.time.ZonedDateTime =
    ZonedDateTime.ofInstant(dateTime.toInstant, dateTime.zone)

  implicit def scalaDuration2javaDuration(dur: scala.concurrent.duration.Duration) = java.time.Duration.ofNanos(dur.toNanos)

  implicit def duration2SDuration(dur: org.joda.time.Duration) = FiniteDuration(dur.getMillis, TimeUnit.MILLISECONDS)

  implicit def javaDuration2SDuration(dur: java.time.Duration) = FiniteDuration(dur.toMillis, TimeUnit.MILLISECONDS)

  implicit def javaDuration2jodaDuration(dur: java.time.Duration) = org.joda.time.Duration.millis(dur.toMillis)

  /*
  implicit class RichIssue(issue: GHIssue) {
    lazy val assignee = Option(issue.getAssignee)

    lazy val labelNames = issue.getLabels.map(_.getName)

    def labelledState(applicableFilter: String => Boolean) = new LabelledState(issue, issue.labelNames.toSet)
  }

  implicit class RichCommitPointer(commitPointer: GHCommitPointer) {
    def asRevCommit(implicit revWalk: RevWalk): RevCommit = commitPointer.getSha.asObjectId.asRevCommit
  }

  implicit class RichPullRequest(pullRequest: GHPullRequest) {
    /**
     * @return interestingPaths which were affected by the pull request
     */
    def affects(interestingPaths: Set[String])(implicit revWalk: RevWalk): Set[String] = {
      implicit val reader = revWalk.getObjectReader
      GitChanges.affectedFolders(pullRequest.getBase.asRevCommit, pullRequest.getHead.asRevCommit, interestingPaths)
    }
  }

  implicit class RichOrg(org: GHOrganization) {

    lazy val membersAdminUrl = s"https://github.com/orgs/${org.getLogin}/members"

    lazy val teamsByName: Map[String, GHTeam] = org.getTeams().toMap
  }

  implicit class RichTeam(team: GHTeam) {
    def ensureHasMember(user: GHUser) {
      if (!team.getMembers.contains(user)) {
        team.add(user)
      }
    }
  }

  */

  val dateTimeFormatter = DateTimeFormat.forPattern("yyyy-MM-dd'T'HH:mm:ssZ")
}
