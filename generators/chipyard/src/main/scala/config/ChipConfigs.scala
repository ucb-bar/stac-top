package chipyard

import org.chipsalliance.cde.config.{Config}
import freechips.rocketchip.diplomacy._

// A simple config demonstrating how to set up a basic chip in Chipyard
class SRAMDigitalConfig extends Config(
  //==================================
  // Set up TestHarness
  //==================================
  new chipyard.WithAbsoluteFreqHarnessClockInstantiator ++ // use absolute frequencies for simulations in the harness
                                                           // NOTE: This only simulates properly in VCS

  //==================================
  // Set up tiles
  //==================================
  // new chipyard.config.WithTLSerialLocation(
  //   freechips.rocketchip.subsystem.FBUS,
  //   freechips.rocketchip.subsystem.PBUS) ++                       // attach TL serial adapter to f/p busses
  // new freechips.rocketchip.subsystem.WithIncoherentBusTopology ++ // use incoherent bus topology
  // new freechips.rocketchip.subsystem.WithNMemoryChannels(0) ++
  new freechips.rocketchip.subsystem.WithNoMemPort ++             // remove backing memory

  // new freechips.rocketchip.subsystem.With1TinyCore(scratchpadBase = None) ++             // single tiny rocket-core
  // new freechips.rocketchip.subsystem.With1TinyCore(scratchpadBase = Some(0x11000000)) ++             // single tiny rocket-core
  // new freechips.rocketchip.subsystem.With1TinyCore(scratchpadBase = Some(0x08000000)) ++             // single tiny rocket-core
  new freechips.rocketchip.subsystem.WithL1DCacheSets(sets = 64) ++ // 64 sets, 4K cache
  new freechips.rocketchip.subsystem.With1TinyCore(scratchpadBase = None) ++             // single tiny rocket-core

  new chipyard.config.WithBroadcastManager ++ // Replace L2 with a broadcast hub for coherence
  new freechips.rocketchip.subsystem.WithBufferlessBroadcastHub() ++ // Remove buffers from broadcast manager

  new testchipip.WithBackingScratchpad(base = 0x08000000, mask = ((4<<10)-1)) ++ // 4 KB

  //==================================
  // Set up I/O
  //==================================
  /* 1GB TSI region, 1ch */
  new testchipip.WithSerialTLWidth(1) ++
  new chipyard.config.WithSerialTLBackingMemory ++                                      // Backing memory is over serial TL protocol
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 1L) ++                  // 1GB max external memory

  /* 1x UART */
  new chipyard.config.WithUARTOverride(address = 0x10020000, baudrate = 115200) ++
  
  /* XIP SPI with 194's PSRAM changes */
  new chipyard.harness.WithSimSPIFlashModel(true) ++       // add the SPI flash model in the harness (writeable)  
  new chipyard.config.WithSPIFlash(address = 0x10021000, fAddress = 0x20000000, size = 0x10000000) ++             // add the SPI psram controller (1 MiB)

  /* boot_sel=0: hang/wait for TSI. boot_sel=1: self-boot (priority bootloader, try SPI first then BEBE) */
  new testchipip.WithCustomBootPinAltAddr(address = 0x000100c0) ++

  /* JTAG is in the default config */

  //==================================
  // Set up clock./reset
  //==================================
  new chipyard.clocking.WithPLLSelectorDividerClockGenerator ++   // Use a PLL-based clock selector/divider generator structure

  // Create two clock groups, uncore and fbus, in addition to the tile clock groups
  new chipyard.clocking.WithClockGroupsCombinedByName("uncore", "implicit", "sbus", "mbus", "cbus", "system_bus") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("fbus", "fbus", "pbus") ++

  // Set up the crossings
  new chipyard.config.WithFbusToSbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between SBUS and FBUS
  new chipyard.config.WithCbusToPbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between PBUS and CBUS
  new chipyard.config.WithSbusToMbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossings between backside of L2 and MBUS

  new chipyard.config.AbstractConfig)

