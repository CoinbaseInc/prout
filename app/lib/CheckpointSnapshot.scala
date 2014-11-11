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

import com.madgag.git._
import lib.Config.Checkpoint
import org.eclipse.jgit.lib.ObjectId
import org.joda.time.{DateTime, ReadableInstant}
import play.api.Logger
import play.api.libs.ws.WS

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent._

object CheckpointSnapshot {

  val hexRegex = """\b\p{XDigit}{40}\b""".r

  def apply(checkpoint: Checkpoint): Future[Option[ObjectId]] = {
    import play.api.Play.current

    WS.url(checkpoint.url.toString).get().map(resp => hexRegex.findFirstIn(resp.body).map(_.asObjectId)).andThen {
      case ci => Logger.info(s"Site '${checkpoint.name}' commit id: ${ci.map(_.map(_.name()))}")
    }
  }
}

case class CheckpointSnapshot(
  checkpoint: Checkpoint,
  commitId: Option[ObjectId],
  time: ReadableInstant = DateTime.now
)
