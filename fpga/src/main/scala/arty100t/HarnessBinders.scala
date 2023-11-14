package chipyard.fpga.arty100t

import chisel3._

import freechips.rocketchip.jtag.{JTAGIO}
import freechips.rocketchip.subsystem.{PeripheryBusKey}
import freechips.rocketchip.tilelink.{TLBundle}
import freechips.rocketchip.util.{HeterogeneousBag}

import sifive.blocks.devices.uart.{UARTPortIO, HasPeripheryUARTModuleImp, UARTParams}
import sifive.blocks.devices.jtag.{JTAGPins, JTAGPinsFromPort}
import sifive.blocks.devices.pinctrl.{BasePin}

import sifive.fpgashells.ip.xilinx.{IBUFG, IOBUF, PULLUP, PowerOnResetFPGAOnly}

import chipyard._
import chipyard.harness._
import chipyard.iobinders.JTAGChipIO

import testchipip._

class WithArty100TUARTTSI(uartBaudRate: BigInt = 115200) extends OverrideHarnessBinder({
  (system: CanHavePeripheryTLSerial, th: HasHarnessSignalReferences, ports: Seq[ClockedIO[SerialIO]]) => {
    implicit val p = chipyard.iobinders.GetSystemParameters(system)
    ports.map({ port =>
      val ath = th.asInstanceOf[Arty100THarness]
      val freq = p(PeripheryBusKey).dtsFrequency.get
      val bits = SerialAdapter.asyncQueue(port, th.buildtopClock, th.buildtopReset)
      withClockAndReset(th.buildtopClock, th.buildtopReset) {
        val ram = SerialAdapter.connectHarnessRAM(system.serdesser.get, bits, th.buildtopReset)
        val uart_to_serial = Module(new UARTToSerial(
          freq, UARTParams(0, initBaudRate=uartBaudRate)))
        val serial_width_adapter = Module(new SerialWidthAdapter(
          narrowW = 8, wideW = SerialAdapter.SERIAL_TSI_WIDTH))
        serial_width_adapter.io.narrow.flipConnect(uart_to_serial.io.serial)

        ram.module.io.tsi_ser.flipConnect(serial_width_adapter.io.wide)

        ath.io_uart_bb.bundle <> uart_to_serial.io.uart
        ath.other_leds(1) := uart_to_serial.io.dropped

        ath.other_leds(9) := ram.module.io.adapter_state(0)
        ath.other_leds(10) := ram.module.io.adapter_state(1)
        ath.other_leds(11) := ram.module.io.adapter_state(2)
        ath.other_leds(12) := ram.module.io.adapter_state(3)
      }
    })
  }
})

class WithArty100TDDRTL extends OverrideHarnessBinder({
  (system: CanHaveMasterTLMemPort, th: HasHarnessSignalReferences, ports: Seq[HeterogeneousBag[TLBundle]]) => {
    require(ports.size == 1)
    val artyTh = th.asInstanceOf[Arty100THarness]
    val bundles = artyTh.ddrClient.out.map(_._1)
    val ddrClientBundle = Wire(new HeterogeneousBag(bundles.map(_.cloneType)))
    bundles.zip(ddrClientBundle).foreach { case (bundle, io) => bundle <> io }
    ddrClientBundle <> ports.head
  }
})

// Uses PMOD JA/JB
class WithArty100TSerialTLToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: SerialTLPort) => {
    val artyTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[Arty100THarness]
    val harnessIO = IO(port.io.cloneType).suggestName("serial_tl")
    harnessIO <> port.io
    val clkIO = IOPin(harnessIO.clock)
    val packagePinsWithPackageIOs = Seq(
      ("G13", clkIO),
      ("B11", IOPin(harnessIO.bits.out.valid)),
      ("A11", IOPin(harnessIO.bits.out.ready)),
      ("D12", IOPin(harnessIO.bits.in.valid)),
      ("D13", IOPin(harnessIO.bits.in.ready)),
      ("B18", IOPin(harnessIO.bits.out.bits, 0)),
      ("A18", IOPin(harnessIO.bits.out.bits, 1)),
      ("K16", IOPin(harnessIO.bits.out.bits, 2)),
      ("E15", IOPin(harnessIO.bits.out.bits, 3)),
      ("E16", IOPin(harnessIO.bits.in.bits, 0)),
      ("D15", IOPin(harnessIO.bits.in.bits, 1)),
      ("C15", IOPin(harnessIO.bits.in.bits, 2)),
      ("J17", IOPin(harnessIO.bits.in.bits, 3))
    )
    packagePinsWithPackageIOs foreach { case (pin, io) => {
      artyTh.xdc.addPackagePin(io, pin)
      artyTh.xdc.addIOStandard(io, "LVCMOS33")
    }}

    // Don't add IOB to the clock, if its an input
    if (DataMirror.directionOf(port.io.clock) == Direction.Input) {
      packagePinsWithPackageIOs foreach { case (pin, io) => {
        artyTh.xdc.addIOB(io)
      }}
    }

    artyTh.sdc.addClock("ser_tl_clock", clkIO, 100)
    artyTh.sdc.addGroup(pins = Seq(clkIO))
    artyTh.xdc.clockDedicatedRouteFalse(clkIO)
  }
})


class WithArty100TStacControllerToGPIO extends HarnessBinder({
  case (th: HasHarnessInstantiators, port: StacControllerTopIO) => {
    val artyTh = th.asInstanceOf[LazyRawModuleImp].wrapper.asInstanceOf[Arty100THarness]
    val harnessIO = IO(port.io.cloneType).suggestName("stac_controller")
    harnessIO <> port.io
    val packagePinsWithPackageIOs = Seq(
      ("J18", IOPin(harnessIO.sramExtEn)),
      ("K15", IOPin(harnessIO.sramScanMode)),
      ("J15", IOPin(harnessIO.sramScanIn)),
      ("U12", IOPin(harnessIO.sramScanEn)),
      ("V12", IOPin(harnessIO.sramBistEn)),
      ("V10", IOPin(harnessIO.sramBistStart)),
      ("V11", IOPin(harnessIO.clkSel)),
      ("U14", IOPin(harnessIO.pllSel)),
      ("V14", IOPin(harnessIO.pllScanEn)),
      ("T13", IOPin(harnessIO.pllScanRst)),
      ("U13", IOPin(harnessIO.pllScanClk)),
      ("D4", IOPin(harnessIO.pllScanIn)),
      ("D3", IOPin(harnessIO.pllArstb))
      ("F4", IOPin(harnessIO.customBoot))
      ("F3", IOPin(harnessIO.sramScanOut))
      ("E2", IOPin(harnessIO.sramBistDone))
      ("D2", IOPin(harnessIO.pllScanOut))
    )
    packagePinsWithPackageIOs foreach { case (pin, io) => {
      artyTh.xdc.addPackagePin(io, pin)
      artyTh.xdc.addIOStandard(io, "LVCMOS33")
    }}

    packagePinsWithPackageIOs foreach { case (pin, io) => {
      artyTh.xdc.addIOB(io)
    }}

  }
})
