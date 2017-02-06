package actors

import akka.actor.{Actor, ActorLogging, Props}
import akka.cluster.Cluster
import akka.cluster.client.ClusterClientReceptionist
import akka.cluster.pubsub.DistributedPubSub
import akka.event.LoggingReceive
import akka.persistence.PersistentActor

import scala.concurrent.duration.FiniteDuration
import scala.collection.immutable.Queue
import messages._


class VisionManagerActor(workTimeout: FiniteDuration) extends PersistentActor with ActorLogging {

  val mediator = DistributedPubSub(context.system).mediator
  ClusterClientReceptionist(context.system).registerService(self)

  override def persistenceId: String = Cluster(context.system).selfRoles.find(_.startsWith("backend-")) match {
    case Some(role) => role + "-master"
    case _ => "master"
  }

  // not state backed
  private var workers = Map[String, WorkState]

  // state backed
  private var workState = VisionManagerActor.empty

  val cleanupTask = context.system.scheduler.schedule(workTimeout/2, workTimeout/2, self, CleanupTick)

  override def postStop(): Unit = cleanupTask.cancel()

  /**
    * Akka persistence Eventsourced requires the receiveRecover method
    */
  override def receiveRecover: Receive = LoggingReceive({
    case event: WorkStatus =>
      workState = workState.updated()
  })


  /**
    * Message handler
    */
  override def receive: Receive = LoggingReceive({
    case WorkAccepted => //TODO: do something

  })


}

object VisionManagerActor {
  def props(workTimeout: FiniteDuration): Props = {
    Props(classOf[VisionManagerActor], workTimeout)
  }

  def empty: WorkManagement = WorkManagement(
    pendingWork = Queue.empty,
    workInProgress = Map.empty,
    acceptedWorkIds = Set.empty,
    doneWorkIds = Set.empty)

  def updated() = {

  }
}

case class WorkManagement private (
  private val pendingWork: Queue[Work],
  private val workInProgress: Map[String, Work],
  private val acceptedWorkIds: Set[String],
  private val doneWorkIds: Set[String]) {


}
