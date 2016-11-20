import akka.actor.ActorSystem
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import akka.stream.scaladsl.{Sink, Source}
import akka.stream.testkit.scaladsl.{TestSink, TestSource}
import akka.testkit.TestProbe
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.scalatest.FlatSpec


class TwitterStreamerSpec extends FlatSpec {
  implicit val actorSystem = ActorSystem("test")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem))

  behavior of "TwitterStreamer"

  behavior of "tweetStats"

  it should "count correctly" in {
    val testTweet = TwitterStreamerSpec.randomTweet
    val tweets = List(testTweet.copy(retweeted_status = None), testTweet, testTweet.copy(retweeted_status = None))
    val testSource = Source(tweets).via(TwitterStreamer.tweetStats)
    testSource
      .runWith(TestSink.probe[String])
      .request(1)
      .expectNext(s"2 original tweets and 1 retweets. Originality: 66%")
      .expectComplete()
  }
}

object TwitterStreamerSpec {
  implicit val formats = DefaultFormats
  def randomTweet: Tweet = {
    val json = scala.io.Source.fromURL(getClass.getResource("tweet.json")).mkString.stripLineEnd
    parse(json).extract[Tweet]
  }
}
