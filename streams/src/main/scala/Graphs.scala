package org.example.akka.streams

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Supervision}

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

object Graphs extends App {
  implicit val system: ActorSystem = ActorSystem("g")
  implicit val ec: ExecutionContextExecutor = system.dispatcher

  final case class Author(handle: String)
  final case class Person(first: String, last: String) {
    override def toString: String = s"$first $last"
  }

  val input = Source(
    Author("roland.kuhn") ::
      Author("patrik.norwall") ::
      Author("andrzej.ludwikowski") ::
      Author("johan.andrén") ::
      Author("enno.runne") ::
      Author("peter.vlugter") ::
      Author("faultyname") ::
      Author("renato.cavalcanti") ::
      Author("eduardo.pinto") ::
      Nil)

  val transformer = Flow[Author].map(transform)

  def transform(author: Author) = {
    val name = author.handle.split("\\.")
    Person(name(0), name(1))
  }

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

  future.onComplete(_ => system.terminate())
}
