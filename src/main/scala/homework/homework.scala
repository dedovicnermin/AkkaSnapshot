package homework
import akka.actor.{Actor, ActorSystem, Props, ActorRef}

case class Start(left : ActorRef, right : ActorRef)
case object BEGIN
case object TOKEN_X
case object TOKEN_Y
case object SNAPSHOT
case object MARKER
case object PRINT_AND_CLEAR

class TokenRing extends Actor {
  var tokenXCount = 0
  var tokenYCount = 0
  var leftNeighbor : ActorRef = _
  var rightNeighbor : ActorRef = _
  var snapshotHelper : RecordingState = _

  var isRecording = false

  def receive: Receive = {
    case TOKEN_X =>
      if (snapshotHelper.isRecording) snapshotHelper.handleTokenEvent(sender().path.toString, TOKEN_X)
      tokenXCount = tokenXCount + 1
      println(s"$self received TOKEN_X from ${sender()} XCount: $tokenXCount YCount: $tokenYCount")
      Thread.sleep(500)
      leftNeighbor ! TOKEN_X
    case TOKEN_Y =>
      if (snapshotHelper.isRecording) snapshotHelper.handleTokenEvent(sender().path.toString, TOKEN_Y)
      tokenYCount = tokenYCount + 1
      println(s"$self received TOKEN_Y from ${sender()} XCount: $tokenXCount YCount: $tokenYCount")
      Thread.sleep(500)
      rightNeighbor ! TOKEN_Y
    case SNAPSHOT =>
      // record state, send out marker to outbound channels, record inbound
      snapshotHelper.initSnapshot(tokenXCount, tokenYCount)
      leftNeighbor ! MARKER
      rightNeighbor ! MARKER
    case MARKER =>
      if (!snapshotHelper.isRecording) {
        snapshotHelper.initSnapshot(tokenXCount, tokenYCount, sender().path.toString)
        leftNeighbor ! MARKER
        rightNeighbor ! MARKER
      } else {
        snapshotHelper.handleMarkerEvent(sender().path.toString)
      }
    case Start(left, right) =>
      leftNeighbor = left
      rightNeighbor = right
      snapshotHelper = new RecordingState(self.path.toString, leftNeighbor.path.toString, rightNeighbor.path.toString)
    case BEGIN =>
      leftNeighbor ! TOKEN_X
      rightNeighbor ! TOKEN_Y
  }
}

object Server extends App {
  val system = ActorSystem("TokenRing")
  val first = system.actorOf(Props[TokenRing](), name = "first")
  val second = system.actorOf(Props[TokenRing](), name = "second")
  val third = system.actorOf(Props[TokenRing](), name = "third")

  println(first.path)
  println(second.path)
  println(third.path)

  first ! Start(third, second)
  second ! Start(first, third)
  third ! Start(second, first)

  first ! BEGIN

  Thread.sleep(5000)
  first ! SNAPSHOT
  Thread.sleep(10000)
  second ! SNAPSHOT
  Thread.sleep(5000)
  third ! SNAPSHOT
}
