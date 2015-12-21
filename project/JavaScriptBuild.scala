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

import sbt._
import com.typesafe.sbt.packager.Keys._

/**
 * Build of UI in JavaScript
 */
object JavaScriptBuild {

  import play.PlayImport.PlayKeys._

  val gruntBuild = TaskKey[Int]("grunt-build")
  val gruntWatch = TaskKey[Int]("grunt-watch")
  val npmInstall = TaskKey[Int]("npm-install")

  val javaScriptUiSettings = Seq(
    npmInstall := Grunt.npmProcess("install").run().exitValue(),
    gruntBuild := Grunt.gruntProcess("build").run().exitValue(),
    gruntWatch := Grunt.gruntProcess("watch").run().exitValue(),

    gruntBuild <<= gruntBuild dependsOn npmInstall,

    // runs grunt before staging the application
    dist <<= dist dependsOn gruntBuild,

    // Turn off play's internal less compiler
    lessEntryPoints := Nil
  )

  def npmCommand() = Command.args("npm", "<npm-command>") { (state, args) =>
    Process("npm" :: args.toList) !;
    state
  }

}
