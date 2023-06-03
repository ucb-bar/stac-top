package srambist

import org.chipsalliance.cde.config.{Field, Config}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.tilelink._

case class SramBistParams(
    address: BigInt = 0x1000
)

case object SramBistKey extends Field[Option[SramBistParams]](None)

trait CanHavePeripherySramBist { this: BaseSubsystem =>
  val sramBist = p(SramBistKey).map { params =>
    SramBistAttachParams(params).attachTo(this).ioNode.makeSink()
  }
}

trait HasPeripherySramBistModuleImp extends LazyModuleImp {
  val outer: CanHavePeripherySramBist
  val io = outer.sramBist.map { sramBist =>
    sramBist.makeIO()(ValName("sram_bist"))
  }
}

class WithSramBist(params: SramBistParams)
    extends Config((site, here, up) => { case SramBistKey =>
      Some(params)
    })

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
