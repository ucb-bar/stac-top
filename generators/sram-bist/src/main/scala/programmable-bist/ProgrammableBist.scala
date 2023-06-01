package srambist.programmablebist

import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chisel3.util.random.MaxPeriodFibonacciLFSR

case class ProgrammableBistParams(
    elementTableLength: Int = 8,
    operationsPerElement: Int = 8,
    patternTableLength: Int = 8,
    maxRowAddrWidth: Int = 10,
    maxColAddrWidth: Int = 3,
    dataWidth: Int = 32,
    randAddrWidth: Int = 14
) {
  // data + mask + row addr + col addr + operation
  def seedWidth = 2 * dataWidth + maxColAddrWidth + maxRowAddrWidth + 1
}

class ProgrammableBist(val params: ProgrammableBistParams) extends Module {

  object OperationType extends ChiselEnum {
    val read = Value(0.U(2.W))
    val write = Value(1.U(2.W))
    val rand = Value(2.U(2.W))
  }

  object FlipType extends ChiselEnum {
    val flipped = Value(0.U(1.W))
    val unflipped = Value(1.U(1.W))
  }

  object Direction extends ChiselEnum {
    val up = Value(0.U(2.W))
    val down = Value(1.U(2.W))
    val rand = Value(2.U(2.W))
  }

  object ElementType extends ChiselEnum {
    val waitOp = Value(0.U(1.W))
    val rwOp = Value(1.U(1.W))
  }

  object Dimension extends ChiselEnum {
    val row = Value(0.U(1.W))
    val col = Value(1.U(1.W))
  }

  class Operation extends Bundle {
    val operationType = OperationType()
    // Randomize data?
    val randData = Bool()
    // Randomize mask?
    val randMask = Bool()

    // Data pattern address if data is not randomized.
    val dataPatternIdx = UInt(log2Ceil(params.patternTableLength).W)
    // Mask pattern address if mask is not randomized.
    val maskPatternIdx = UInt(log2Ceil(params.patternTableLength).W)

    // Bitwise flip data?
    val flipped = FlipType()
  }

  class OperationElement extends Bundle {
    val operations = Vec(params.operationsPerElement, new Operation())
    // Number of operations minus 1
    val maxIdx = UInt(log2Ceil(params.operationsPerElement).W)
    val dir = Direction()
    // Number of random addresses to try.
    // Only used if `dir` is set to `rand`.
    val numAddrs = UInt(params.randAddrWidth.W)
  }

  class WaitElement extends Bundle {
    val cyclesToWait = UInt(params.randAddrWidth.W)
  }

  class Element extends Bundle {
    val operationElement = new OperationElement()
    val waitElement = new WaitElement()
    val elementType = ElementType()
  }

  val io = IO(new Bundle {
    val en = Input(Bool())
    val start = Input(Bool())
    val maxRowAddr = Input(UInt(params.maxRowAddrWidth.W))
    val maxColAddr = Input(UInt(params.maxColAddrWidth.W))
    val innerDim = Input(Dimension())
    val maxElementIdx = Input(UInt(log2Ceil(params.elementTableLength).W))
    val seed = Input(UInt(params.seedWidth.W))
    val patternTable =
      Input(Vec(params.patternTableLength, UInt(params.dataWidth.W)))
    val elementSequence = Input(Vec(params.elementTableLength, new Element()))
    val cycleLimit = Input(UInt(32.W))
    val sramEn = Output(Bool())
    val sramWen = Output(Bool())
    val row = Output(UInt(params.maxRowAddrWidth.W))
    val col = Output(UInt(params.maxColAddrWidth.W))
    val data = Output(UInt(params.dataWidth.W))
    val mask = Output(UInt(params.dataWidth.W))
    val checkEn = Output(Bool())
    val cycle = Output(UInt(32.W))
    val done = Output(Bool())
    val resetHash = Output(Bool())
  })

  val currElement = Wire(new Element)
  val currOperation = Wire(new Operation)
  val currOperationElement = Wire(new OperationElement)
  val currWaitElement = Wire(new WaitElement)
  val checkEn = Wire(Bool())
  val sramEn = Wire(Bool())
  val opsDone = Wire(Bool())
  val up = Wire(Bool())
  val rowsDone = Wire(Bool())
  val colsDone = Wire(Bool())
  val addrDone = Wire(Bool())
  val randAddrOrder = Wire(Bool())
  val sramWen = Wire(Bool())
  val randData = Wire(UInt(params.dataWidth.W))
  val randMask = Wire(UInt(params.dataWidth.W))
  val randRow = Wire(UInt(params.maxRowAddrWidth.W))
  val randCol = Wire(UInt(params.maxColAddrWidth.W))
  val randWen = Wire(Bool())
  val dataPatternEntry = Wire(UInt(params.dataWidth.W))
  val detData = Wire(UInt(params.dataWidth.W))

  val lfsr = Module(new MaxPeriodFibonacciLFSR(params.seedWidth))
  lfsr.io.seed.bits := io.seed.asBools
  lfsr.io.seed.valid := io.start
  lfsr.io.increment := true.B
  val rand = lfsr.io.out.asUInt
  randData := rand(params.dataWidth - 1, 0)
  randMask := rand(2 * params.dataWidth - 1, params.dataWidth)
  randRow := rand(
    2 * params.dataWidth + params.maxRowAddrWidth - 1,
    2 * params.dataWidth
  )
  randCol := rand(
    2 * params.dataWidth + params.maxRowAddrWidth + params.maxColAddrWidth - 1,
    2 * params.dataWidth + params.maxRowAddrWidth
  )
  randWen := rand(
    2 * params.dataWidth + params.maxRowAddrWidth + params.maxColAddrWidth
  )

