package com.example

import akka.actor.typed.{ ActorRef, Behavior }
import akka.persistence.typed.scaladsl.{ Effect, EventSourcedBehavior }
import akka.persistence.typed.PersistenceId
import akka.persistence.typed.scaladsl.EventSourcedBehavior.CommandHandler
import akka.persistence.typed.scaladsl.EventSourcedBehavior.EventHandler
import akka.cluster.sharding.typed.scaladsl.EntityTypeKey

object BankAccount {

  sealed trait Command
  final case class Deposit(amount: Int)(val replyTo: ActorRef[DepositResponse])   extends Command
  final case class Withdraw(amount: Int)(val replyTo: ActorRef[WithdrawResponse]) extends Command
  final case class GetBalance()(val replyTo: ActorRef[GetBalanceResponse])        extends Command

  sealed trait Response
  sealed trait WithdrawResponse                               extends Response
  sealed trait DepositResponse                                extends Response
  final case object ShortBalance                              extends WithdrawResponse
  final case class BalanceChanged(balance: Int, cause: Event) extends WithdrawResponse with DepositResponse
  sealed trait GetBalanceResponse                             extends Response
  final case class AccountBalance(balance: Int)               extends GetBalanceResponse

  sealed trait Event
  final case class Deposited(amount: Int)  extends Event
  final case class Withdrawed(amount: Int) extends Event

  final case class Account(accountNo: AccountNo, balance: Int) {
    def deposit(amount: Int): Account  = copy(balance = balance + amount)
    def withdraw(amount: Int): Account = copy(balance = balance - amount)
  }

  val TypeKey: EntityTypeKey[Command] =
    EntityTypeKey[Command]("BankAccount")

  def apply(accountNo: AccountNo, persistenceId: PersistenceId): Behavior[Command] =
    EventSourcedBehavior[Command, Event, Account](
      persistenceId = persistenceId,
      emptyState = Account(accountNo, balance = 0),
      commandHandler,
      eventHandler,
    ).snapshotWhen { (_, event, _) =>
      event match {
        case _: Deposited  => true
        case _: Withdrawed => true
      }
    }

  private[this] val commandHandler: CommandHandler[Command, Event, Account] = { (state, command) =>
    command match {
      case deposit: Deposit =>
        val event = Deposited(deposit.amount)
        Effect
          .persist(event)
          .thenReply(deposit.replyTo)(newState => BalanceChanged(newState.balance, cause = event))
      case withdraw: Withdraw if withdraw.amount > state.balance =>
        Effect.reply(withdraw.replyTo)(ShortBalance)
      case withdraw: Withdraw =>
        val event = Withdrawed(withdraw.amount)
        Effect
          .persist(event)
          .thenReply(withdraw.replyTo)(newState => BalanceChanged(newState.balance, cause = event))
      case getBalance: GetBalance =>
        Effect.reply(getBalance.replyTo)(AccountBalance(state.balance))
    }
  }

  private[this] val eventHandler: EventHandler[Account, Event] = { (state, event) =>
    event match {
      case Withdrawed(amount) => state.withdraw(amount)
      case Deposited(amount)  => state.deposit(amount)
    }
  }
}
