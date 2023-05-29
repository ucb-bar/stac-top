package srambist.programmablebist

import srambist.{ProgrammableBistParams}
import chisel3._
import chisel3.ChiselEnum
import chisel3.util.log2Ceil
import chisel3.util.random.FibonacciLFSR

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
    val dataPattern = UInt(log2Ceil(params.patternTableLength).W)
    // Mask pattern address if mask is not randomized.
    val maskPattern = UInt(log2Ceil(params.patternTableLength).W)

    // Bitwise flip data?
    val flipped = FlipType()
  }
  
  class OperationElement extends Bundle {
    val operations = Vec(params.operationsPerElement, new Operation())
    val count = UInt(log2Ceil(params.operationsPerElement).W)
    val dir = Direction()
    val mask = UInt(log2Ceil(params.patternTableLength).W)
    // Number of random addresses to try.
    // Only used if `dir` is set to `rand`.
    val numAddrs = Uint(14.W)
  }

  class WaitElement extends Bundle {
    val cyclesToWait = UInt(14.W)
  }

  class Element extends Bundle {
    val operationElement = new OperationElement()
    val waitElement = new WaitElement()
    val elementType = ElementType()
  }

  class Pattern extends Bundle {
    val pattern = UInt(params.dataWidth.W) // todo: what is the best size of the pattern?
  }

  val io = IO(new Bundle {
    val en = Input(Bool())
    val start = Input(Bool())
    val maxRowAddr = Input(UInt(params.maxRowAddrWidth.W))  
    val maxColAddr = Input(UInt(params.maxColAddrWidth.W))  
    val innerDim = Input(Dimension())  
    val count = Input(UInt(log2Ceil(params.elementTableLength).W))  
    val seed = Input(UInt(params.seedWidth.W))  
    val patternTable = Input(Vec(params.patternTableLength, new Pattern())) 
    val elementSequence = Input(Vec(params.elementTableLength, new Element())) 
    val cycleLimit = Input(UInt(32.W))

    val sramEn = Output(Bool())
    val sramWen = Output(Bool())
    val row = Output(UInt(params.maxRowAddrWidth.W))
    val col = Output(UInt(params.maxColAddrWidth.W))
    val data = Output(UInt(params.dataWidth.W))
    val mask = Output(UInt(params.dataWidth.W))

  })

  val lfsr = Module(new FibonacciLFSR(params.seedWidth, Set(4, 3)))
  lfsr.io.seed := io.seed
  val rand_val = lfsr.io.out
  val elementIndex = RegInit(0.asUInt(log2Ceil(params.elementTableLength).W))
  val opIndex = RegInit(0.asUInt(log2Ceil(params.operationsPerElement).W))
  val currElement = Wire(new Element)
  val currOperation = Wire(new Operation)
  val currOperationElement = Wire(new Operation)
  val inProgress = RegInit(false.B)

  currElement := io.elementSequence(elementIndex)
  currOperationElement := currElement.operationElement
  currOperation := currOperationElement(opIndex)

  io.sramEn := false.B
  io.sramWen := false.B
    
  when (io.start && io.en) {
    elementIndex := 0.U
    opIndex := 0.U
    inProgress := true.B
  }
  
  when (inProgress) {
    when (currElement.elementType == ElementType.operationElement) {
      io.sramEn := true.B
      io.sramWen := currOperation.
      opIndex := opIndex + 1.U
      when (opIndex === currOperationElement.count) {
        // on the next cycle, begin a new operation
        elementIndex := elementIndex + 1.U
        opIndex := 0.U
      }
    }
  }
  

  /** 
   *  for each element:
   *    initialize address
   *    for each address:
   *      for each operation in element:
   *        do operation
   *      update address (up/down depending on element)
  */

}

