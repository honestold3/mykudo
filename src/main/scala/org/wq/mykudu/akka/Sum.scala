package org.wq.mykudu.akka

import akka.actor.Actor
import akka.actor.Props
import akka.actor.ActorSystem
import akka.routing.RoundRobinPool

// 定义一个case类
sealed trait SumTrait
case class Result(value: Int) extends SumTrait

case class Message(v1: Int,v2 :Int) extends SumTrait

// 计算用的Actor
class SumActor extends Actor {
  val RANGE = 10000

  def calculate(start: Int, end: Int, flag : Int): Int = {
    var cal = 0

    for (i <- (start to end)) {
      for (j <- 1 to 3000000) {}
      cal += i
    }

    println("flag : " + flag + ".")
    return cal
  }

  def receive = {
    case value: Message =>
      sender ! Result(calculate((RANGE / value.v2) * (value.v1 - 1) + 1, (RANGE / value.v2) * value.v1, value.v1))
    case _ => println("未知 in SumActor...")
  }
}

// 打印结果用的Actor
class PrintActor extends Actor {
  def receive = {
    case (sum: Int, startTime: Long) =>
      println("总数为：" + sum + "；所花时间为："
        + (System.nanoTime() - startTime)/1000000000.0 + "秒。")
    case _ => println("未知 in PrintActor...")
  }
}

// 主actor，发送计算指令给SumActor，发送打印指令给PrintActor
class MasterActor extends Actor {
  var sum = 0
  var count = 0
  var startTime: Long = 0
  var ncpu = 4

  // 声明Actor实例，nrOfInstances是pool里所启routee（SumActor）的数量，
  // 这里用4个SumActor来同时计算，很Powerful。
  val sumActor   = context.actorOf(Props[SumActor]
    .withRouter(RoundRobinPool(nrOfInstances = ncpu)), name = "sumActor")
  val printActor = context.actorOf(Props[PrintActor], name = "printActor")

  def receive = {
    case "calculate..." =>
      startTime = System.nanoTime()
      for (i <- 1 to ncpu) sumActor ! Message(i,ncpu)
    case Result(value) =>
      sum += value
      count += 1
      if (count == ncpu) {
        printActor ! (sum, startTime)
        context.stop(self)
      }
    case _ => println("未知 in MasterActor...")
  }
}

object Sum {
  def main(args: Array[String]): Unit = {
    var sum = 0

    val system = ActorSystem("MasterActorSystem")
    val masterActor = system.actorOf(Props[MasterActor], name = "masterActor")
    masterActor ! "calculate..."

    Thread.sleep(5000)
    system.shutdown()
  }
}