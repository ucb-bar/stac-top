# STAC Test Plan

See the [chip docs](./test_chip.md) for information on what's on the chip
and the modes of testing the SRAMs, including a high-level description of
possible test sequences.
This document will focus on board-level testing concerns.

## Power

A 6V external power supply is fed to an over/under voltage protection IC.
A power on button is fed to this IC. The IC debounces the button
and turns on the output power when the button is pressed.

A first stage LDO regulates the 6V supply to 4.66V.
One second stage LDO regulates the 4.66V LDO output to 3.3V for VDDIO;
another LDO regulates the 4.66V output to 1.8V for VDDD.

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

## Rocket

The Rocket core on the STAC chip supports 4 primary protocols:
JTAG, UART, Quad SPI, and Serial TileLink.
The intended usage of each of these interfaces is described below.

### JTAG

[JTAG](https://en.wikipedia.org/wiki/JTAG) is a protocol for
providing debug access to the Rocket core.
It is carried over the following STAC pins:
* `JTAG_TCK`
* `JTAG_TMS`
* `JTAG_TDI`
* `JTAG_TDO`

JTAG allows us to access CPU registers and memory, and allows us to upload small programs for the core to execute.

We feed these JTAG signals to an [FT2232HL](https://www.ftdichip.com/old2020/Products/ICs/FT2232H.html)
chip, which adapts them to USB.
The USB signals can then be connected to a host computer.

The host computer uses the [OpenOCD](https://openocd.org/) software to
drive the JTAG interface over USB. The USB to JTAG conversion is done by the FT2232HL.

We have tested this setup with a STAC chip by jumper wiring from a STAC breakout board
to an FT2232HL breakout board with a USB C connector, and then plugging that USB C port into a laptop.
To reduce jumper wire spaghetti, we plan to place the FT2232HL on the STAC test board,
rather than using it as a separate breakout board.
The USB C connector we plan to use is
Amphenol [12402012E212A](https://www.digikey.com/en/products/detail/amphenol-cs-commercial-products/12402012E212A/13683192).
We will connect the USB-C port on the board to a USB-C port on a laptop using a cable such as
Qualtek [3027007-005M](https://www.digikey.com/en/products/detail/qualtek/3027007-005M/9738749).

Note: JTAG operations require the Rocket core to be able to execute instructions.

### UART

[UART](https://en.wikipedia.org/wiki/Universal_asynchronous_receiver-transmitter) is an
asynchronous serial protocol with a configurable baud rate.
UART requires only two signals:
* `uart_0_txd` (output from the STAC chip)
* `uart_0_rxd` (input to the STAC chip)

Similar to JTAG, we use an FT2232HL to convert UART to USB so that it is convenient
to drive the UART from a laptop.
The FT2232HL supports two channels: we use one channel for JTAG, and the other for UART.
Thus, we only need one FT2232HL IC to speak both protocols.


### Quad SPI

[QSPI](https://infocenter.nordicsemi.com/index.jsp?topic=%2Fps_nrf52840%2Fqspi.html)
is a protocol similar to SPI, except that it can carry 4 bits at a time (rather than
the 1 bit at a time supported by SPI).
It is intended for interfacing with external flash memory.
The STAC chip has one QSPI interface, which uses the following signals:
* `spi_0_dq_0`
* `spi_0_dq_1`
* `spi_0_dq_2`
* `spi_0_dq_3`
* `spi_0_cs_0`
* `spi_0_sck`

These signal names are different from the signal names conventionally used for QSPI;
this is an artifact of how Chisel generates signal names.

We envision two uses of the QSPI interface:
1. Talk to a PSRAM chip. This will be useful for debugging:
   it provides a fallback in case the serial TileLink interface does not work,
   or if the SRAM macros tied to the Rocket do not work. The QSPI interface
   may also be faster than serial TileLink, since QSPI carries 4 bits per cycle,
   whereas serial TileLink only carries 1 bit per cycle.
   The PSRAM chip we are currently considering is the
   [APS6404L-3SQR-SN](https://www.mouser.com/ProductDetail/AP-Memory/APS6404L-3SQR-SN?qs=IS%252B4QmGtzzqsn3S5xo%2FEEg%3D%3D).
2. Talk to a flash chip. If serial TileLink and the SRAMs do work,
   we can use the flash as a larger source of external memory.
   We plan to use the [IS25LP128-JBLE](https://www.digikey.com/en/products/detail/issi-integrated-silicon-solution-inc/IS25LP128-JBLE/5189776)
   flash chip.

Both the PSRAM and the flash chip will use the 3.3V VDDIO3V3 supply,
so they will not be directly affected if we sweep the core VDDD1V8 supply
for low VDD tests of STAC circuitry.

Since we are not sure whether all the SRAMs and the serial TileLink interface work as expected,
we'd like it to be easy to switch between a PSRAM and a flash chip.

We will use a TI [TS3A27518EPWR](https://www.digikey.com/en/products/detail/texas-instruments/TS3A27518EPWR/2075716)
analog mux to switch between the QSPI peripherals, as shown in the datasheet for that part.
Only one QSPI device will be active at a time;
it is OK if we need to reset the core and/or re-power the board
when switching devices.
This analog mux is not intended to let us operate both QSPI devices
at the same time.
The control inputs of the mux are set by an RC debounced mechanical switch.

There are a few other options we've discussed:
* Having 8 female header pins on the main STAC board
  and mounting a separate breakout board (such as
  [this](https://www.adafruit.com/product/5632)) above those headers.
  This approach has poor return paths, since there is only one ground pin.
* Use a [Samtec MEC2-08-01-L-TH1](https://www.samtec.com/products/mec2-08-01-l-th1-wt)
  connector. This connector would require the flash/psram breakout board
  to have edge fingers at a 2mm pitch, along with a notch in the board to allow
  it to fit into the connector's polarizing plug.
* Place both the PSRAM and the flash on the main board, and mux between them.
  However, since the QSPI data pins are input/output, the mux would have to be an
  analog mux. This seems error prone, but is doable.
* Use 0ohm resistors to disconnect one of either the PSRAM or the flash.
  This is also doable, but inconvenient: we'd need to solder/desolder at least
  4 resistors per QSPI chip.
* Use a mechanical switch. We'd prefer not to place a mechanical switch
  in the signal path. RC-debouncing is not a viable option, as we don't
  want to add a large capacitance to these nets.

We believe the analog mux approach is best in terms of having
proper grounding and return paths, while not requiring us to design
a custom breakout board for the QSPI peripherals.

We aren't currently sure how often we'll need to swap between PSRAM/flash chips;
this will depend on bringup/debugging progress. The analog mux solution
will make it easy to swap peripherals whenever we need to do so.

### Serial TileLink

Serial TileLink (abbreviated serial TL) is a one bit per clock cycle variant of the TileLink protocol.
It can be used for reading/writing memory, including memory-mapped peripherals.

Serial TL uses the following signals:
* `serial_tl_bits_out_ready`
* `serial_tl_bits_out_valid`
* `serial_tl_bits_in_ready`
* `serial_tl_bits_in_bits`
* `serial_tl_clock`
* `serial_tl_bits_out_bits`
* `serial_tl_bits_in_valid`

We will use an Arty A7-100T FPGA to drive the serial TileLink interface.
ChipYard provides RTL for speaking serial TL, and we can program
the Arty FPGA with this RTL.

The serial TL signals connect to the FPGA via a 12-pin right-angle PMOD header.
Of the 12 PMOD pins, 2 are used for ground and 2 for power.
However, we leave the pins intended for power unconnected.
We don't want STAC to try to power itself off of the FPGA power supply,
and we don't want the FPGA to try to power itself off of the STAC power supply.

Serial TL is particularly important, as it allows reading/writing memory without
needing the Rocket core to be involved (unlike JTAG, which goes through the core).

## SRAM Test Area

The SRAM test area consists of several SRAM macros that we wish to test.
See the chip docs for a more detailed description of what is in the SRAM test area.

The SRAM test area can be configured via memory-mapped registers (which can
be read/written by either serial TL or JTAG), or via a scan chain.
The scan chain signals are connected to the Arty FPGA via PMOD headers.
We will write custom Chisel/Verilog to control these signals.

The SRAM test area uses the following signals:
* `SRAM_BIST_START`
* `SRAM_EN`
* `SRAM_BIST_EN`
* `SRAM_EXT_EN`
* `SRAM_SCAN_IN`
* `SRAM_SCAN_OUT`
* `SRAM_SCAN_EN`
* `SRAM_BIST_DONE`
* `SRAM_SCAN_MODE`

For each test SRAM macro, we plan to perform the following tests:
1. Single write then read at nominal VDD.
2. BIST March C- pattern at nominal VDD and low frequency.
3. BIST March C- pattern at nominal VDD, increasing frequency until errors are detected.
4. BIST March C- pattern at low frequency, decreasing VDD until errors are detected.

Note that only VDDD1V8 will be decreased; VDDIO3V3 will be kept at 3.3V.

Each operation will be set up configuring memory-mapped registers via serial TL using
the Arty FPGA. If serial TL does not work correctly, we will fall back to configuring
those registers via the scan chain.
The control flags (such as `SRAM_EXT_EN` and `SRAM_SCAN_MODE`) will be set by the FPGA;
the required values for each test mode is given in the chip docs.

For tests involving VDD sweeps, we will use a source meter configured to provide a precise supply voltage.
We plan to use a Keithley 2420 source meter. We estimate that we will need 1mV precision
over a 0.4V - 2.0V range. The specs for this source meter say it has a programming resolution
of 50uV and a source accuracy of ~600uV. This should be more than sufficient for our usage.
The source meter uses banana plugs, which we'll need to adapt to the male headers on our board
(using a cable like [this](https://www.amazon.com/Goupchn-Stackable-Banana-Plug-Jumper/dp/B08KZJS8VB),
though we can likely build something similar ourselves).

The source meter will be connected to VDDDEXT1V8, which is connected to VDDD1V8 via a jumper.
The Keithley source meter supports, but does not require, 4 wire sense.
We don't anticipate strictly needing to use 4 wire sense,
but we have designed the board to support a 4 wire connection
in case we find it necessary. The 4 wire connector is a male 0.1" Samtec TSW
header, placed close to the STAC VDDD pin nearest to the SRAM test area.

When testing at high clock frequencies or low VDDD, we will bypass the Rocket to the maximum
extent possible. This means we won't depend on external flash/PSRAM, JTAG/UART/serial TL, etc.
while performing the test.

