package tweet

import akka.actor.ActorSystem
import akka.stream.scaladsl.Source
import akka.stream.testkit.scaladsl.TestSink
import akka.stream.{ActorMaterializer, ActorMaterializerSettings}
import org.json4s.DefaultFormats
import org.json4s.native.JsonMethods._
import org.scalatest.{FlatSpec, Matchers}

import scala.util.Failure

class StreamTest extends FlatSpec with Matchers {
  implicit val actorSystem = ActorSystem("test")
  implicit val materializer = ActorMaterializer(ActorMaterializerSettings(actorSystem))

  behavior of "TwitterStreamer"

  behavior of "tweetStats"

  it should "count correctly" in {
    val testTweet = StreamSpec.randomTweet
    val tweets = List(testTweet.copy(retweeted_status = None), testTweet, testTweet.copy(retweeted_status = None))
    val testSource = Source(tweets).via(Stream.tweetStats)
    testSource
      .runWith(TestSink.probe[String])
      .request(1)
      .expectNext(s"2 original tweets and 1 retweets. Originality: 66%")
      .expectComplete()
  }

  behavior of "jsonExtract"

  it should "fail silently" in {
    val invalidTweetData = "gsfsd"
    Stream.jsonExtract(invalidTweetData) shouldBe a [Failure[_]]
  }
}

object StreamSpec {
  implicit val formats = DefaultFormats
  def randomTweet: Tweet = {
    val json = scala.io.Source.fromURL(getClass.getResource("/tweet.json")).mkString.stripLineEnd
    parse(json).extract[Tweet]
  }
}
