package chipyard.sky130

import barstools.iocell.chisel.{AnalogIOCell, AnalogIOCellBundle, DigitalGPIOCell, DigitalGPIOCellBundle, DigitalInIOCell, DigitalInIOCellBundle, DigitalOutIOCell, DigitalOutIOCellBundle, IOCell, IOCellTypeParams}
import chipyard.iobinders.{HasIOBinders, IOCellKey}
import chipyard.sky130.util.analog.ConvertAnalog
import chisel3._
import chisel3.experimental.{Analog, BaseModule, attach}
import freechips.rocketchip.diplomacy.{InModuleBody, LazyModule}
import org.chipsalliance.cde.config.Config

class Sky130EFGPIOV2IO extends Bundle {
  // VCCHIB domain
  val OUT = Input(Bool())
  val OE_N = Input(Bool())

  // VCCD domain
  val SLOW = Input(Bool())
  val DM = Input(UInt(3.W))
  val VTRIP_SEL = Input(Bool())
  val INP_DIS = Input(Bool())
  val HLD_OVR = Input(Bool())
  val ENABLE_VDDIO = Input(Bool())
  val IB_MODE_SEL = Input(Bool())
  val ANALOG_EN = Input(Bool())
  val ANALOG_SEL = Input(Bool())
  val ANALOG_POL = Input(Bool())

  val HLD_H_N = Input(Bool())
  val ENABLE_H = Input(Bool())
  val ENABLE_INP_H = Input(Bool())
  val ENABLE_VDDA_H = Input(Bool())
  val ENABLE_VSWITCH_H = Input(Bool())

  // Power
  val VDDIO = Analog(1.W)
  val VDDIO_Q = Analog(1.W)
  val VDDA = Analog(1.W)
  val VCCD = Analog(1.W)
  val VSWITCH = Analog(1.W)
  val VCCHIB = Analog(1.W)
  val VSSA = Analog(1.W)
  val VSSD = Analog(1.W)
  val VSSIO_Q = Analog(1.W)
  val VSSIO = Analog(1.W)

  val PAD = Analog(1.W)

  // Direct pad connections - VDDIO domain
  val PAD_A_NOESD_H = Analog(1.W)
  val PAD_A_ESD_0_H = Analog(1.W)
  val PAD_A_ESD_1_H = Analog(1.W)
  val AMUXBUS_A = Analog(1.W)
  val AMUXBUS_B = Analog(1.W)

  // VCCHIB domain
  val IN = Output(Bool())
  // VDDIO domain
  val IN_H = Output(Bool())

  // special nets used to tie off certain pins (ENABLE_INP_H)
  // VDDIO domain
  val TIE_HI_ESD = Output(Bool())
  val TIE_LO_ESD = Output(Bool())
}

object consts {
  val defaultCellName = "sky130_ef_io__gpiov2_pad_wrapped"
}

class Sky130EFGPIOV2Cell(cellName: String = consts.defaultCellName) extends BlackBox {
  val io = IO(new Sky130EFGPIOV2IO)

  override val desiredName = cellName
}

class Sky130EFIOCellCommonIO extends Bundle {
  // VDDIO domain
  val porb_h = Input(Bool())
}

trait HasSky130EFIOCellCommonIO {
  this: BaseModule =>
  val commonIO = IO(new Sky130EFIOCellCommonIO)
}

abstract class Sky130EFGPIOV2CellIOCellBase(cellName: String) extends RawModule with HasSky130EFIOCellCommonIO {
  val cell = Module(new Sky130EFGPIOV2Cell(cellName = cellName))

  // special nets
  cell.io.ENABLE_INP_H := cell.io.TIE_LO_ESD // tie - disable input when enable_h low

  // VDDIO domain
  cell.io.HLD_H_N := cell.io.TIE_HI_ESD // stay out of hibernate/hold mode

