package chipyard.sky130.util.analog

import chisel3._
import chisel3.experimental.{Analog, attach}
import chisel3.internal.firrtl.Width
import chisel3.util.HasBlackBoxInline

class AnalogDriver(width: Width) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val ana = Analog(width)
    val in = Input(Bits(width))
  })

  setInline("AnalogDriver.v",
    s"""
      |module AnalogDriver(
      |  inout [${width.get - 1}:0] ana,
      |  input in
      |);
      |  assign ana = in;
      |endmodule
      |""".stripMargin)
}

class AnalogReader(width: Width) extends BlackBox with HasBlackBoxInline {
  val io = IO(new Bundle {
    val ana = Analog(width)
    val out = Output(Bits(width))
  })

  setInline("AnalogReader.v",
    s"""
       |module AnalogReader(
       |  inout [${width.get - 1}:0] ana,
       |  output out
       |);
       |  assign out = ana;
       |endmodule
       |""".stripMargin)
}

object ConvertAnalog {
  def readFrom(ana: Analog): Bits = {
    val reader = Module(new AnalogReader(ana.getWidth.W))
    attach(ana, reader.io.ana)
    reader.io.out
  }

  def driveFrom(in: Bits): Analog = {
    val driver = Module(new AnalogDriver(in.getWidth.W))
    driver.io.in := in
    driver.io.ana
  }

  def drive(ana: Analog, from: Bits): Unit = {
    require(ana.getWidth == from.getWidth)

    val _driver = Module(new AnalogDriver(ana.getWidth.W))

    _driver.io.in := from
    attach(_driver.io.ana, ana)
  }
}
