package srambist.programmablebist

import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chisel3.util.random.MaxPeriodFibonacciLFSR

import srambist.ProgrammableBistParams

class ProgrammableBist(params: ProgrammableBistParams) extends Module {

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
    val mask = UInt(log2Ceil(params.patternTableLength).W)
    // Number of random addresses to try.
    // Only used if `dir` is set to `rand`.
    val numAddrs = UInt(14.W)
  }

  class WaitElement extends Bundle {
    val cyclesToWait = UInt(14.W)
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
    val numElements = Input(UInt(log2Ceil(params.elementTableLength).W))
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

  val lfsr = Module(new MaxPeriodFibonacciLFSR(params.seedWidth))
  lfsr.io.seed.bits := io.seed.asBools
  lfsr.io.seed.valid := io.start
  lfsr.io.increment := true.B
  val rand = lfsr.io.out
  // val randAddr = rand(params.dataWidth-1, 0)
  // val randMask = rand(2*params.dataWidth-1, params.dataWidth)

  io.data := 0.U
  io.mask := 0.U
  io.checkEn := false.B
  io.cycle := 0.U
  io.done := false.B
  io.resetHash := false.B

  val row = RegInit(0.asUInt(params.maxRowAddrWidth.W))
  val col = RegInit(0.asUInt(params.maxColAddrWidth.W))
  val elementIndex = RegInit(0.asUInt(log2Ceil(params.elementTableLength).W))
  val opIndex = RegInit(0.asUInt(log2Ceil(params.operationsPerElement).W))
  val inProgress = RegInit(false.B)

  io.row := row
  io.col := col

  val currElement = Wire(new Element)
  val currOperation = Wire(new Operation)
  val currOperationElement = Wire(new OperationElement)
  val opsDone = Wire(Bool())
  val up = Wire(Bool())
  val rowsDone = Wire(Bool())
  val colsDone = Wire(Bool())
  val addrDone = Wire(Bool())

  currElement := io.elementSequence(elementIndex)
  currOperationElement := currElement.operationElement
  currOperation := currOperationElement.operations(opIndex)
  opsDone := opIndex === currOperationElement.maxIdx
  up := currOperationElement.dir === Direction.up

  val rowEnd = Mux(up, io.maxRowAddr, 0.U)
  val colEnd = Mux(up, io.maxColAddr, 0.U)
  val rowStart = Mux(up, 0.U, io.maxRowAddr)
  val colStart = Mux(up, 0.U, io.maxColAddr)
  val rowNext = Mux(up, row + 1.U, row - 1.U)
  val colNext = Mux(up, col + 1.U, col - 1.U)
  rowsDone := row === rowEnd
  colsDone := col === colEnd
  addrDone := rowsDone && colsDone

  io.sramEn := false.B
  io.sramWen := false.B

  when(io.start && io.en) {
    elementIndex := 0.U
    opIndex := 0.U
    inProgress := true.B
  }

  when(inProgress) {
    when(currElement.elementType === ElementType.rwOp) {
      io.sramEn := true.B
      io.sramWen := false.B
      opIndex := opIndex + 1.U
    }
  }

  when(opsDone) {
    opIndex := 0.U
    when(io.innerDim === Dimension.row) {
      row := rowNext
    }
      .otherwise {
        col := colNext
      }
    when(addrDone) {
      elementIndex := elementIndex + 1.U
      // TODO these should be rowStartNext and rowEndNext

    }
    when(!addrDone && rowsDone && io.innerDim === Dimension.row) {
      row := rowStart
    }

    when(!addrDone && colsDone && io.innerDim === Dimension.col) {
      col := colStart
    }
  }

  /** for each element: initialize address for each address: for each operation
    * in element: do operation update address (up/down depending on element)
    */

}
