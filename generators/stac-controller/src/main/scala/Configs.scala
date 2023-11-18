package staccontroller

import org.chipsalliance.cde.config.{Config, Field}
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.{BaseSubsystem, TLBusWrapperLocation}
import freechips.rocketchip.tilelink._

case class StacControllerParams(
    address: BigInt = 0x1000
)

case object StacControllerKey extends Field[Option[StacControllerAttachParams]](None)

trait CanHavePeripheryStacController { this: BaseSubsystem =>
  val stacControllerNode = p(StacControllerKey).map { params =>
    params.attachTo(this).ioNode.makeSink()
  }
}

trait HasPeripheryStacControllerModuleImp extends LazyModuleImp {
  val outer: CanHavePeripheryStacController
  val stacControllerIO = outer.stacControllerNode.map { stacControllerNode =>
    stacControllerNode.makeIO()(ValName("stac_controller"))
  }
}

class WithStacController(params: StacControllerParams) extends Config((site, here, up) => {
  case StacControllerKey => Some(StacControllerAttachParams(params))
})

class WithStacControllerLocation(where: TLBusWrapperLocation) extends Config((site, here, up) => {
  case StacControllerKey => up(StacControllerKey).map(_.copy(controlWhere = where))
})

class WithStacControllerCrossingType(xType: ClockCrossingType) extends Config((site, here, up) => {
  case StacControllerKey => up(StacControllerKey).map(_.copy(controlXType = xType))
})
