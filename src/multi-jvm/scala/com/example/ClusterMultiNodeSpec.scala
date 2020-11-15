package com.example

import akka.Done
import akka.actor.typed.ActorSystem
import akka.remote.testkit.{ MultiNodeSpec, MultiNodeSpecCallbacks }

import scala.language.implicitConversions
import org.scalatest.BeforeAndAfterAll
import org.scalatest.matchers.should.Matchers
import org.scalatest.wordspec.AnyWordSpecLike
import akka.actor.typed.scaladsl.adapter._
import akka.cluster.ClusterEvent.{ CurrentClusterState, MemberUp }
import akka.cluster.Cluster
import akka.testkit.TestProbe
import com.example.ClusterMultiNodeSpecSampleConfig.node1

trait ClusterMultiNodeSpec extends MultiNodeSpecCallbacks with AnyWordSpecLike with Matchers with BeforeAndAfterAll {
  self: MultiNodeSpec =>

  protected def typedSystem: ActorSystem[Nothing] = system.toTyped

  override def initialParticipants: Int = roles.size

  override def beforeAll(): Unit = multiNodeSpecBeforeAll()

  override def afterAll(): Unit = multiNodeSpecAfterAll()

  // Might not be needed anymore if we find a nice way to tag all logging from a node
  override implicit def convertToWordSpecStringWrapper(s: String): WordSpecStringWrapper =
    new WordSpecStringWrapper(s"$s (on node '${self.myself.name}', $getClass)")

  protected val cluster: Cluster = Cluster(system)

  private[this] val clusterEventSubscriber: TestProbe = {
    val probe = TestProbe()
    cluster.subscribe(probe.ref, classOf[MemberUp])
    probe.ignoreMsg { case _: CurrentClusterState => true }
    probe
  }

  protected def joinCluster(): Unit = {
    cluster.join(node(node1).address)
    clusterEventSubscriber.fishForSpecificMessage() {
      case MemberUp(member) if member.address == myAddress => Done
    }
  }
}
