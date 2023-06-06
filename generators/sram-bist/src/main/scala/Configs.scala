package srambist

import org.chipsalliance.cde.config.{Config, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.{BaseSubsystem, TLBusWrapperLocation}
import freechips.rocketchip.tilelink._

case class SramBistParams(
    address: BigInt = 0x1000
)

case object SramBistKey extends Field[Option[SramBistAttachParams]](None)

trait CanHavePeripherySramBist { this: BaseSubsystem =>
  val sramBistNode = p(SramBistKey).map { params =>
    params.attachTo(this).ioNode.makeSink()
  }
}

trait HasPeripherySramBistModuleImp extends LazyModuleImp {
  val outer: CanHavePeripherySramBist
  val sramBistIO = outer.sramBistNode.map { sramBistNode =>
    sramBistNode.makeIO()(ValName("sram_bist"))
  }
}

class WithSramBist(params: SramBistParams) extends Config((site, here, up) => {
  case SramBistKey => Some(SramBistAttachParams(params))
})

class WithSramBistLocation(where: TLBusWrapperLocation) extends Config((site, here, up) => {
  case SramBistKey => up(SramBistKey).map(_.copy(controlWhere = where))
})

class WithSramBistCrossingType(xType: ClockCrossingType) extends Config((site, here, up) => {
  case SramBistKey => up(SramBistKey).map(_.copy(controlXType = xType))
})

object ChiseltestSramFailureMode extends Enumeration {
  type Type = Value
  val stuckAt, transition, none =
    Value
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
