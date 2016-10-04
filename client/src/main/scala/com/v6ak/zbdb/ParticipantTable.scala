package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval.TimeIntervalOrdering

object ParticipantTable {
}

final case class ParticipantTable (startTime: Moment, parts: Seq[Part], data: Seq[Participant], formatVersion: FormatVersion) {

  val participantMap = {
    def processOrderGroup(order: Int, members: Seq[Participant]): Seq[ParticipantInContext.Successful] = members.map(participant => ParticipantInContext.Successful(participant, order))
    val (finishedParticipants, unfinishedParticipants) = data.partition(hasFinished)
    val finishedParticipantsGroups = finishedParticipants.groupBy(_.totalTime).toIndexedSeq.sortBy(_._1) match {
      case Seq() => Seq()
      case orderGroups =>
        orderGroups.map(_._2).tail.scanLeft(processOrderGroup(1, orderGroups.head._2))((previousGroup, currentGroup) => processOrderGroup(previousGroup(0).order + previousGroup.size, currentGroup))
    }
    val finishedParticipantsInContext = finishedParticipantsGroups.flatten
    val unfinishedParticipantsInContext = unfinishedParticipants.map(ParticipantInContext.Failed)
    val allParticipantsInContext = finishedParticipantsInContext ++ unfinishedParticipantsInContext
    allParticipantsInContext.map(pic => pic.participant -> pic).toMap
  }

  implicit def toParticipantInContext(participant: Participant): ParticipantInContext = participantMap(participant)

  private def hasFinished(row: Participant) = (row.partTimes.size == parts.size) && row.partTimes.forall(_.isInstanceOf[PartTimeInfo.Finished])

  private def expandSeq[T](s: Seq[T], empty: T, size: Int) = s ++ Seq.fill(size - s.size)(empty)

  private def longZip[T, U](a: Seq[T], emptyA: T)(b: Seq[U], emptyB: U) = {
    val maxSize = a.size max b.size
    val ea = expandSeq(a, emptyA, maxSize)
    val eb = expandSeq(b, emptyB, maxSize)
    (ea, eb).zipped
  }

  val firsts = data.foldLeft(Seq.empty[BestParticipantData]){ (fastestSoFar, participant) =>
    longZip[BestParticipantData, Option[PartTimeInfo]](
      fastestSoFar, BestParticipantData.Empty
    )(
      participant.partTimes.map(Some(_)), None
    ).map{(fastestParticipantSoFar, current) => fastestParticipantSoFar.merge(current)}
  }

}
