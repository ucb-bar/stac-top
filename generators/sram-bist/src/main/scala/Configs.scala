package srambist

import org.chipsalliance.cde.config.{Field, Config}

case class SramBistParams(
    address: BigInt = 0x1000
)

case object SramBistKey extends Field[Option[SramBistParams]](None)

object ChiseltestSramFailureMode extends Enumeration {
  type Type = Value
  val stuckAt, transition, none =
    Value // TODO: Add relevant failure modes
}

case object WithChiseltestSramsKey
    extends Field[Option[ChiseltestSramFailureMode.Type]](
      None
    )

class WithChiseltestSrams(
    failureMode: ChiseltestSramFailureMode.Type
) extends Config((site, here, up) => { case WithChiseltestSramsKey =>
      Some(failureMode)
    })
