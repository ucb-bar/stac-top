// SRAM22 SRAM model
// Words: 4096
// Word size: 8
// Write size: 8

module sram22_4096x8m8w8_test(
`ifdef USE_POWER_PINS
    vdd,
    vss,
`endif
  clk,we,wmask,addr,din,dout,sae_int,sae_muxed
  );

  // These parameters should NOT be set to
  // anything other than their defaults.
  parameter DATA_WIDTH = 8 ;
  parameter ADDR_WIDTH = 12 ;
  parameter WMASK_WIDTH = 1 ;
  parameter RAM_DEPTH = 1 << ADDR_WIDTH;

`ifdef USE_POWER_PINS
    inout vdd; // power
    inout vss; // ground
`endif
  input  clk; // clock
  input  we; // write enable
  input [WMASK_WIDTH-1:0] wmask; // write mask
  input [ADDR_WIDTH-1:0]  addr; // address
  input [DATA_WIDTH-1:0]  din; // data in
  output reg [DATA_WIDTH-1:0] dout; // data out
  
  input sae_muxed; // muxed sense amp enable
  output sae_int; // internal sense amp enable
  

  reg [DATA_WIDTH-1:0] mem [0:RAM_DEPTH-1];

  // Fill memory with zeros.
  // For simulation only. The real SRAM
  // may not be initialized to all zeros.
  integer i;
  initial begin
    for (i = 0 ; i < RAM_DEPTH ; i = i + 1)
    begin
      mem[i] = {DATA_WIDTH{1'b0}};
    end
  end

  always @(posedge clk)
  begin
    // Write
    if (we) begin
        if (wmask[0]) begin
          mem[addr][7:0] <= din[7:0];
        end

      // Output is arbitrary when writing to SRAM
      dout <= {DATA_WIDTH{1'bx}};
    end

    // Read
    if (!we) begin
      dout <= mem[addr];
    end
  end

endmodule

