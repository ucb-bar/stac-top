# STAC Test Plan

See the [chip docs](./test_chip.md) for information on what's on the chip
and the modes of testing the SRAMs, including a high-level description of
possible test sequences.
This document will focus on board-level testing concerns.

## Power

A 6V external power supply is fed to an over/under voltage protection IC.
A power on button is fed to this IC. The IC debounces the button
and turns on the output power when the button is pressed.

A first stage LDO regulates the 6V supply to 5V.
One second stage LDO regulates the 5V LDO output to 3.3V for VDDIO;
another LDO regulates the 5V output to 1.8V for VDDD.

If we wish to bypass the LDOs or test the chip under different voltages,
power can also be supplied via a dedicated header. Two independent switches
(one for VDDD, one for VDDIO)
select between the on-board regulated power and the external power header.

## Clocking

The digital clock (`clock_clock`) and the SRAM sense amp clock (`SRAM_SAE_CLK`)
are provided to the board using SMA connectors.
The SKY130 GPIOs claim to have a maximum frequency of ~66 MHz, so if the on-chip
PLL does not work, the maximum testable clock frequency will be 66 MHz.

## Digital signals

All SRAM and Rocket digital signals will be broken out to 0.1" headers
compatible with the Arty A7-100T FPGA platform.

The 7 serial TileLink signals will be routed to a 12-pin PMOD connector,
which can also plug in the one of the PMOD slots on the Arty A7-100T.

The FPGA will be used for all digital testing, including application
of test stimuli. The Arty PLL can generate at least an 800MHz clock,
which should be sufficiently oversampled for our < 100 MHz expected test frequency.

In the event that the our on-chip PLL works and we are able to run
SRAM test sequences at higher frequencies,
we can still use the FPGA to read out the BIST checksum via the scan chain.
This scan chain readout need not be particularly fast.

