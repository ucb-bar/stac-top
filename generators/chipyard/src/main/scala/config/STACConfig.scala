package chipyard

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.diplomacy._

class STACConfig extends Config(
  //==================================
  // Set up Sky130 IO
  //==================================
  new chipyard.sky130.WithSky130EFIOCells ++
  new chipyard.sky130.WithSky130EFIOTotalCells(45) ++
  new chipyard.sky130.WithSky130ChipTop ++

  new STACDigitalConfig())

class STACDigitalConfig extends Config(
  //==================================
  // SRAM test payload configuration
  //==================================
  new srambist.WithSramBist(srambist.SramBistParams()) ++ // add SRAM BIST peripheral
  new chipyard.iobinders.WithSramBistIOCells ++
  new chipyard.harness.WithSramBistTiedToMMIOMode ++

  //==================================
  // Set up tiles
  //==================================
  new freechips.rocketchip.subsystem.WithL1ICacheSets(64) ++ // 64 sets, 1 way, 4K cache
  new freechips.rocketchip.subsystem.WithL1ICacheWays(1) ++
  new freechips.rocketchip.subsystem.WithL1DCacheSets(64) ++ // 64 sets, 1 way, 4K cache
  new freechips.rocketchip.subsystem.WithL1DCacheWays(1) ++
  new freechips.rocketchip.subsystem.WithMTE(mteRegions = List(
    /* Scratchpad */
    freechips.rocketchip.rocket.MTERegion(
      base = 0x08000000L,
      size = ((4<<10)-1),
    ),
    /* PSRAM */
    freechips.rocketchip.rocket.MTERegion(
      base = 0x20000000L,
      size = 0x10000000L,
    ),
    /* TSI */
    freechips.rocketchip.rocket.MTERegion(
      base = 0x80000000L,
      size = (1 << 30) * 1L,
    )
  )) ++

  new freechips.rocketchip.subsystem.WithNSmallCores(1) ++ // single small rocket-core

  new testchipip.WithBackingScratchpad(base = 0x08000000, mask = ((4<<10)-1)) ++ // 4 KB

  //==================================
  // Set up I/O
  //==================================
  new testchipip.WithSerialTLWidth(1) ++
  // No AXI backing memory in sim since that causes AMBA prot fields to stay in
  // the main design, which the serdes do not support.
  new chipyard.config.WithSerialTLBackingMemory ++                                      // Backing memory is over serial TL protocol
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 1L) ++                  // 1GB max external memory

  /* 1x UART */
  new chipyard.config.WithUARTOverride(address = 0x10020000, baudrate = 115200) ++

  /* XIP SPI with 194's PSRAM changes */
  new chipyard.harness.WithSimSPIFlashModel(rdOnly = false) ++ // add the SPI flash model in the harness (writeable)
  new chipyard.config.WithSPIFlash(address = 0x10021000, fAddress = 0x20000000, size = 0x10000000) ++ // add the SPI psram controller (1 MiB)

  /* boot_sel=0: hang/wait for TSI. boot_sel=1: self-boot (priority bootloader, try SPI first then BEBE) */
  new testchipip.WithCustomBootPinAltAddr(address = 0x000100c0) ++

  /* JTAG is in the default config */

  //==================================
  // Set up clock./reset
  //==================================
  new chipyard.clocking.WithPLLSelectorDividerClockGenerator ++   // Use a PLL-based clock selector/divider generator structure
  new chipyard.WithAbsoluteFreqHarnessClockInstantiator ++        // use absolute frequencies for simulations in the harness
                                                                  // NOTE: This only simulates properly in VCS

  // Create two clock groups, uncore and fbus, in addition to the tile clock groups
  new chipyard.clocking.WithClockGroupsCombinedByName("uncore", "implicit", "sbus", "mbus", "cbus", "system_bus") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("fbus", "fbus", "pbus") ++

  // Set up the crossings
  new chipyard.config.WithFbusToSbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between SBUS and FBUS
  new chipyard.config.WithCbusToPbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossing between PBUS and CBUS
  new chipyard.config.WithSbusToMbusCrossingType(AsynchronousCrossing()) ++  // Add Async crossings between backside of L2 and MBUS
  new testchipip.WithAsynchronousSerialSlaveCrossing ++                      // Add Async crossing between serial and MBUS. Its master-side is tied to the FBUS


  new freechips.rocketchip.subsystem.WithNBanks(1) ++              // one bank
  new chipyard.config.WithBroadcastManager ++                      // Replace L2 with a broadcast hub for coherence
  new freechips.rocketchip.subsystem.WithBufferlessBroadcastHub ++ // Remove buffers from broadcast manager
  new freechips.rocketchip.subsystem.WithCoherentBusTopology ++    // use coherent bus topology

  new chipyard.config.AbstractConfig)
