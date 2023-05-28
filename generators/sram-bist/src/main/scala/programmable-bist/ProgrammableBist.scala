package sramtestunit.programmablebist

import sramtestunit.{ProgrammableBistParams}

class ProgrammableBist(params: ProgrammableBistParams) extends Module {

  val lfsr = LFSR(77, increment = true.B, seed = seed);
  
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

  object Element extends ChiselEnum {
    val operationElement = OperationElement()
    val waitElement = WaitElement()
  }

  object Dimension extends ChiselEnum {
    val row = Value(0.U(1.W))
    val col = Value(1.U(1.W))
  }

  class Operation extends Bundle {
    val operation = OperationType()
    val patternAddress = UInt(log2Ceil(params.patternTableLength).W)
    val flipped = FlipType()
  }
  
  class OperationElement extends Bundle {
    val operations = Vec(params.operationsPerElement, Operation())
    val count = UInt(log2Ceil(params.operationsPerElement).W)
    val direction = Direction()
    val mask = UInt(log2Ceil(params.patternTableLength).W)
  }

  class WaitElement extends Bundle {
    val cyclesToWait = UInt(14.W)
  }

  class Pattern extends Bundle {
    val pattern = UInt(64.W) // todo: what is the best size of the pattern?
  }

  val io = IO(new Bundle {
    val en = Input(Bool())
    val maxRowAddr = Input(UInt(10.W))  
    val maxColAddr = Input(UInt(3.W))  
    val innerDim = Input(Dimension())  
    val count = Input(UInt(log2Ceil(params.elementTableLength).W))  
    val seed = Input(UInt(77.W))  
    val patternTable = Input(Vec(params.patternTableLength, Pattern())) 
    val elementSequence = Input(Vec(params.elementTableLength, Element())) 

    val sramEn = Output(Bool())
    val sramWen = Output(Bool())
    val row = Output(UInt(10.W))
    val col = Output(UInt(3.W))
    val data = Output(64.W)
    val mask = Output(64.W)

  })

}
