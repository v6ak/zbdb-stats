package com.v6ak.zbdb

final case class ParticipantPlotGenerator(
  nameGenitive: String,
  nameAccusative: String,
  glyph: Glyph,
  generator: Seq[Participant] => PlotData
)