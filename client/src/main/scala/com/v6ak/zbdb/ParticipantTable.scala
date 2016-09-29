package com.v6ak.zbdb

import com.example.moment.Moment
import com.v6ak.scalajs.time.TimeInterval.TimeIntervalOrdering

object ParticipantTable {
}

final case class ParticipantTable (startTime: Moment, parts: Seq[Part], data: Seq[Participant], formatVersion: FormatVersion) {
  import ParticipantTable._

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

}
