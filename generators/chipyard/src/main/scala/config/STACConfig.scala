package chipyard

import org.chipsalliance.cde.config.Config
import freechips.rocketchip.diplomacy._
import freechips.rocketchip.rocket.{DCacheParams, ICacheParams, MulDivParams, RocketCoreParams}
import freechips.rocketchip.subsystem.{CacheBlockBytes, InSubsystem, RocketCrossingParams, RocketTileAttachParams, SystemBusKey, TileMasterPortParams, TilesLocated}
import freechips.rocketchip.tile.{RocketTileParams, XLen}

private class With1TinyCoreNoSPad extends Config((site, here, up) => {
  case XLen => 32
  case TilesLocated(InSubsystem) => {
    val tiny = RocketTileParams(
      core = RocketCoreParams(
        useVM = false,
        fpu = None,
        mulDiv = Some(MulDivParams(mulUnroll = 8))),
      btb = None,
      dcache = Some(DCacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = 64, // 4KB
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        nMSHRs = 0,
        blockBytes = site(CacheBlockBytes),
        scratch = None)),
      icache = Some(ICacheParams(
        rowBits = site(SystemBusKey).beatBits,
        nSets = 64,
        nWays = 1,
        nTLBSets = 1,
        nTLBWays = 4,
        blockBytes = site(CacheBlockBytes)))
    )
    List(RocketTileAttachParams(
      tiny,
      RocketCrossingParams(
        crossingType = SynchronousCrossing(),
        master = TileMasterPortParams())
    ))
  }
})

class STACConfig extends Config(
  //==================================
  // Set up TestHarness
  //==================================
  new chipyard.WithAbsoluteFreqHarnessClockInstantiator ++ // use absolute frequencies for simulations in the harness
                                                           // NOTE: This only simulates properly in VCS

  //==================================
  // Set up I/O
  //==================================
  new testchipip.WithSerialTLWidth(1) ++
  new chipyard.harness.WithSimAXIMemOverSerialTL ++                                     // Attach fast SimDRAM to TestHarness
  new chipyard.config.WithSerialTLBackingMemory ++                                      // Backing memory is over serial TL protocol
  new freechips.rocketchip.subsystem.WithExtMemSize((1 << 30) * 2L) ++                  // 2GB max external memory

  //==================================
  // Set up tiles
  //==================================
  new With1TinyCoreNoSPad ++                                      // single tiny rocket-core

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
  new testchipip.WithAsynchronousSerialSlaveCrossing ++                      // Add Async crossing between serial and MBUS. Its master-side is tied to the FBUS


  new freechips.rocketchip.subsystem.WithNBanks(1) ++           // one bank
  new chipyard.config.WithBroadcastManager ++                   // use broadcast manager
  new freechips.rocketchip.subsystem.WithCoherentBusTopology ++ // use coherent bus topology

  new chipyard.config.AbstractConfig)
