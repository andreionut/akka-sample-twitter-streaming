/**
 * This is a fully working example of Twitter's Streaming API client.
 * NOTE: this may stop working if at any point Twitter does some breaking changes to this API or the JSON structure.
 */

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.{Sink, Source}
import com.hunorkovacs.koauth.domain.KoauthRequest
import com.hunorkovacs.koauth.service.consumer.DefaultConsumerService
import org.json4s._
import org.json4s.native.JsonMethods._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TwitterStreamer extends App {

	//Get your credentials from https://apps.twitter.com and replace the values below
	private val consumerKey = sys.env("CONSUMER_KEY")
	private val consumerSecret = sys.env("CONSUMER_SECRET")
	private val accessToken = sys.env("ACCESS_TOKEN")
	private val accessTokenSecret = sys.env("ACCESS_TOKEN_SECRET")
	private val url = "https://stream.twitter.com/1.1/statuses/filter.json"

	implicit val system = ActorSystem("twitter-streamer")
	implicit val materializer = ActorMaterializer()
	implicit val formats = DefaultFormats

	private val consumer = new DefaultConsumerService(system.dispatcher)

	//Filter tweets by a term "london"
	val body = "track=paris"
	val source = Uri(url)

	//Create Oauth 1a header
	val oauthHeader: Future[String] = consumer.createOauthenticatedRequest(
		KoauthRequest(
			method = "POST",
			url = url,
			authorizationHeader = None,
			body = Some(body)
		),
		consumerKey,
		consumerSecret,
		accessToken,
		accessTokenSecret
	) map (_.header)

  def twitterStream() = {
    oauthHeader.map { header =>
      val httpHeaders: List[HttpHeader] = List(
        HttpHeader.parse("Authorization", header) match {
          case ParsingResult.Ok(h, _) => Some(h)
          case _ => None
        },
        HttpHeader.parse("Accept", "*/*") match {
          case ParsingResult.Ok(h, _) => Some(h)
          case _ => None
        }
      ).flatten

      val httpRequest: HttpRequest = HttpRequest(
        method = HttpMethods.POST,
        uri = source,
        headers = httpHeaders,
        entity = HttpEntity(
          contentType = ContentType(MediaTypes.`application/x-www-form-urlencoded`, charset = HttpCharsets.`UTF-8`),
          string = body
        )
      )
      val request = Http().singleRequest(httpRequest)
      request.flatMap { response =>
        if (response.status.intValue() != 200) {
          response.entity.dataBytes.runForeach{ x => println(x.utf8String)}
        } else {
          val source = response.entity.dataBytes
            .scan("")((acc, curr) => if (acc.contains("\r\n")) curr.utf8String else acc + curr.utf8String)
            .filter(_.contains("\r\n"))
          Future(mainStream(source))
        }
      }
    }
  }

  def mainStream(source: Source[String, Any]) = {
    val tweets = source
      .map{ json => parse(json).extract[Tweet] }
      .map(_.text)

    tweets.runWith(Sink.foreach(println))
  }

  twitterStream()
}

