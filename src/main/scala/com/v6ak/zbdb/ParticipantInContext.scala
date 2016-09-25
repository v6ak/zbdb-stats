package com.v6ak.zbdb

abstract sealed class ParticipantInContext {

  def participant: Participant

  def orderOption: Option[Int]

  def hasFinished: Boolean

}


object ParticipantInContext{

  @inline implicit def stripParticipant(pic: ParticipantInContext): Participant = pic.participant

  final case class Successful(participant: Participant, order: Int) extends ParticipantInContext{
    override def orderOption: Option[Int] = Some(order)
    override def hasFinished: Boolean = true
  }

  final case class Failed(participant: Participant) extends ParticipantInContext{
    override def orderOption: Option[Int] = None
    override def hasFinished: Boolean = false
  }

}