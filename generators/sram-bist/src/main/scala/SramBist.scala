package srambist

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

import srambist.analog.SramParams

class SramBistTopIO extends Bundle {
  val sramExtEn = Input(Bool())
  val sramScanMode = Input(Bool())
  val sramEn = Input(Bool())
  val sramScanIn = Input(Bool())
  val sramScanOut = Input(Bool())
  val sramScanEn = Input(Bool())
  val sramSaeClk = Input(Bool())
  val bistEn = Input(Bool())
  val bistDone = Output(Bool())
}

abstract class SramBist(busWidthBytes: Int, params: SramBistParams)(implicit
    p: Parameters
) extends IORegisterRouter(
      RegisterRouterParams(
        name = "SramBist",
        compat = Seq(),
        base = params.address,
        beatBytes = busWidthBytes
      ),
      new SramBistTopIO
    ) {

  lazy val module = new LazyModuleImp(this) {
    val io = ioNode.bundle
    val scanChainIntf = Module(new ScanChainIntf)
    val bistTop = Module(new BistTop(new BistTopParams))

    val ex = Wire(new DecoupledIO(Bool()))

    scanChainIntf.io.sramScanMode := io.sramScanMode
    scanChainIntf.io.sramScanIn := io.sramScanIn
    scanChainIntf.io.sramScanEn := io.sramScanEn
    io.sramScanOut := scanChainIntf.io.sramScanOut

    bistTop.io.sramExtEn := io.sramExtEn
    bistTop.io.sramScanMode := io.sramScanMode
    bistTop.io.sramEn := io.sramEn
    bistTop.io.bistEn := io.bistEn
    bistTop.io.saeClk := io.sramSaeClk
    bistTop.io.ex := ex.valid
    ex.ready := bistTop.io.done

    bistTop.io.addr := scanChainIntf.io.addr.q
    bistTop.io.din := scanChainIntf.io.din.q
    bistTop.io.mask := scanChainIntf.io.mask.q
    bistTop.io.we := scanChainIntf.io.we.q.asBool
    bistTop.io.sramId := scanChainIntf.io.sramId.q
    bistTop.io.sramSel := scanChainIntf.io.sramSel.q
    bistTop.io.saeCtl := scanChainIntf.io.saeCtl.q
    bistTop.io.saeSel := scanChainIntf.io.saeSel.q
    bistTop.io.bistRandSeed := scanChainIntf.io.bistRandSeed.q
    bistTop.io.bistSigSeed := scanChainIntf.io.bistSigSeed.q
    bistTop.io.bistMaxRowAddr := scanChainIntf.io.bistMaxRowAddr.q
    bistTop.io.bistMaxColAddr := scanChainIntf.io.bistMaxColAddr.q
    bistTop.io.bistInnerDim := scanChainIntf.io.bistInnerDim.q
    bistTop.io.bistPatternTable := scanChainIntf.io.bistPatternTable.q
    bistTop.io.bistElementSequence := scanChainIntf.io.bistElementSequence.q
    bistTop.io.bistMaxElementIdx := scanChainIntf.io.bistMaxElementIdx.q
    bistTop.io.bistCycleLimit := scanChainIntf.io.bistCycleLimit.q

    scanChainIntf.io.dout := bistTop.io.dout
    scanChainIntf.io.tdc := bistTop.io.tdc
    scanChainIntf.io.done := bistTop.io.done.asUInt

    scanChainIntf.io.bistFail := bistTop.io.bistFail.asUInt
    scanChainIntf.io.bistFailCycle := bistTop.io.bistFailCycle
    scanChainIntf.io.bistExpected := bistTop.io.bistExpected
    scanChainIntf.io.bistReceived := bistTop.io.bistReceived
    scanChainIntf.io.bistSignature := bistTop.io.bistSignature

    io.bistDone := bistTop.io.bistDone

    regmap(
      SramBistCtrlRegs.ADDR -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.ADDR, scanChainIntf.io.addr)
      ),
      SramBistCtrlRegs.DIN -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.DIN, scanChainIntf.io.din)
      ),
      SramBistCtrlRegs.MASK -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.MASK, scanChainIntf.io.mask)
      ),
      SramBistCtrlRegs.WE -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.WE, scanChainIntf.io.we)
      ),
      SramBistCtrlRegs.SRAM_ID -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.SRAM_ID, scanChainIntf.io.sramId)
      ),
      SramBistCtrlRegs.SRAM_SEL -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.SRAM_SEL, scanChainIntf.io.sramSel)
      ),
      SramBistCtrlRegs.SAE_CTL -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.SAE_CTL, scanChainIntf.io.saeCtl)
      ),
      SramBistCtrlRegs.SAE_SEL -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.SAE_SEL, scanChainIntf.io.saeSel)
      ),
      SramBistCtrlRegs.DOUT -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.DOUT, scanChainIntf.io.doutMmio)
      ),
      SramBistCtrlRegs.TDC -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.TDC, scanChainIntf.io.tdcMmio)
      ),
      SramBistCtrlRegs.EX -> Seq(RegField.w(1, ex)),
      SramBistCtrlRegs.DONE -> Seq(
        RegField.rwReg(SramBistCtrlRegWidths.DONE, scanChainIntf.io.doneMmio)
      ),
      SramBistCtrlRegs.BIST_RAND_SEED -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_RAND_SEED,
          scanChainIntf.io.bistRandSeed
        )
      ),
      SramBistCtrlRegs.BIST_SIG_SEED -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_SIG_SEED,
          scanChainIntf.io.bistSigSeed
        )
      ),
      SramBistCtrlRegs.BIST_MAX_ROW_ADDR -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_MAX_ROW_ADDR,
          scanChainIntf.io.bistMaxRowAddr
        )
      ),
      SramBistCtrlRegs.BIST_MAX_COL_ADDR -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_MAX_COL_ADDR,
          scanChainIntf.io.bistMaxColAddr
        )
      ),
      SramBistCtrlRegs.BIST_INNER_DIM -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_INNER_DIM,
          scanChainIntf.io.bistInnerDim
        )
      ),
      SramBistCtrlRegs.BIST_ELEMENT_SEQUENCE -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_ELEMENT_SEQUENCE,
          scanChainIntf.io.bistElementSequence
        )
      ),
      SramBistCtrlRegs.BIST_PATTERN_TABLE -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_PATTERN_TABLE,
          scanChainIntf.io.bistPatternTable
        )
      ),
      SramBistCtrlRegs.BIST_MAX_ELEMENT_IDX -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_MAX_ELEMENT_IDX,
          scanChainIntf.io.bistMaxElementIdx
        )
      ),
      SramBistCtrlRegs.BIST_CYCLE_LIMIT -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_CYCLE_LIMIT,
          scanChainIntf.io.bistCycleLimit
        )
      ),
      SramBistCtrlRegs.BIST_FAIL -> Seq(
        RegField
          .rwReg(SramBistCtrlRegWidths.BIST_FAIL, scanChainIntf.io.bistFailMmio)
      ),
      SramBistCtrlRegs.BIST_FAIL_CYCLE -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_FAIL_CYCLE,
          scanChainIntf.io.bistFailCycleMmio
        )
      ),
      SramBistCtrlRegs.BIST_EXPECTED -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_EXPECTED,
          scanChainIntf.io.bistExpectedMmio
        )
      ),
      SramBistCtrlRegs.BIST_RECEIVED -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_RECEIVED,
          scanChainIntf.io.bistReceivedMmio
        )
      ),
      SramBistCtrlRegs.BIST_SIGNATURE -> Seq(
        RegField.rwReg(
          SramBistCtrlRegWidths.BIST_SIGNATURE,
          scanChainIntf.io.bistSignatureMmio
        )
      )
    )
  }
}

