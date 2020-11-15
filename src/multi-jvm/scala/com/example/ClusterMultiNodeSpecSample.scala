package com.example

import akka.actor.typed.receptionist.{ Receptionist, ServiceKey }
import akka.actor.typed.{ ActorRef, Behavior }
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.adapter._
import akka.remote.testconductor.RoleName
import akka.remote.testkit.{ MultiNodeConfig, MultiNodeSpec }
import com.example.ClusterMultiNodeSpecSample.Ponger.Ping
import com.typesafe.config.ConfigFactory

import scala.concurrent.duration._

object ClusterMultiNodeSpecSampleConfig extends MultiNodeConfig {

  commonConfig(ConfigFactory.load())

  val node1: RoleName = role("node1")
  val node2: RoleName = role("node2")
}

object ClusterMultiNodeSpecSample {
  object Ponger {
    val PingServiceKey: ServiceKey[Ping] = ServiceKey[Ping]("PingService")

    final case class Pong()
    final case class Ping(replyTo: ActorRef[Pong])

    def apply(): Behavior[Ping] = Behaviors.setup { context =>
      context.system.receptionist ! Receptionist.Register(PingServiceKey, context.self)

      Behaviors.receiveMessage { case Ping(replyTo) =>
        replyTo ! Pong()
        Behaviors.same
      }
    }
  }
}

class ClusterMultiNodeSpecSampleMultiJvm1 extends ClusterMultiNodeSpecSample
class ClusterMultiNodeSpecSampleMultiJvm2 extends ClusterMultiNodeSpecSample

class ClusterMultiNodeSpecSample extends MultiNodeSpec(ClusterMultiNodeSpecSampleConfig) with ClusterMultiNodeSpec {
  import ClusterMultiNodeSpecSample._
  import ClusterMultiNodeSpecSampleConfig._

  "A ClusterMultiNodeSpecSample" must {

    "wait for all nodes to enter a barrier" in {
      joinCluster()
      enterBarrier("startup")
    }

    "send to and receive from a remote node" in {

      runOn(node2) {
        typedSystem.systemActorOf(Ponger(), "ponger")
      }
      enterBarrier("deployed")

      runOn(node1) {
        typedSystem.receptionist ! Receptionist.Subscribe(Ponger.PingServiceKey, testActor)
        fishForSpecificMessage() {
          case Ponger.PingServiceKey.Listing(listing) if listing.nonEmpty =>
            listing.foreach(_ ! Ping(testActor))
        }
        expectMsg(10.seconds, Ponger.Pong())
      }

      enterBarrier("finished")
    }
  }
}
