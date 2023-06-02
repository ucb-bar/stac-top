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
import srambist.sramharness.SaeSrc
import srambist.SramBistCtrlRegs._

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
    val bistTopParams = new BistTopParams
    val bistTop = Module(new BistTop(bistTopParams))

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
    bistTop.io.sramSel := scanChainIntf.io.sramSel.q.asTypeOf(SramSrc())
    bistTop.io.saeCtl := scanChainIntf.io.saeCtl.q
    bistTop.io.saeSel := scanChainIntf.io.saeSel.q.asTypeOf(SaeSrc())
    bistTop.io.bistRandSeed := scanChainIntf.io.bistRandSeed
    bistTop.io.bistSigSeed := scanChainIntf.io.bistSigSeed.q
    bistTop.io.bistMaxRowAddr := scanChainIntf.io.bistMaxRowAddr.q
    bistTop.io.bistMaxColAddr := scanChainIntf.io.bistMaxColAddr.q
    bistTop.io.bistInnerDim := scanChainIntf.io.bistInnerDim.q.asTypeOf(bistTop.bist.Dimension())
    bistTop.io.bistPatternTable := scanChainIntf.io.bistPatternTable.asTypeOf(Vec(bistTopParams.bistParams.patternTableLength, UInt(bistTopParams.bistParams.dataWidth.W)))
    bistTop.io.bistElementSequence := scanChainIntf.io.bistElementSequence.asTypeOf(Vec(bistTopParams.bistParams.elementTableLength, new bistTop.bist.Element()))
    bistTop.io.bistMaxElementIdx := scanChainIntf.io.bistMaxElementIdx.q
    bistTop.io.bistCycleLimit := scanChainIntf.io.bistCycleLimit.q
    bistTop.io.bistStopOnFailure := scanChainIntf.io.bistStopOnFailure.q.asBool

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
      SCAN_CHAIN_OFFSET(ADDR) -> Seq(
        RegField.rwReg(REG_WIDTH(ADDR), scanChainIntf.io.addr)
      ),
      SCAN_CHAIN_OFFSET(DIN) -> Seq(
        RegField.rwReg(REG_WIDTH(DIN), scanChainIntf.io.din)
      ),
      SCAN_CHAIN_OFFSET(MASK) -> Seq(
        RegField.rwReg(REG_WIDTH(MASK), scanChainIntf.io.mask)
      ),
      SCAN_CHAIN_OFFSET(WE) -> Seq(
        RegField.rwReg(REG_WIDTH(WE), scanChainIntf.io.we)
      ),
      SCAN_CHAIN_OFFSET(SRAM_ID) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_ID), scanChainIntf.io.sramId)
      ),
      SCAN_CHAIN_OFFSET(SRAM_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(SRAM_SEL), scanChainIntf.io.sramSel)
      ),
      SCAN_CHAIN_OFFSET(SAE_CTL) -> Seq(
        RegField.rwReg(REG_WIDTH(SAE_CTL), scanChainIntf.io.saeCtl)
      ),
      SCAN_CHAIN_OFFSET(SAE_SEL) -> Seq(
        RegField.rwReg(REG_WIDTH(SAE_SEL), scanChainIntf.io.saeSel)
      ),
      SCAN_CHAIN_OFFSET(DOUT) -> Seq(
        RegField.rwReg(REG_WIDTH(DOUT), scanChainIntf.io.doutMmio)
      ),
    SCAN_CHAIN_OFFSET(TDC) -> (0 until 4) .map { i => 
        RegField.rwReg(REG_WIDTH(TDC), scanChainIntf.io.tdcMmio(i))
      },
      SCAN_CHAIN_OFFSET(EX) -> Seq(RegField.w(1, ex)),
      SCAN_CHAIN_OFFSET(DONE) -> Seq(
        RegField.rwReg(REG_WIDTH(DONE), scanChainIntf.io.doneMmio)
      ),
    SCAN_CHAIN_OFFSET(BIST_RAND_SEED) -> (0 until 2).map { i =>
        RegField.rwReg(
          REG_WIDTH(BIST_RAND_SEED),
          scanChainIntf.io.bistRandSeedMmio(i)
        )
      },
      SCAN_CHAIN_OFFSET(BIST_SIG_SEED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_SIG_SEED),
          scanChainIntf.io.bistSigSeed
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_MAX_ROW_ADDR) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_ROW_ADDR),
          scanChainIntf.io.bistMaxRowAddr
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_MAX_COL_ADDR) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_COL_ADDR),
          scanChainIntf.io.bistMaxColAddr
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_INNER_DIM) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_INNER_DIM),
          scanChainIntf.io.bistInnerDim
        )
      ),
    SCAN_CHAIN_OFFSET(BIST_ELEMENT_SEQUENCE) -> (0 until 9).map { i =>
        RegField.rwReg(
          REG_WIDTH(BIST_ELEMENT_SEQUENCE),
          scanChainIntf.io.bistElementSequenceMmio(i)
        )
      },
      SCAN_CHAIN_OFFSET(BIST_PATTERN_TABLE) -> (0 until 4).map { i =>
        RegField.rwReg(
          REG_WIDTH(BIST_PATTERN_TABLE),
          scanChainIntf.io.bistPatternTableMmio(i)
        )
      },
      SCAN_CHAIN_OFFSET(BIST_MAX_ELEMENT_IDX) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_MAX_ELEMENT_IDX),
          scanChainIntf.io.bistMaxElementIdx
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_CYCLE_LIMIT) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_CYCLE_LIMIT),
          scanChainIntf.io.bistCycleLimit
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_STOP_ON_FAILURE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_STOP_ON_FAILURE),
          scanChainIntf.io.bistStopOnFailure
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_FAIL) -> Seq(
        RegField
          .rwReg(REG_WIDTH(BIST_FAIL), scanChainIntf.io.bistFailMmio)
      ),
      SCAN_CHAIN_OFFSET(BIST_FAIL_CYCLE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_FAIL_CYCLE),
          scanChainIntf.io.bistFailCycleMmio
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_EXPECTED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_EXPECTED),
          scanChainIntf.io.bistExpectedMmio
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_RECEIVED) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_RECEIVED),
          scanChainIntf.io.bistReceivedMmio
        )
      ),
      SCAN_CHAIN_OFFSET(BIST_SIGNATURE) -> Seq(
        RegField.rwReg(
          REG_WIDTH(BIST_SIGNATURE),
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
