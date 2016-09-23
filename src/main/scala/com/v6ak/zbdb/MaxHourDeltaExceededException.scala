package com.v6ak.zbdb

import com.example.moment.Moment

case class MaxHourDeltaExceededException(maxHourDelta: Int, prevTime: Moment, currentTime: Moment) extends RuntimeException(s"following time overreaches time delta $maxHourDelta hours: $currentTime (previous time: $prevTime)")
