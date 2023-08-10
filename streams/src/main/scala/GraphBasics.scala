package org.example.akka.streams

import akka.NotUsed
import akka.actor.ActorSystem
import akka.stream.ClosedShape
import akka.stream.scaladsl.{Balance, Broadcast, Flow, GraphDSL, Merge, RunnableGraph, Sink, Source, Zip}
import com.sun.org.apache.xalan.internal.lib.ExsltDatetime.dateTime

import java.time.{Instant, LocalDateTime, ZoneId}
import scala.language.postfixOps

object GraphBasics extends App {
  final case class Author(handle: String)

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
      Tweet(Author("m.martynas"), System.currentTimeMillis, "wow #akka !") ::
      Tweet(Author("akka.team"), System.currentTimeMillis, "#akka rocks!") ::
      Tweet(Author("banana.man"), System.currentTimeMillis, "#bananas rock!") ::
      Tweet(Author("apple.man"), System.currentTimeMillis, "#apples rock!") ::
      Tweet(Author("drama"), System.currentTimeMillis, "we compared #apples to #oranges!") ::
      Nil)

  val transformer = Flow[Tweet].map(x => Tweet(Author(x.author.handle.replaceAll("\\.", " ")), x.timestamp, x.body))
  val dater = Flow[Tweet].map(x => Instant.ofEpochMilli(x.timestamp).atZone(ZoneId.systemDefault()).toLocalDateTime)
  val output = Sink.foreach[(Tweet, LocalDateTime)](println)

  // step 1 - setting up the fundamentals for the graph
  val graph = RunnableGraph.fromGraph(
    GraphDSL.create() { implicit builder: GraphDSL.Builder[NotUsed] => // builder = MUTABLE data structure
      import GraphDSL.Implicits._ // brings some nice operators into scope

      // step 2 - add the necessary components of this graph
      val broadcast = builder.add(Broadcast[Tweet](2)) // fan-out operator
      val zip = builder.add(Zip[Tweet, LocalDateTime]) // fan-in operator

      input ~> broadcast

      broadcast.out(0) ~> transformer ~> zip.in0
      broadcast.out(1) ~> dater  ~> zip.in1

      zip.out ~> output

      ClosedShape // FREEZE the builder's shape
      // shape
    } // graph
  ) // runnable graph

    graph.run() // run the graph and materialize it

//  /**
//   * exercise 1: feed a source into 2 sinks at the same time (hint: use a broadcast)
//   */
//
//  val firstSink = Sink.foreach[Tweet](x => println(s"First sink: $x"))
//  val secondSink = Sink.foreach[LocalDateTime](x => println(s"Second sink: $x"))
//
//  // step 1
//  val sourceToTwoSinksGraph = RunnableGraph.fromGraph(
//    GraphDSL.create() { implicit builder =>
//      import GraphDSL.Implicits._
//
//      // step 2 - declaring the components
//      val broadcast = builder.add(Broadcast[Tweet](2))
//
//      // step 3 - tying up the components
//      input ~>  broadcast ~> firstSink
//      broadcast ~> secondSink
//      //      broadcast.out(0) ~> firstSink
//      //      broadcast.out(1) ~> secondSink
//
//      // step 4
//      ClosedShape
//    }
//  )


}