  val rowCounter = RegInit(0.asUInt(params.maxRowAddrWidth.W))
  val colCounter = RegInit(0.asUInt(params.maxColAddrWidth.W))
  // Used for random addresses only
  val addrCounter = RegInit(1.asUInt(params.randAddrWidth.W))
  // Counts the number of cycles passed when executing a wait element
  val waitCycleCounter = RegInit(1.asUInt(params.randAddrWidth.W))
  val elementIndex = RegInit(0.asUInt(log2Ceil(params.elementTableLength).W))
  val opIndex = RegInit(0.asUInt(log2Ceil(params.operationsPerElement).W))
  val done = RegInit(false.B)
  val cycle = RegInit(1.U(32.W))

  currElement := io.elementSequence(elementIndex)
  currOperationElement := currElement.operationElement
  currWaitElement := currElement.waitElement
  currOperation := currOperationElement.operations(opIndex)
  opsDone := opIndex === currOperationElement.maxIdx
  up := currOperationElement.dir === Direction.up
  randAddrOrder := currOperationElement.dir === Direction.rand
  checkEn := currOperation.operationType === OperationType.read && !currOperation.randData && currElement.elementType === ElementType.rwOp

  dataPatternEntry := io.patternTable(currOperation.dataPatternIdx)
  detData := Mux(
    currOperation.flipped === FlipType.flipped,
    ~dataPatternEntry,
    dataPatternEntry
  )
  val detMask = io.patternTable(currOperation.maskPatternIdx)

  val upNext = Mux(
    elementIndex === io.maxElementIdx,
    true.B,
    io.elementSequence(elementIndex + 1.U).operationElement.dir === Direction.up
  )
  val rowEnd = Mux(up, io.maxRowAddr, 0.U)
  val colEnd = Mux(up, io.maxColAddr, 0.U)
  val rowStart = Mux(up, 0.U, io.maxRowAddr)
  val colStart = Mux(up, 0.U, io.maxColAddr)
  val rowStartNext = Mux(upNext, 0.U, io.maxRowAddr)
  val colStartNext = Mux(upNext, 0.U, io.maxColAddr)
  val rowNext = Mux(up, rowCounter + 1.U, rowCounter - 1.U)
  val colNext = Mux(up, colCounter + 1.U, colCounter - 1.U)
  rowsDone := rowCounter === rowEnd
  colsDone := colCounter === colEnd
  addrDone := Mux(
    randAddrOrder,
    addrCounter === currOperationElement.numAddrs,
    rowsDone && colsDone
  )

  sramEn := !done && io.en && currElement.elementType === ElementType.rwOp

  // TODO wrong for random ops
  sramWen := !done && io.en && (currOperation.operationType === OperationType.write || (currOperation.operationType === OperationType.rand && randWen))

  when(!done) {
    when(io.en && currElement.elementType === ElementType.rwOp) {
      opIndex := opIndex + 1.U
    }
    when(io.en && currElement.elementType === ElementType.waitOp) {
      waitCycleCounter := waitCycleCounter + 1.U
    }
  }

  when(io.en && !done && (cycle < io.cycleLimit || io.cycleLimit === 0.U)) {
    cycle := cycle + 1.U
  }

  when(cycle === io.cycleLimit && io.cycleLimit =/= 0.U) {
    done := true.B
  }

  when(
    io.en && waitCycleCounter === currWaitElement.cyclesToWait && currElement.elementType === ElementType.waitOp
  ) {
    when(elementIndex === io.maxElementIdx) {
      done := true.B
    }.otherwise {
      elementIndex := elementIndex + 1.U
      addrCounter := 1.U
      waitCycleCounter := 1.U
      rowCounter := rowStartNext
      colCounter := colStartNext
    }
  }

  when(
    io.en && !done && opsDone && currElement.elementType === ElementType.rwOp
  ) {
    opIndex := 0.U
    addrCounter := addrCounter + 1.U
    when(io.innerDim === Dimension.row) {
      rowCounter := rowNext
    }.otherwise {
      colCounter := colNext
    }

    when(addrDone) {
      when(elementIndex === io.maxElementIdx) {
        done := true.B
        // io.sramEn := false.B
        // io.sramWen := false.B
      }.otherwise {
        elementIndex := elementIndex + 1.U
        addrCounter := 1.U
        waitCycleCounter := 1.U
      }
      rowCounter := rowStartNext
      colCounter := colStartNext
    }

    when(!addrDone && rowsDone && io.innerDim === Dimension.row) {
      rowCounter := rowStart
      colCounter := colNext
    }

    when(!addrDone && colsDone && io.innerDim === Dimension.col) {
      colCounter := colStart
      rowCounter := rowNext
    }
  }

  when(io.start) {
    when(io.elementSequence(0.U).operationElement.dir === Direction.up) {
      rowCounter := 0.U
      colCounter := 0.U
    }.otherwise {
      rowCounter := io.maxRowAddr
      colCounter := io.maxColAddr
    }
    addrCounter := 1.U
    elementIndex := 0.U
    opIndex := 0.U
    done := false.B
    cycle := 1.U
  }

  io.row := Mux(
    currOperationElement.dir === Direction.rand,
    randRow,
    rowCounter
  )
  io.col := Mux(
    currOperationElement.dir === Direction.rand,
    randCol,
    colCounter
  )
  io.data := Mux(currOperation.randData, randData, detData)
  io.mask := Mux(currOperation.randMask, randMask, detMask)
  io.checkEn := checkEn
  io.cycle := cycle
  io.done := done
  io.resetHash := false.B
  io.sramEn := sramEn
  io.sramWen := sramWen
}