class TLSramBist(busWidthBytes: Int, params: SramBistParams)(implicit
    p: Parameters
) extends SramBist(busWidthBytes, params)
    with HasTLControlRegMap

case class SramBistAttachParams(
    device: SramBistParams,
    controlWhere: TLBusWrapperLocation = PBUS,
    blockerAddr: Option[BigInt] = None,
    controlXType: ClockCrossingType = NoCrossing,
    intXType: ClockCrossingType = NoCrossing
) {
  def attachTo(where: Attachable)(implicit p: Parameters): TLSramBist = {
    val name = "sram_bist"
    val tlbus = where.locateTLBusWrapper(controlWhere)
    val sramBistClockDomainWrapper = LazyModule(
      new ClockSinkDomain(take = None)
    )
    val sramBist = sramBistClockDomainWrapper {
      LazyModule(new TLSramBist(tlbus.beatBytes, device))
    }
    sramBist.suggestName(name)

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

      sramBistClockDomainWrapper.clockNode := (controlXType match {
        case _: SynchronousCrossing =>
          tlbus.dtsClk.map(_.bind(sramBist.device))
          tlbus.fixedClockNode
        case _: RationalCrossing =>
          tlbus.clockNode
        case _: AsynchronousCrossing =>
          val sramBistClockGroup = ClockGroup()
          sramBistClockGroup := where.asyncClockGroupsNode
          blockerOpt.map { _.clockNode := sramBistClockGroup }.getOrElse {
            sramBistClockGroup
          }
      })

      (sramBist.controlXing(controlXType)
        := TLFragmenter(tlbus)
        := blockerOpt.map { _.node := bus }.getOrElse { bus })
    }

    sramBist
  }
}

object SramBist {
  val nextId = { var i = -1; () => { i += 1; i } }

  def makePort(node: BundleBridgeSource[SramBistTopIO], name: String)(implicit
      p: Parameters
  ): ModuleValue[SramBistTopIO] = {
    val sramBistNode = node.makeSink()
    InModuleBody { sramBistNode.makeIO()(ValName(name)) }
  }
}
