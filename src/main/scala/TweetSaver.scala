import akka.actor.ActorSystem
import akka.event.Logging
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

import scala.io.{Source, StdIn}
import scala.util.Random

object TweetSaver {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("tweet-saver")
    implicit val materializer = ActorMaterializer()
    // needed for the future flatMap/onComplete in the end
    implicit val executionContext = system.dispatcher

    val logger = Logging(system, getClass)

    val route =
      path("save") {
        // curl -X POST http://localhost:8080/save -d "tweet text"
        post {
          entity(as[String]) { tweetText =>
            val fakeSaveTime = Source.fromFile("src/main/resources/application.txt").mkString.stripLineEnd.toInt
            val saveTime = fakeSaveTime// + Random.nextInt(fakeSaveTime/10)
            Thread.sleep(saveTime)
            logger.info(s"Time took to save tweet '$tweetText' in $saveTime ms")
            complete("ok!")
          }
        }
      }

    val bindingFuture = Http().bindAndHandle(route, "localhost", 8080)

    println(s"Server online at http://localhost:8080/\nPress RETURN to stop...")
    StdIn.readLine() // let it run until user presses return
    bindingFuture
      .flatMap(_.unbind()) // trigger unbinding from the port
      .onComplete(_ => system.terminate()) // and shutdown when done
  }
}
