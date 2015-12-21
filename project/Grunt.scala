/*
 * Copyright 2015 HM Revenue & Customs
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

import java.net.InetSocketAddress

import sbt._
import play.PlayRunHook

/*
  Grunt runner
*/
object Grunt {
  def apply(base: File): PlayRunHook = {

    object GruntProcess extends PlayRunHook {

      var gruntRun: Option[Process] = None

      override def beforeStarted(): Unit = {
        val log = ConsoleLogger()
        log.info("run npm install...")
        npmProcess("install").!

        log.info("Starting default Grunt task..")
        gruntRun = Some(gruntProcess("default").run())
      }

      override def afterStarted(addr: InetSocketAddress): Unit = {
        gruntProcess("watch").run()
      }

      override def afterStopped(): Unit = {
        // Stop grunt when play run stops
        gruntRun.foreach(p => p.destroy())
        gruntRun = None
      }
    }

    GruntProcess
  }

  def gruntCommand() = Command.args("grunt", "<grunt-command>") { (state, args) =>
    gruntProcess(args:_*) !;
    state
  }
  def gruntProcess(args: String*) = Process("node" :: "node_modules/.bin/grunt" :: args.toList)
  def npmProcess(args: String*) = Process("npm" :: args.toList)

}
