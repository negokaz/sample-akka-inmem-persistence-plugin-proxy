package com.example

import akka.actor.typed.ActorSystem
import akka.actor.typed.scaladsl.Behaviors

import scala.concurrent.Await
import scala.concurrent.duration._

object Main extends App {

  val system = ActorSystem(Behaviors.empty, "ExampleSystem")

  val service = new BankAccountService(system)

  val accountNo = AccountNo("00002222")

  println(Await.result(service.deposit(accountNo, 100), 10.seconds))
  println(Await.result(service.withdraw(accountNo, 100), 10.seconds))
  println(Await.result(service.withdraw(accountNo, 100), 10.seconds))
  println(Await.result(service.getBalance(accountNo), 10.seconds))
}
