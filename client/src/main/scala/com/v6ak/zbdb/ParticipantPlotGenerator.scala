package com.v6ak.zbdb

final case class ParticipantPlotGenerator(nameGenitive: String, nameAccusative: String, glyphiconName: String, generator: Seq[Participant] => PlotData)