package sramtestunit

case class ProgrammableBistParams(
  elementTableLength : Int = 8,
  operationsPerElement : Int = 8,
  patternTableLength : Int = 8,
)


// case object ProgrammableBistKey extends Field[Option[ProgrammableBistParams]](None)

//trait CanHavePeripheryProgrammableBist {
  // connect Programmable BIST to subsystem here
//}

// class WithProgrammableBist() extends Config((site, here, up) => {
//  case ProgrammableBistKey => Some(ProgrammableBistParams())
//})
