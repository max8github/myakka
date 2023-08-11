package org.example.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.{ActorAttributes, Attributes, Supervision}

import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps
import scala.util.{Failure, Random, Success}

object Graphs extends App {
  given system: ActorSystem = ActorSystem("g")

  given ec: ExecutionContextExecutor = system.dispatcher

  private val input = Stream.badSource()
  private val transformer = Flow[Author].map(transform)

  private def transform(author: Author): Person = {
    val name = author.handle.split("\\.")
    if (name.length < 2) println("Ouch!")
    Person(capitalizeFirst(name(0)), capitalizeFirst(name(1)))
  }

  private val sink = Sink.foreach[Person](println)
  private val future = input.via(transformer).runWith(sink)

  future.onComplete {
    case Success(_) =>
      println("The future succeeded")
      system.terminate()

    case Failure(ex) =>
      println(s"The future failed with $ex")
      system.terminate()
  }
}

private object Stream {
  def badSource(): Source[Author, NotUsed] =
    Source((Gen.nextHandlerStringSeq() ++ Seq(Gen.nextToken()) ++ Gen.nextHandlerStringSeq()).map(Author.apply))

  def goodSource(): Source[Author, NotUsed] =
    Source(Gen.nextHandlerStringSeq().map(Author.apply))
}

private object Gen {
  def nextHandlerStringSeq(): Seq[String] =
    (1 to 1 + Rnd.nextInt(64)).map { _ => nextToken() + "." + nextToken() }

  def nextToken(): String = {
    val length = MinTokenLength + Rnd.nextInt(MaxTokenLength - MinTokenLength + 1)
    (" " * length).map { _ => nextLetter() }
  }

  private def nextLetter(): Char =
    Alphabet(Rnd.nextInt(Alphabet.length))

  private val Alphabet = "abcdefghijklmnopqrstuvwxyz"
  private val Rnd = Random()
  private val MinTokenLength = 2
  private val MaxTokenLength = 16
}

private final case class Author(handle: String)

private final case class Person(first: String, last: String) {
  override def toString: String = s"$first $last"
}

private def capitalizeFirst(str: String): String =
  str.head.toUpper.toString + str.tail
