package chipyard.stac.clocking

import barstools.iocell.chisel.IOCell
import chipyard.HasHarnessSignalReferences
import chipyard.clocking.{ClockWithFreq, HasChipyardPRCI, TLClockDivider, TLClockSelector}
import chipyard.harness.{ComposeHarnessBinder, OverrideHarnessBinder}
import chipyard.iobinders.{GetSystemParameters, IOCellKey, OverrideLazyIOBinder}
import chisel3._
import chisel3.experimental.Analog
import freechips.rocketchip.diplomacy.{InModuleBody, LazyModule, LazyModuleImp}
import freechips.rocketchip.prci._
import freechips.rocketchip.subsystem.BaseSubsystem
import freechips.rocketchip.tilelink.TLBuffer
import org.chipsalliance.cde.config.Parameters

import scala.collection.mutable

class MultiPLLTopIO extends Bundle {
  val scan_in = Input(Bool())
  val scan_clk = Input(Bool())
  val scan_en = Input(Bool())
  val scan_rst = Input(Bool())
  val scan_out = Output(Bool())
  val arstb = Input(Bool())
  val sel = Input(Bool())
  // Chisel can't treat the wire as a pass-through when <>'ing with
  // Analog, so we need to do them separately
//  val ref_in = Analog(1.W)
//  val clk_out = Analog(1.W)
//  val div_out = Analog(1.W)
}

class WithMultiPLLClockGenerator extends OverrideLazyIOBinder({
  (system: HasChipyardPRCI) => {
    // Connect the implicit clock
    implicit val p: Parameters = GetSystemParameters(system)
    val implicitClockSinkNode = ClockSinkNode(Seq(ClockSinkParameters(name = Some("implicit_clock"))))
    system.connectImplicitClockSinkNode(implicitClockSinkNode)
    InModuleBody {
      val implicit_clock = implicitClockSinkNode.in.head._1.clock
      val implicit_reset = implicitClockSinkNode.in.head._1.reset
      system.asInstanceOf[BaseSubsystem].module match { case l: LazyModuleImp => {
        l.clock := implicit_clock
        l.reset := implicit_reset
      }}
    }
    val tlbus = system.asInstanceOf[BaseSubsystem].locateTLBusWrapper(system.prciParams.slaveWhere)
    val baseAddress = system.prciParams.baseAddress
    val clockDivider  = system.prci_ctrl_domain { LazyModule(new TLClockDivider (baseAddress + 0x20000, tlbus.beatBytes)) }
    val clockSelector = system.prci_ctrl_domain { LazyModule(new IOClockSelector(2)) }

    tlbus.toVariableWidthSlave(Some("clock-div-ctrl")) { clockDivider.tlNode := TLBuffer() }
    val clockSelectorIO = clockSelector.ioNode.makeSink()

    system.allClockGroupsNode := clockDivider.clockNode := clockSelector.clockNode

    // Connect all other requested clocks
    val slowClockSource = ClockSourceNode(Seq(ClockSourceParameters(name = Some("clkext"))))
    val pllClockSourceOut = ClockSourceNode(Seq(ClockSourceParameters(name = Some("pll_clk_out"))))
    val pllClockSourceDiv = ClockSourceNode(Seq(ClockSourceParameters(name = Some("pll_clk_div"))))

    // The order of the connections to clockSelector.clockNode configures the inputs
    // of the clockSelector's clockMux. Default to using the slowClockSource,
    // software should enable the PLL, then switch to the pllClockSource
    clockSelector.clockNode := slowClockSource
    clockSelector.clockNode := pllClockSourceOut
    clockSelector.clockNode := pllClockSourceDiv

    InModuleBody {
      val binderNodes = mutable.Buffer[Data]()
      val binderCells = mutable.Buffer[IOCell]()

      def addIOCell(data: (Data, Seq[IOCell])): Unit = {
        binderNodes += data._1
        binderCells ++= data._2
      }

      def generateIO(
        coreSignal: Data,
        name: String,
        abstractResetAsAsync: Boolean = false
      ): Unit =
        addIOCell(IOCell.generateIOFromSignal(
          coreSignal,
          name,
          p(IOCellKey),
          abstractResetAsAsync = abstractResetAsAsync)
        )

      // Main digital clock + reset
      val clock_wire = Wire(Input(new ClockWithFreq(80)))
      val reset_wire = Wire(Input(AsyncReset()))
      generateIO(clock_wire, "clock")
      generateIO(reset_wire, "reset")

      slowClockSource.out.map(_._1).foreach { o =>
        o.clock := clock_wire.clock
        o.reset := reset_wire
      }

      generateIO(clockSelectorIO.bundle.sel, "clksel")

      // PLL IO
      val pll = Module(new MultiPLL)
      val pllIO = Wire(new MultiPLLTopIO)
      pll.io.io_scan_in <> pllIO.scan_in
      pll.io.io_scan_clk <> pllIO.scan_clk
      pll.io.io_scan_en <> pllIO.scan_en
      pll.io.io_scan_rst <> pllIO.scan_rst
      pll.io.io_scan_out <> pllIO.scan_out
      pll.io.io_arstb <> pllIO.arstb
      pll.io.io_pll_sel <> pllIO.sel
      generateIO(pllIO, "pll")
      // see comment in MultiPLLTopIO
      generateIO(pll.io.clock, "pll_ref_in")
      generateIO(pll.io.io_pll_clk_out_gr, "pll_clk_out")
      generateIO(pll.io.io_pll_div_out_gr, "pll_div_out")

      // PLL clock outputs
      pllClockSourceOut.out.map(_._1).foreach { o =>
        o.clock := pll.io.io_pll_clk_out.asClock
        o.reset := reset_wire
      }
      pllClockSourceDiv.out.map(_._1).foreach { o =>
        o.clock := pll.io.io_pll_div_out.asClock
        o.reset := reset_wire
      }

      (binderNodes.toSeq, binderCells.toSeq)
    }
  }
})

// extremely lazy harness binders
class WithClockAndResetFromHarnessAndMultiPLLTiedOff extends OverrideHarnessBinder({
  (system: HasChipyardPRCI, th: HasHarnessSignalReferences, ports: Seq[Data]) => {
    implicit val p = GetSystemParameters(system)
    ports.map ({
      case c: ClockWithFreq => {
        th.setRefClockFreq(c.freqMHz)
        c.clock := th.buildtopClock
      }
      case r: AsyncReset => r := th.buildtopReset.asAsyncReset
      case d: UInt => d.name match {
        case "clksel" => d := 0.U
      }
      case pll: MultiPLLTopIO =>
        pll.scan_en := false.B
        pll.scan_clk := false.B
        pll.scan_in := false.B
        pll.scan_rst := false.B
        pll.arstb := !th.buildtopReset.asBool
        pll.sel := false.B // on
      case ana: Analog =>
    })
  }
})
