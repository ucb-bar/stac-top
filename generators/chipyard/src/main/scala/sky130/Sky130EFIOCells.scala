package chipyard.sky130

import barstools.iocell.chisel.{AnalogIOCell, AnalogIOCellBundle, DigitalGPIOCell, DigitalGPIOCellBundle, DigitalInIOCell, DigitalInIOCellBundle, DigitalOutIOCell, DigitalOutIOCellBundle, IOCell, IOCellTypeParams}
import chipyard.iobinders.{HasIOBinders, IOCellKey}
import chipyard.sky130.util.analog.ConvertAnalog
import chisel3._
import chisel3.experimental.{Analog, BaseModule, attach}
import freechips.rocketchip.diplomacy.{InModuleBody, LazyModule}
import freechips.rocketchip.util.ElaborationArtefacts
import org.chipsalliance.cde.config.Config

import scala.collection.mutable

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

trait Sky130EFIOCellLike extends IOCell {
  this: BaseModule =>
  val commonIO = IO(new Sky130EFIOCellCommonIO)
}

abstract class Sky130EFGPIOV2CellIOCellBase(cellName: String) extends RawModule with Sky130EFIOCellLike {
  val iocell = Module(new Sky130EFGPIOV2Cell(cellName = cellName))

  // special nets
  iocell.io.ENABLE_INP_H := iocell.io.TIE_LO_ESD // tie - disable input when enable_h low

  // VDDIO domain
  iocell.io.HLD_H_N := iocell.io.TIE_HI_ESD // stay out of hibernate/hold mode

  // VCCD (core) domain
  iocell.io.SLOW := false.B // no slow mode
  iocell.io.HLD_OVR := false.B // turn off overide
  iocell.io.VTRIP_SEL := false.B // CMOS input signalling not LVTTL
  iocell.io.IB_MODE_SEL := false.B // use VDDIO not VCCHIB for pad input signalling
  // FIXME: how to handle? see ENABLE_H
  // enable_vddio=1 implies VCCHIB + HV supplies valid, VCCD (+ LV) control signals valid
  // caravel ties to nearby VCCD supply
  iocell.io.ENABLE_VDDIO := true.B // enable HV circuits
  iocell.io.ANALOG_EN := false.B // disable analog driver
  iocell.io.ANALOG_SEL := false.B // tie off analog AMUXBUS sel for good measure
  iocell.io.ANALOG_POL := false.B // tie off analog polarity sel for good measure

  // VDDA domain
  iocell.io.ENABLE_VDDA_H := iocell.io.TIE_LO_ESD // disable analog supplies to analog block

  // VSWITCH domain
  iocell.io.ENABLE_VSWITCH_H := iocell.io.TIE_LO_ESD // disable pumped-up VDDA supply


  // VDDIO domain
  iocell.io.ENABLE_H := commonIO.porb_h
}

class Sky130EFGPIOV2CellAnalog(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with AnalogIOCell {
  val io = IO(new AnalogIOCellBundle)

  // FIXME: replace with analog pad cell

  attach(io.pad, iocell.io.PAD)
  attach(io.core, iocell.io.PAD_A_NOESD_H)
  // FIXME: what even should happen here...
  iocell.io.DM := "b000".U(3.W)
  iocell.io.OUT := false.B
  iocell.io.OE_N := true.B
  iocell.io.INP_DIS := true.B
}

class Sky130EFGPIOV2CellIO(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalGPIOCell {
  val io = IO(new DigitalGPIOCellBundle)

  attach(io.pad, iocell.io.PAD)

  iocell.io.DM := "b110".U(3.W)
  iocell.io.OUT := io.o
  iocell.io.OE_N := !io.oe
  io.i := iocell.io.IN
  iocell.io.INP_DIS := !io.ie
}

class Sky130EFGPIOV2CellIn(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalInIOCell {
  val io = IO(new DigitalInIOCellBundle)

  ConvertAnalog.drive(iocell.io.PAD, from = io.pad)

  iocell.io.DM := "b001".U(3.W)
  iocell.io.OUT := false.B
  iocell.io.OE_N := true.B
  io.i := iocell.io.IN
  iocell.io.INP_DIS := !io.ie
}

class Sky130EFGPIOV2CellOut(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with DigitalOutIOCell {
  val io = IO(new DigitalOutIOCellBundle)

  io.pad := ConvertAnalog.readFrom(iocell.io.PAD)

  iocell.io.DM := "b110".U(3.W)
  iocell.io.OUT := io.o
  iocell.io.OE_N := !io.oe
  iocell.io.INP_DIS := true.B
}

class Sky130EFGPIOV2CellNoConn(cellName: String = consts.defaultCellName)
  extends Sky130EFGPIOV2CellIOCellBase(cellName) with IOCell {
  val io = IO(new Bundle {
    val pad = Analog(1.W)
  })

  attach(io.pad, iocell.io.PAD)

  iocell.io.DM := "b000".U(3.W)
  iocell.io.OUT := false.B
  iocell.io.OE_N := true.B
  iocell.io.INP_DIS := true.B
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

  val sky130EFIOCellInsts: mutable.Buffer[Sky130EFIOCellLike] = mutable.Buffer[Sky130EFIOCellLike]()

  def registerSky130EFIOCell(cell: Sky130EFIOCellLike): Unit = {
    cell.commonIO.porb_h := porb_h.getWrappedValue

    sky130EFIOCellInsts.append(cell)
  }

  InModuleBody {
    this match {
      case top: HasIOBinders =>
        top.iocells.getWrappedValue.foreach {
          case cell: Sky130EFIOCellLike => registerSky130EFIOCell(cell)
          case _ =>
        }
    }
  }

  ElaborationArtefacts.add("sky130io.json", {
    "[\n" + sky130EFIOCellInsts.map { cell =>
        s"""
         |  {
         |    "name": ${cell.signalName.map(n => s"\"$n\"").getOrElse("null")}
         |  }
         |""".stripMargin.stripTrailing().substring(1)
    }.mkString(",\n") + "\n]"
  })
}
