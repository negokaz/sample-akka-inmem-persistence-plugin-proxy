package com.example

import akka.persistence.journal.PersistencePluginProxy
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import com.example.BankAccount.{ AccountBalance, BalanceChanged }
import com.typesafe.config.ConfigFactory
import org.scalatest.Inside
import org.scalatest.prop.TableDrivenPropertyChecks._

object BankAccountServiceSpecConfig extends MultiNodeConfig {

  commonConfig(ConfigFactory.load())

  val node1: RoleName = role("node1")
  val node2: RoleName = role("node2")
  val node3: RoleName = role("node3")
  val node4: RoleName = role("node4")
}

class BankAccountServiceSpecMultiJvm1 extends BankAccountServiceSpec
class BankAccountServiceSpecMultiJvm2 extends BankAccountServiceSpec
class BankAccountServiceSpecMultiJvm3 extends BankAccountServiceSpec
class BankAccountServiceSpecMultiJvm4 extends BankAccountServiceSpec

class BankAccountServiceSpec extends MultiNodeSpec(BankAccountServiceSpecConfig) with ClusterMultiNodeSpec with Inside {
  import BankAccountServiceSpecConfig._

  // node1 (first node) never shutdown while testing because it has the controller of multi-node-testkit.
  // For more details see: https://doc.akka.io/docs/akka/2.6/multi-node-testing.html#things-to-keep-in-mind
  PersistencePluginProxy.setTargetLocation(system, node(node1).address)

  "BankAccountService" should {

    "form cluster" in {
      runOn(node1, node2, node3) {
        joinCluster()
      }
      enterBarrier("cluster formed")
    }

    val bankAccountService = new BankAccountService(typedSystem)

    "return the latest balance even if getBalance is called in another node" in {
      val accountNos = Table("accountNo", AccountNo("00001"), AccountNo("00002"), AccountNo("00003"))
      forAll(accountNos) { accountNo =>
        runOn(node1) {
          val depositResponse = bankAccountService.deposit(accountNo, 100).await
          inside(depositResponse) { case changed: BalanceChanged =>
            changed.balance should be(100)
          }
        }
        enterBarrier("deposited")
        runOn(node2) {
          val getBalanceResponse = bankAccountService.getBalance(accountNo).await
          inside(getBalanceResponse) { case balance: AccountBalance =>
            balance.balance should be(100)
          }
        }
      }
      enterBarrier("done")
    }

    "return the latest balance even if a cluster node is replaced" in {
      val accountNos = Table("accountNo", AccountNo("00011"), AccountNo("00012"), AccountNo("00013"))
      runOn(node1) {
        forAll(accountNos) { accountNo =>
          val depositResponse = bankAccountService.deposit(accountNo, 100).await
          inside(depositResponse) { case changed: BalanceChanged =>
            changed.balance should be(100)
          }
        }
      }
      enterBarrier("deposited")
      runOn(node1) {
        testConductor.shutdown(node3).await
      }
      runOn(node1, node2, node4) {
        enterBarrier("node3 shutdown")
      }
      runOn(node4) {
        joinCluster()
      }
      runOn(node1, node2, node4) {
        enterBarrier("node3 was replaced to node4")
      }
      runOn(node4) {
        forAll(accountNos) { accountNo =>
          val getBalanceResponse = bankAccountService.getBalance(accountNo).await
          inside(getBalanceResponse) { case balance: AccountBalance =>
            balance.balance should be(100)
          }
        }
      }
      runOn(node1, node2, node4) {
        enterBarrier("done")
      }
    }
  }
}
