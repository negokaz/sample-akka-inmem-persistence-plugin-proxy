package com.example

import akka.actor.typed.ActorSystem
import akka.cluster.sharding.typed.scaladsl.EntityRef
import akka.cluster.sharding.typed.scaladsl.{ ClusterSharding, Entity }
import akka.persistence.typed.PersistenceId
import akka.util.Timeout
import com.example.BankAccount._

import scala.concurrent.Future
import scala.concurrent.duration._

class BankAccountService(system: ActorSystem[_]) {

  private[this] val sharding = ClusterSharding(system)

  sharding.init(Entity(typeKey = BankAccount.TypeKey) { entityContext =>
    BankAccount(
      AccountNo(entityContext.entityId),
      PersistenceId(entityContext.entityTypeKey.name, entityContext.entityId),
    )
  })

  private[this] def entityRef(accountNo: AccountNo): EntityRef[Command] = {
    sharding.entityRefFor(BankAccount.TypeKey, accountNo.underlying)
  }

  private[this] implicit val askTimeout: Timeout = Timeout(10.seconds)

  def deposit(accountNo: AccountNo, amount: Int): Future[DepositResponse] = {
    entityRef(accountNo) ? Deposit(amount)
  }

  def withdraw(accountNo: AccountNo, amount: Int): Future[WithdrawResponse] = {
    entityRef(accountNo) ? Withdraw(amount)
  }

  def getBalance(accountNo: AccountNo): Future[GetBalanceResponse] = {
    entityRef(accountNo) ? GetBalance()
  }
}
