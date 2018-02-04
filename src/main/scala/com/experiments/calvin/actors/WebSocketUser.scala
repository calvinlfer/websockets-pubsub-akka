package com.experiments.calvin.actors

import akka.actor.{Actor, ActorLogging, ActorRef, Props}
import akka.cluster.pubsub.DistributedPubSub
import akka.cluster.pubsub.DistributedPubSubMediator._
import com.experiments.calvin.actors.WebSocketUser.{Calculate, Calculated}
import WebSocketUser._
import akka.event.LoggingReceive

import scala.util.Try

/**
  * Represents a WebSocket user. One of these actors is created for every user that connects to the server
  * over a WebSocket
  *
  * @param username is the name of the user
  */
class WebSocketUser(username: String) extends Actor with ActorLogging {
  private val topic                      = "calculated-events"
  private val mediator                   = DistributedPubSub(context.system).mediator
  private var wsHandle: Option[ActorRef] = None

  private def messageWsHandle(e: Event): Unit =
    wsHandle.fold(())(actor => actor ! e)

  override def preStart(): Unit = {
    mediator ! Subscribe(topic, self)
  }

  override def receive: Receive = LoggingReceive {
    case SubscribeAck(Subscribe(`topic`, None, `self`)) =>
      log.info("Subscribed {} to {}", username, `topic`)

    case UnsubscribeAck(Unsubscribe(`topic`, None, `self`)) =>
      log.info("Un-subscribed {} from {}", username, `topic`)
      context.stop(self)

    case c: Command =>
      c match {
        // `actorRef` is a handle to communicate back to the WebSocket user
        case ConnectWsHandle(actorRef) =>
          wsHandle = Some(actorRef)

        case WsHandleDropped =>
          log.warning("Downstream WebSocket has been disconnected, stopping {}", username)
          mediator ! Unsubscribe(topic, self)

        case Calculate(a, b, operator) =>
          val answer = parse(a, operator, b)
          val result =
            answer.fold(error => Calculated(s"Error processing request: ${error.getMessage}", None, username),
                        res => Calculated(s"$a $operator $b", Some(res), username))

          mediator ! Publish(topic, result)
          messageWsHandle(result)
      }

    case e: Event =>
      e match {
        case c: Calculated =>
          if (c.username != username) messageWsHandle(c)
          else ()
      }
  }
}

object WebSocketUser {
  sealed trait Command
  case class Calculate(operandA: Int, operandB: Int, operator: String) extends Command
  case class ConnectWsHandle(actorRef: ActorRef)                       extends Command
  case object WsHandleDropped                                          extends Command

  sealed trait Event
  case class Calculated(description: String, answer: Option[Int], username: String) extends Event

  def parse(leftOp: Int, operator: String, rightOp: Int): Try[Int] = Try {
    operator match {
      case "+" => leftOp + rightOp
      case "-" => leftOp - rightOp
      case "/" => leftOp / rightOp
      case "*" => leftOp * rightOp
      case _   => sys.error("Invalid operator")
    }
  }

  def props(username: String): Props = Props(new WebSocketUser(username))
}
