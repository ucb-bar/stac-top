package chipyard.sky130

import chisel3._
import freechips.rocketchip.diplomacy.{InModuleBody, LazyModule}

class Sky130EFCaravelPORIO extends Bundle {
  // HV domain
  val porb_h = Output(Bool())

  // LV domain
  val porb_l = Output(Bool())
  val por_l = Output(Bool())
}

class Sky130EFCaravelPOR extends BlackBox {
  val io = IO(new Sky130EFCaravelPORIO)

  override val desiredName = "simple_por"
}

trait HasSky130EFCaravelPOR {
  this: LazyModule =>

  val por = InModuleBody {
    val por = Module(new Sky130EFCaravelPOR)
    por
  }
  val porb_h = InModuleBody {
    val porb_h = Wire(Bool()).suggestName("porb_h")
    porb_h := por.io.porb_h

    dontTouch(porb_h) // this net name used for routing

    porb_h
  }
}
