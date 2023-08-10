package org.example.akka.streams

import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Keep, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Supervision}

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

object Graphs extends App {
  given system: ActorSystem = ActorSystem("g")
  given ec: ExecutionContextExecutor = system.dispatcher

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

  val sinkAuthor = Sink.foreach[Author](a => println(s"FAULTY! $a"))

  val transformer = Flow[Author].map(transform)
    .map {
      case left@Left(_) => left
      case right@Right(_) => right
    }.divertTo(sinkAuthor.contramap(_.swap.getOrElse(Author("empty"))), _.isLeft) //Divert faulty elements
    .map(e => e.getOrElse(Person("", "")))

  def transform(author: Author) = {
    try {
      val name = author.handle.split("\\.")
      Right(Person(name(0), name(1)))
    } catch {
      case _: Throwable => Left(author)
    }
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
