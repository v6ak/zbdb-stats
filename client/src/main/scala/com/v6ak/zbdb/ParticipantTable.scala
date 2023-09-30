package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval
import com.v6ak.scalajs.time.TimeInterval.TimeIntervalOrdering
import scala.language.implicitConversions

final case class FullPartInfo(
  previousPartMetaOption: Option[Part],
  partMeta: Part,
  part: PartTimeInfo,
  nextPartOption: Option[PartTimeInfo],
)

object ParticipantTable {
}

final case class ParticipantTable (startTime: Moment, parts: Seq[Part], data: Seq[Participant], formatVersion: FormatVersion) {

  val (participantMap, bestTotalTimeOption) = {
    def processOrderGroup(order: Int, members: Seq[Participant]): Seq[ParticipantInContext.Successful] = members.map(participant => ParticipantInContext.Successful(participant, order))
    val (finishedParticipants, unfinishedParticipants) = data.partition(hasFinished)
    val finishedParticipantsGroups = finishedParticipants.groupBy(_.totalTime).toIndexedSeq.sortBy(_._1) match {
      case Seq() => Seq()
      case orderGroups =>
        orderGroups.map(_._2).tail.scanLeft(processOrderGroup(1, orderGroups.head._2))((previousGroup, currentGroup) => processOrderGroup(previousGroup(0).order + previousGroup.size, currentGroup))
    }
    val finishedParticipantsInContext = finishedParticipantsGroups.flatten
    val bestTotalTimeOption = finishedParticipantsInContext.headOption.map(_.totalTime)
    val unfinishedParticipantsInContext = unfinishedParticipants.map(ParticipantInContext.Failed)
    val allParticipantsInContext = finishedParticipantsInContext ++ unfinishedParticipantsInContext
    (allParticipantsInContext.map(pic => pic.participant -> pic).toMap, bestTotalTimeOption)
  }

  implicit def toParticipantInContext(participant: Participant): ParticipantInContext = participantMap(participant)

  def previousPartCummulativeLengths = Seq(BigDecimal(0)) ++ parts.map(_.cumulativeTrackLength)

  private def hasFinished(row: Participant) = (row.partTimes.size == parts.size) && row.partTimes.forall(_.isInstanceOf[PartTimeInfo.Finished])

  private def expandSeq[T](s: Seq[T], empty: T, size: Int) = s ++ Seq.fill(size - s.size)(empty)

  private def longZip[T, U](a: Seq[T], emptyA: T)(b: Seq[U], emptyB: U) = {
    val maxSize = a.size max b.size
    val ea = expandSeq(a, emptyA, maxSize)
    val eb = expandSeq(b, emptyB, maxSize)
    ea lazyZip eb
  }

  val firsts = data.foldLeft(Seq.empty[BestParticipantData]){ (fastestSoFar, participant) =>
    longZip[BestParticipantData, Option[PartTimeInfo]](
      fastestSoFar, BestParticipantData.Empty
    )(
      participant.partTimes.map(Some(_)), None
    ).map{(fastestParticipantSoFar, current) => fastestParticipantSoFar.merge(current)}
  }

  def partData(row: Participant, pos: Int) = row.partTimes.lift(pos)
  def pauseData(row: Participant, pos: Int) = row.pauses.lift(pos)

  def finishedPartData(row: Participant, pos: Int): Option[PartTimeInfo.Finished] = partData(row, pos).collect {
    case x: PartTimeInfo.Finished => x
  }

  def filterOthers(pos: Int, current: Participant)(f: (PartTimeInfo, PartTimeInfo) => Boolean) = {
    val currentPartInfo = current.partTimes(pos)
    data.filter { p =>
      (p != current) &&
        partData(p, pos).exists(f(currentPartInfo, _))
    }
  }

  def filterOthersPauses(pos: Int, current: Participant)(f: (Pause, Pause) => Boolean) = {
    val currentPartInfo = current.pauses(pos)
    data.filter { p =>
      (p != current) &&
        pauseData(p, pos).exists(f(currentPartInfo, _))
    }
  }

  private def partWalkEvents(row: Participant)(
    fullPartInfo: FullPartInfo,
    pos: Int,
  ): Seq[WalkEvent] = {
    import fullPartInfo._
    val checkpoint = Checkpoint(pos, partMeta.place, partMeta.cumulativeTrackLength)
    Seq(
      WalkEvent.Departure(
        time = part.startTime,
        togetherWith = filterOthers(pos, row)((me, other) => me.startTime isSame other.startTime),
        checkpoint = checkpoint,
        isStart = previousPartMetaOption.isEmpty
      )
    ) ++ (
      part match {
        case PartTimeInfo.Finished(_startTime, endTime, intervalTime) =>
          val isFinish = pos == parts.size - 1
          //noinspection ConvertibleToMethodValue
          Seq(
            WalkEvent.Walk(
              duration = intervalTime,
              len = partMeta.trackLength,
              overtakes = Overtakes(
                overtook = filterOthers(pos, row)((me, other) => me overtook other),
                overtakenBy = filterOthers(pos, row)((me, other) => other overtook me),
              ),
            ),
            WalkEvent.Arrival(
              time = endTime,
              togetherWith = filterOthers(pos, row)((me, other) =>
                other.endTimeOption.exists(endTime isSame _)
              ),
              checkpoint = checkpoint,
              isFinish = isFinish,
            )
          ) ++ nextPartOption.fold[Seq[WalkEvent]](
            if (isFinish) Seq()
            else Seq(WalkEvent.GaveUp.AtCheckpoint(checkpoint))
          ) { nextPart =>
            Seq(
              WalkEvent.WaitingOnCheckpoint(
                checkpoint = checkpoint,
                duration = TimeInterval((nextPart.startTime - endTime) / 60 / 1000),
                overtakes = Overtakes(
                  overtook = filterOthersPauses(pos, row)((me, other) => me overtook other),
                  overtakenBy = filterOthersPauses(pos, row)((me, other) => other overtook me),
                )
              )
            )
          }
        case PartTimeInfo.Unfinished(_startTime) => Seq(WalkEvent.GaveUp.DuringWalk(pos + 1))
      }
    )
  }


  def walkEvents(row: Participant): Seq[WalkEvent] = {
    val prevParts = Seq(None) ++ parts.map(Some(_))
    val nextPartInfos: Seq[Option[PartTimeInfo]] = row.partTimes.drop(1).map(Some(_)) ++ Seq(None)

    (
      prevParts lazyZip
        parts lazyZip
        row.partTimes lazyZip
        nextPartInfos
    )
      .map(FullPartInfo)
      .zipWithIndex
      .flatMap((partWalkEvents(row) _).tupled)
  }


}
