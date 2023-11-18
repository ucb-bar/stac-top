package chipyard

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.subsystem.SBUS

class STACConfig extends Config(
  //==================================
  // Set up Sky130 IO
  //==================================
  new chipyard.stac.clocking.WithClockAndResetFromHarnessAndMultiPLLTiedOff ++
  new chipyard.stac.clocking.WithMultiPLLClockGenerator ++
  new chipyard.sky130.WithSky130EFIOCells ++
  new chipyard.sky130.WithSky130EFIOTotalCells(45) ++
  new chipyard.sky130.WithSky130ChipTop ++

  new STACDigitalConfig())

class STACDigitalConfig extends Config(
  //==================================
  // SRAM test payload configuration
  //==================================
  new srambist.WithSramBistLocation(SBUS) ++ // See clocking
  new srambist.WithSramBistCrossingType(SynchronousCrossing()) ++
  new srambist.WithSramBist(srambist.SramBistParams()) ++ // add SRAM BIST peripheral
  new chipyard.iobinders.WithSramBistIOCells ++
  new chipyard.harness.WithSramBistTiedToMMIOMode ++

  //==================================
  // Set up tiles
  //==================================
  new freechips.rocketchip.subsystem.WithAsynchronousRocketTiles(depth = 8, sync = 3) ++ // See clocking section

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

  // Clock groups: SBUS (incl. BIST), Rocket, FBUS (incl. TSI), everything else
  new chipyard.clocking.WithClockGroupsCombinedByName("periph", "cbus", "mbus", "pbus", "implicit") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("rocket", "tile_0") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("fbus", "fbus") ++
  new chipyard.clocking.WithClockGroupsCombinedByName("sbus", "sbus") ++

  // Set up the crossings
  new chipyard.config.WithFbusToSbusCrossingType(AsynchronousCrossing()) ++
  new chipyard.config.WithSbusToCbusCrossingType(AsynchronousCrossing()) ++
  new chipyard.config.WithCbusToPbusCrossingType(SynchronousCrossing()) ++
  new chipyard.config.WithSbusToMbusCrossingType(AsynchronousCrossing()) ++
  new testchipip.WithAsynchronousSerialSlaveCrossing ++                      // Add Async crossing between serial and MBUS. Its master-side is tied to the FBUS
  // Async rocket -> above
  // Sync SRAM BIST on SBUS -> above


  new freechips.rocketchip.subsystem.WithNBanks(1) ++              // one bank
  new chipyard.config.WithBroadcastManager ++                      // Replace L2 with a broadcast hub for coherence
  new freechips.rocketchip.subsystem.WithCoherentBusTopology ++    // use coherent bus topology

  new chipyard.config.AbstractConfig)

class STACBringupHostConfig extends Config(
  //=============================
  // Set up TestHarness for standalone-sim
  //=============================
  new chipyard.WithAbsoluteFreqHarnessClockInstantiator ++  // Generate absolute frequencies
  new chipyard.WithSerialTLPunchthrough ++                // Don't generate IOCells for the serial TL (this design maps to FPGA)
  new staccontroller.WithStacControllerLocation(SBUS) ++ // See clocking
  new staccontroller.WithStacControllerCrossingType(SynchronousCrossing()) ++
  new staccontroller.WithStacController(staccontroller.StacControllerParams()) ++ // add SRAM BIST peripheral
  new chipyard.iobinders.WithStacControllerIOCells ++

  //=============================
  // Setup the SerialTL side on the bringup device
  //=============================
  new testchipip.WithSerialTLWidth(1) ++                                       // match width with the chip
  new testchipip.WithSerialTLMem(base = 0x0, size = 0x80000000L,               // accessible memory of the chip that doesn't come from the tethered host
                                 isMainMemory = false) ++          // This assumes off-chip mem starts at 0x8000_0000
  new testchipip.WithSerialTLClockDirection(provideClockFreqMHz = None) ++ // bringup board drives the clock for the serial-tl receiver on the chip, use 75MHz clock

  //============================
  // Setup bus topology on the bringup system
  //============================
  new testchipip.WithOffchipBusClient(SBUS,                                    // offchip bus hangs off the SBUS
    blockRange = AddressSet.misaligned(0x80000000L, (BigInt(1) << 30) * 4)) ++ // offchip bus should not see the main memory of the testchip, since that can be accessed directly
  new testchipip.WithOffchipBus ++                                             // offchip bus

  //=============================
  // Set up memory on the bringup system
  //=============================
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 1L) ++         // match what the chip believes the max size should be

  //=============================
  // Generate the TSI-over-UART side of the bringup system
  //=============================
  new testchipip.WithUARTTSIClient(initBaudRate = BigInt(921600)) ++           // nonstandard baud rate to improve performance

  //=============================
  // Set up clocks of the bringup system
  //=============================
  new chipyard.clocking.WithPassthroughClockGenerator ++ // pass all the clocks through, since this isn't a chip
  new chipyard.config.WithFrontBusFrequency(50.0) ++     // run all buses of this system at 75 MHz
  new chipyard.config.WithMemoryBusFrequency(50.0) ++
  new chipyard.config.WithPeripheryBusFrequency(50.0) ++

  // Base is the no-cores config
  new chipyard.NoCoresConfig)

// DOC include start: TetheredChipLikeRocketConfig
// class TetheredChipLikeRocketConfig extends Config(
//   new chipyard.harness.WithAbsoluteFreqHarnessClockInstantiator ++   // use absolute freqs for sims in the harness
//   new chipyard.harness.WithMultiChipSerialTL(0, 1) ++                // connect the serial-tl ports of the chips together
//   new chipyard.harness.WithMultiChip(0, new STACConfig) ++ // ChipTop0 is the design-to-be-taped-out
//   new chipyard.harness.WithMultiChip(1, new STACBringupHostConfig))  // ChipTop1 is the bringup design
// DOC include end: TetheredChipLikeRocketConfig
