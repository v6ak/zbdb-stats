package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval

abstract sealed class WalkEvent

object WalkEvent {
  abstract sealed class Process extends WalkEvent {
    def duration: TimeInterval

    def overtakes: Overtakes
  }

  abstract sealed class TimePoint extends WalkEvent {
    def time: Moment

    def togetherWith: Seq[Participant]

    def checkpoint: Checkpoint

    def isStart: Boolean

    def isFinish: Boolean
  }

  abstract sealed class UnknownTimePoint extends WalkEvent {}


  case class Departure(
    override val time: Moment,
    override val togetherWith: Seq[Participant],
    checkpoint: Checkpoint,
    isStart: Boolean,
  ) extends TimePoint {
    override def isFinish: Boolean = false
  }

  case class Arrival(
    override val time: Moment,
    override val togetherWith: Seq[Participant],
    checkpoint: Checkpoint,
    isFinish: Boolean,
  ) extends TimePoint {
    override def isStart: Boolean = false
  }

  case class Walk(
    override val duration: TimeInterval,
    override val overtakes: Overtakes,
    len: BigDecimal,
  ) extends Process

  case class WaitingOnCheckpoint(
    checkpoint: Checkpoint,
    override val duration: TimeInterval,
    override val overtakes: Overtakes,
  ) extends Process

  abstract sealed class GaveUp extends UnknownTimePoint {}

  object GaveUp {
    case class AtCheckpoint(checkpoint: Checkpoint) extends GaveUp

    case class DuringWalk(nextPos: Int) extends GaveUp
  }
}
