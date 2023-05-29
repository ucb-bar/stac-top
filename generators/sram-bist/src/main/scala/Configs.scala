package srambist

import org.chipsalliance.cde.config.{Field, Config}

case class ProgrammableBistParams(
    elementTableLength: Int = 8,
    operationsPerElement: Int = 8,
    patternTableLength: Int = 8,
    maxRowAddrWidth: Int = 10,
    maxColAddrWidth: Int = 3,
    dataWidth: Int = 32
) {
  def seedWidth = dataWidth + maxColAddrWidth + maxRowAddrWidth
}

case class SramTestUnitParams(
    programmableBistParams: ProgrammableBistParams
)

object ChiseltestSramFailureMode extends Enumeration {
  type ChiseltestSramFailureMode = Value
  val stuckAt, stuckOpen, transition, none =
    Value // TODO: Add relevant failure modes
}

case object WithChiseltestSramsKey
    extends Field[Option[ChiseltestSramFailureMode.ChiseltestSramFailureMode]](
      None
    )

class WithChiseltestSrams(
    failureMode: ChiseltestSramFailureMode.ChiseltestSramFailureMode
) extends Config((site, here, up) => { case WithChiseltestSramsKey =>
      Some(failureMode)
    })
/*
// case object ProgrammableBistKey extends Field[Option[ProgrammableBistParams]](None)
case object SramTestUnitKey extends Field[Option[SramTestUnitParams]](None)

trait CanHaveSramBist {
  // connect sram unit to subsystem here
}

class WithSramBist(base: BigInt) extends Config((site, here, up) => {
  case SramTestUnitKey => Some(SramTestUnitParams(
    programmableBistParams = ProgrammableBistParams(
      addr = base)
    ))
})

//trait CanHavePeripheryProgrammableBist {
// connect Programmable BIST to subsystem here
//}

// class WithProgrammableBist() extends Config((site, here, up) => {
//  case ProgrammableBistKey => Some(ProgrammableBistParams())
//})
 */