  // VCCD (core) domain
  cell.io.SLOW := false.B // no slow mode
  cell.io.HLD_OVR := false.B // turn off overide
  cell.io.VTRIP_SEL := false.B // CMOS input signalling not LVTTL
  cell.io.IB_MODE_SEL := false.B // use VDDIO not VCCHIB for pad input signalling
  // FIXME: how to handle? see ENABLE_H
  // enable_vddio=1 implies VCCHIB + HV supplies valid, VCCD (+ LV) control signals valid
  // caravel ties to nearby VCCD supply
  cell.io.ENABLE_VDDIO := true.B // enable HV circuits
  cell.io.ANALOG_EN := false.B // disable analog driver
  cell.io.ANALOG_SEL := false.B // tie off analog AMUXBUS sel for good measure
  cell.io.ANALOG_POL := false.B // tie off analog polarity sel for good measure

  // VDDA domain
  cell.io.ENABLE_VDDA_H := cell.io.TIE_LO_ESD // disable analog supplies to analog block

  // VSWITCH domain
  cell.io.ENABLE_VSWITCH_H := cell.io.TIE_LO_ESD // disable pumped-up VDDA supply


  // VDDIO domain
  cell.io.ENABLE_H := commonIO.porb_h
}

class Sky130EFGPIOV2CellAnalog(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with AnalogIOCell {
  val io = IO(new AnalogIOCellBundle)

  // FIXME: replace with analog pad cell

  attach(io.pad, cell.io.PAD)
  attach(io.core, cell.io.PAD_A_NOESD_H)
  // FIXME: what even should happen here...
  cell.io.DM := "b000".U(3.W)
  cell.io.OUT := false.B
  cell.io.OE_N := true.B
  cell.io.INP_DIS := true.B
}

class Sky130EFGPIOV2CellIO(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalGPIOCell {
  val io = IO(new DigitalGPIOCellBundle)

  attach(io.pad, cell.io.PAD)

  cell.io.DM := "b110".U(3.W)
  cell.io.OUT := io.o
  cell.io.OE_N := !io.oe
  io.i := cell.io.IN
  cell.io.INP_DIS := !io.ie
}

class Sky130EFGPIOV2CellIn(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalInIOCell {
  val io = IO(new DigitalInIOCellBundle)

  ConvertAnalog.drive(cell.io.PAD, from = io.pad)

  cell.io.DM := "b001".U(3.W)
  cell.io.OUT := false.B
  cell.io.OE_N := true.B
  io.i := cell.io.IN
  cell.io.INP_DIS := !io.ie
}

class Sky130EFGPIOV2CellOut(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalOutIOCell {
  val io = IO(new DigitalOutIOCellBundle)

  io.pad := ConvertAnalog.readFrom(cell.io.PAD)

  cell.io.DM := "b110".U(3.W)
  cell.io.OUT := io.o
  cell.io.OE_N := !io.oe
  cell.io.INP_DIS := true.B
}

class Sky130EFGPIOV2CellNoConn(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with IOCell {
  val io = IO(new Bundle {
    val pad = Analog(1.W)
  })

  attach(io.pad, cell.io.PAD)

  cell.io.DM := "b000".U(3.W)
  cell.io.OUT := false.B
  cell.io.OE_N := true.B
  cell.io.INP_DIS := true.B
}

case class Sky130EFIOCellTypeParams(cellName: String = consts.defaultCellName)
  extends IOCellTypeParams {
  def analog() = Module(new Sky130EFGPIOV2CellAnalog(cellName = cellName))

  def gpio() = Module(new Sky130EFGPIOV2CellIO(cellName = cellName))

  def input() = Module(new Sky130EFGPIOV2CellIn(cellName = cellName))

  def output() = Module(new Sky130EFGPIOV2CellOut(cellName = cellName))

  // TODO: reset pad
}

/**
 * Use Sky130 gpiov2 IO cells
 *
 * @param cellName name of gpiov2 cell to instantiate
 */
class WithSky130EFIOCells(cellName: String = consts.defaultCellName) extends Config((site, here, up) => {
  case IOCellKey => Sky130EFIOCellTypeParams(cellName = cellName)
})

trait HasSky130EFIOCells {
  this: LazyModule with HasSky130EFCaravelPOR =>

  def registerSky130EFIOCell(cell: HasSky130EFIOCellCommonIO): Unit = {
    cell.commonIO.porb_h := porb_h.getWrappedValue
  }

  InModuleBody {
    this match {
      case top: HasIOBinders =>
        top.iocells.getWrappedValue.foreach {
          case cell: HasSky130EFIOCellCommonIO => registerSky130EFIOCell(cell)
          case _ =>
        }
    }
  }
}
