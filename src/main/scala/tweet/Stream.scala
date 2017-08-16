package tweet

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.scaladsl.{Broadcast, Flow, GraphDSL, RunnableGraph, Sink, Source}
import akka.stream.{ActorMaterializer, ClosedShape}
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

object Stream {
  implicit val system = ActorSystem("twitter-streamer")
  implicit val materializer = ActorMaterializer()
  implicit val formats = DefaultFormats

  case class TweetCounts(original: Int, retweet: Int) {
    val originality = if (original + retweet > 0) {
      100 * original / (original + retweet)
    } else {
      0
    }

    def increment(count: TweetCounts): TweetCounts = TweetCounts(original + count.original, retweet + count.retweet)
  }

  def jsonExtract(json: String): Try[Tweet] = Try(parse(json).extract[Tweet]) match {
    case Success(t) => Success(t)
    case Failure(e) =>
      println("-----")
      println(e.getMessage)
      println(json)
      Failure(e)
  }

  def extractTweet: Flow[String, Tweet, Any] = Flow[String]
    .map(jsonExtract)
    .collect { case Success(t) => t }

  def saveTweet: Sink[Tweet, Any] = Sink.ignore

  def tweetStats: Flow[Tweet, String, Any] = Flow[Tweet]
    .map { tweet => if (tweet.retweeted_status.isDefined) { TweetCounts(0,1) } else { TweetCounts(1,0) } }
    .groupedWithin(20, 10.seconds)
    .map { tweetCounts =>
      tweetCounts.reduceLeft(_.increment(_))
    }
    .statefulMapConcat{ () =>
      var totalCount = TweetCounts(0,0)
      count => {
        totalCount = totalCount.increment(count)
        List(totalCount)
      }
    }
    .map { counts =>
      s"${counts.original} original tweets and ${counts.retweet} retweets. Originality: ${counts.originality}%"
    }

  def mainStream(source: Source[String, Any]) = {
    val g = RunnableGraph.fromGraph(GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] =>
      import GraphDSL.Implicits._

      val bcast = builder.add(Broadcast[Tweet](2))
      source ~> extractTweet ~> bcast ~> saveTweet
                                bcast ~> tweetStats ~> Sink.foreach(println)

      ClosedShape
    })
    g.run()
  }
}
