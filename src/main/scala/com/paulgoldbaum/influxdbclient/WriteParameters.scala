package com.paulgoldbaum.influxdbclient

object WriteParameters {

  object Precision {
    sealed abstract class Precision(val str: String)

    case object NANOSECONDS extends Precision("n")
    case object MICROSECONDS extends Precision("u")
    case object MILLISECONDS extends Precision("ms")
    case object SECONDS extends Precision("s")
    case object MINUTES extends Precision("m")
    case object HOURS extends Precision("h")
  }

  object Consistency {
    sealed abstract class Consistency(val str: String)

    case object ONE extends Consistency("one")
    case object QUORUM extends Consistency("quorum")
    case object ALL extends Consistency("all")
    case object ANY extends Consistency("any")
  }

}