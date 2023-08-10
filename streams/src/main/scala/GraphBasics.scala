package org.example.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.event.Logging
import akka.stream.{Attributes, ClosedShape, Supervision}
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}

import java.time.{Instant, LocalDateTime, ZoneId}
import scala.concurrent.ExecutionContextExecutor
import scala.language.postfixOps

object GraphBasics extends App {
  final case class Author(handle: String)
  final case class Person(firstLast: Array[String]) {
    override def toString: String = s"${firstLast(0)} ${firstLast(1)}"
  }

  final case class Hashtag(name: String)

  final case class Tweet(author: Author, timestamp: Long, body: String) {
    def hashtags: Set[Hashtag] =
      body
        .split(" ")
        .collect {
          case t if t.startsWith("#") => Hashtag(t.replaceAll("[^#\\w]", ""))
        }
        .toSet
  }

  implicit val system: ActorSystem = ActorSystem("GraphBasics")

  val akkaTag = Hashtag("#akka")


  val input: Source[Tweet, NotUsed] = Source(
    Tweet(Author("roland.kuhn"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("patrik.nw"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("b.antonsson"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("drew.hk"), System.currentTimeMillis, "#akka !") ::
      Tweet(Author("k.tosopl"), System.currentTimeMillis, "#akka on the rocks!") ::
      Tweet(Author("m.martynas"), System.currentTimeMillis, "wow #akka!") ::
            Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Tweet(Author("akka.team"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("banana.man"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("apple.man"), System.currentTimeMillis, "#apples rock!") ::
      Nil)

  val transformer = Flow[Tweet].map(x => Person(x.author.handle.split("\\.")))
  val dater = Flow[Tweet].map(x => Instant.ofEpochMilli(x.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime)
  val output = Sink.foreach[(LocalDateTime, Person)](t => println(s"${t._1}: ${t._2}"))

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Tweet](2)) // fan-out operator
      val zip = builder.add(Zip[LocalDateTime, Person]) // fan-in operator

      input ~> broadcast

      broadcast.out(0) ~> dater  ~> zip.in0
      broadcast.out(1) ~> transformer ~> zip.in1

      zip.out ~> output

      ClosedShape // FREEZE the builder's shape
      // shape
    } // graph
  ) // runnable graph


  val decider: Supervision.Decider = {
    case _: ArithmeticException => Supervision.Resume
    case _ => Supervision.Stop
  }

  val done = input
    .map(x => Person(x.author.handle.split("\\.")))
    .log("").withAttributes(Attributes.logLevels(onElement = Logging.WarningLevel, onFinish = Logging.InfoLevel, onFailure = Logging.DebugLevel))
    .run()

  implicit val ec: ExecutionContextExecutor = system.dispatcher
  done.onComplete(_ => system.terminate())



//    graph.run() // run the graph and materialize it

//  //Exercise: feed a source into 2 sinks at the same time (hint: use a broadcast)
//  val firstSink = Sink.foreach[LocalDateTime](x => println(s"First sink: $x"))
//  val secondSink = Sink.foreach[Person](x => println(s"Second sink: $x"))
//
//  // step 1
//  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//
//      val broadcast = builder.add(Broadcast[Tweet](2))
//
//      input ~>  broadcast ~> dater ~> firstSink
//                broadcast ~> secondSink
//      //same as:
//      //      input ~>  broadcast
//      //      broadcast.out(0) ~> dater ~> firstSink
//      //      broadcast.out(1) ~> secondSink
//
//      ClosedShape
//    }
//  )
//
//  sourceToTwoSinksGraph.run()
}
