package staccontroller

import chisel3._
import chisel3.util._
import org.chipsalliance.cde.config.Parameters
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.interrupts._
import freechips.rocketchip.prci._
import freechips.rocketchip.regmapper._
import freechips.rocketchip.subsystem._
import freechips.rocketchip.tilelink._
import freechips.rocketchip.devices.tilelink._
import freechips.rocketchip.util._

import staccontroller.StacControllerCtrlRegs._

class StacControllerTopIO extends Bundle {
  val sramExtEn = Output(Bool())
  val sramScanMode = Output(Bool())
  val sramEn = Output(Bool())
  val sramScanIn = Output(Bool())
  val sramScanEn = Output(Bool())
  val sramBistEn = Output(Bool())
  val sramBistStart = Output(Bool())
  val clkSel = Output(UInt(2.W))
  val pllSel = Output(Bool())
  val pllScanEn = Output(Bool())
  val pllScanRst = Output(Bool())
  val pllScanClk = Output(Bool())
  val pllScanIn = Output(Bool())
  val pllArstb = Output(Bool())
  val customBoot = Output(Bool())
  val sramScanOut = Input(Bool())
  val sramBistDone = Input(Bool())
  val pllScanOut = Input(Bool())
}

class StacControllerIO extends Bundle {
  val top = new StacControllerTopIO
  val mmio = new StacControllerMmioRegIO
}

class StacController()(implicit p: Parameters) extends Module {
  val io = IO(new StacControllerIO)

  io.top.sramExtEn := io.mmio.sramExtEn.q
  io.top.sramScanMode := io.mmio.sramScanMode.q
  io.top.sramEn := io.mmio.sramEn.q
  io.top.sramScanIn := io.mmio.sramScanIn.q
  io.top.sramScanEn := io.mmio.sramScanEn.q
  io.top.sramBistEn := io.mmio.sramBistEn.q
  io.top.sramBistStart := io.mmio.sramBistStart.q
  io.top.clkSel := io.mmio.clkSel.q
  io.top.pllSel := io.mmio.pllSel.q
  io.top.pllScanEn := io.mmio.pllScanEn.q
  io.top.pllScanRst := io.mmio.pllScanRst.q
  io.top.pllScanClk := io.mmio.pllScanClk.q
  io.top.pllScanIn := io.mmio.pllScanIn.q
  io.top.pllArstb := io.mmio.pllArstb.q
  io.top.customBoot := io.mmio.customBoot.q
  io.mmio.sramScanOut.d := io.top.sramScanOut
  io.mmio.sramBistDone.d := io.top.sramBistDone
  io.mmio.pllScanOut.d := io.top.pllScanOut
}

abstract class StacControllerRouter(busWidthBytes: Int, params: StacControllerParams)(
    implicit p: Parameters
) extends IORegisterRouter(
      RegisterRouterParams(
        name = "StacController",
        compat = Seq(),
        base = params.address,
        beatBytes = busWidthBytes
      ),
      new StacControllerTopIO
    ) {

  lazy val module = new LazyModuleImp(this) {
    val io = ioNode.bundle

    val stacController = Module(new StacController())

    io <> stacController.io.top

    regmap(
      REGMAP_OFFSET(SRAM_EXT_EN) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_EXT_EN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_SCAN_MODE) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SCAN_MODE), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_EN) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_EN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_SCAN_IN) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SCAN_IN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_SCAN_EN) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SCAN_EN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_BIST_EN) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_BIST_EN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_BIST_START) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_BIST_START), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_SCAN_OUT) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SCAN_OUT), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(SRAM_BIST_DONE) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_BIST_DONE), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(CLK_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(CLK_SEL), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SEL), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SCAN_EN) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SCAN_EN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SCAN_RST) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SCAN_RST), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SCAN_CLK) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SCAN_CLK), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SCAN_IN) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SCAN_IN), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_SCAN_OUT) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_SCAN_OUT), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(PLL_ARSTB) -> Seq(
        RegField.rwReg(REG_WIDTH(PLL_ARSTB), stacController.io.mmio.sramExtEn)
      ),
      REGMAP_OFFSET(CUSTOM_BOOT) -> Seq(
        RegField.rwReg(REG_WIDTH(CUSTOM_BOOT), stacController.io.mmio.sramExtEn)
      ),
    )
  }
}

class TLStacController(busWidthBytes: Int, params: StacControllerParams)(implicit
    p: Parameters
) extends StacControllerRouter(busWidthBytes, params)
    with HasTLControlRegMap

case class StacControllerAttachParams(
    device: StacControllerParams,
    controlWhere: TLBusWrapperLocation = SBUS,
    blockerAddr: Option[BigInt] = None,
    controlXType: ClockCrossingType = NoCrossing,
) {
  def attachTo(where: Attachable)(implicit p: Parameters): TLStacController = {
    val name = s"stac_controller_${StacController.nextId()}"
    val tlbus = where.locateTLBusWrapper(controlWhere)
    val stacControllerClockDomainWrapper = LazyModule(
      new ClockSinkDomain(take = None)
    )
    val stacController = stacControllerClockDomainWrapper {
      LazyModule(new TLStacController(tlbus.beatBytes, device))
    }
    stacController.suggestName(name)

    tlbus.coupleTo(s"device_named_$name") { bus =>

      val blockerOpt = blockerAddr.map { a =>
        val blocker = LazyModule(
          new TLClockBlocker(
            BasicBusBlockerParams(a, tlbus.beatBytes, tlbus.beatBytes)
          )
        )
        tlbus.coupleTo(s"bus_blocker_for_$name") {
          blocker.controlNode := TLFragmenter(tlbus) := _
        }
        blocker
      }

      stacControllerClockDomainWrapper.clockNode := (controlXType match {
        case _: SynchronousCrossing =>
          tlbus.dtsClk.map(_.bind(stacController.device))
          tlbus.fixedClockNode
        case _: RationalCrossing =>
          tlbus.clockNode
        case _: AsynchronousCrossing =>
          val stacControllerClockGroup = ClockGroup()
          stacControllerClockGroup := where.asyncClockGroupsNode
          blockerOpt.map { _.clockNode := stacControllerClockGroup }.getOrElse {
            stacControllerClockGroup
          }
      })

      (stacController.controlXing(controlXType)
        := TLFragmenter(tlbus)
        := blockerOpt.map { _.node := bus }.getOrElse { bus })
    }

    stacController
  }
}

object StacController {
  val nextId = { var i = -1; () => { i += 1; i } }
}
