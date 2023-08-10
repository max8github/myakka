package org.example.akka.streams

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Supervision}

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

  object Graphs extends App {
    implicit val system: ActorSystem = ActorSystem("graphThatLogsErrors")

  final case class Author(handle: String)

  final case class Person(first: String, last: String) {
    override def toString: String = s"$first $last"
  }

  final case class Tweet(author: Author, timestamp: Long, body: String)

  val input = Source(
    Tweet(Author("roland.kuhn"), System.currentTimeMillis, "If anything can go wrong, it will.") ::
      Tweet(Author("patrik.norwall"), System.currentTimeMillis, "Nothing is as easy as it looks.") ::
      Tweet(Author("andrzej.ludwikowski"), System.currentTimeMillis, "All work expands to fill the time allowed.") ::
      Tweet(Author("johan.andrén"), System.currentTimeMillis, "The one emergency for which you are fully prepared will never happen.") ::
      Tweet(Author("enno.runne"), System.currentTimeMillis, "The likelihood of success is inversely proportional to how important the project is.") ::
      Tweet(Author("peter.vlugter"), System.currentTimeMillis, "If everything is coming your way, watch out.") ::
      Tweet(Author("shocker"), System.currentTimeMillis, "Everything that can work, will work.") ::
      Tweet(Author("akka.team"), System.currentTimeMillis, "The shortest distance between two points is usually under construction.") ::
      Tweet(Author("renato.cavalcanti"), System.currentTimeMillis, "Leftover nuts never match leftover bolts.") ::
      Tweet(Author("eduardo.pinto"), System.currentTimeMillis, "As soon as you switch lanes, your old lane speeds up.") ::
      Nil)

  val transformer = Flow[Tweet].map(x => {
    val name = x.author.handle.split("\\.")
    val first = name(0)
    val last = name(1)
    Person(first, last)
  })

  val sink = Sink.foreach[Person](println)

  val decider: Supervision.Decider = {
    case _: ArrayIndexOutOfBoundsException => Supervision.Resume
    case _ => Supervision.Stop
  }

  val future = input
    .log("").withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel, onFinish = Logging.InfoLevel, onFailure = Logging.DebugLevel))
    .via(transformer).toMat(sink)(Keep.right)
    .withAttributes(ActorAttributes.supervisionStrategy(decider))
    .run()

  implicit val ec: ExecutionContextExecutor = system.dispatcher
  future.onComplete(_ => system.terminate())
}
