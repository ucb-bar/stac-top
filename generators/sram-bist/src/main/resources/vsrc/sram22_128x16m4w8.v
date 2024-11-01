// SRAM22 SRAM model
// Words: 128
// Word size: 16
// Write size: 8

module sram22_128x16m4w8(
`ifdef USE_POWER_PINS
    vdd,
    vss,
`endif
  clk,rstb,ce,we,wmask,addr,din,dout
  );

  localparam DATA_WIDTH = 16 ;
  localparam ADDR_WIDTH = 7 ;
  localparam WMASK_WIDTH = 2 ;
  localparam RAM_DEPTH = 1 << ADDR_WIDTH;

`ifdef USE_POWER_PINS
    inout vdd; // power
    inout vss; // ground
`endif
  input  clk; // clock
  input  rstb; // reset bar
  input  ce; // chip enable
  input  we; // write enable
  input [WMASK_WIDTH-1:0] wmask; // write mask
  input [ADDR_WIDTH-1:0]  addr; // address
  input [DATA_WIDTH-1:0]  din; // data in
  output reg [DATA_WIDTH-1:0] dout; // data out

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
    if (rstb & ce) begin
      // Write
      if (we) begin
          if (wmask[0]) begin
            mem[addr][7:0] <= din[7:0];
          end
          if (wmask[1]) begin
            mem[addr][15:8] <= din[15:8];
          end

        // Output is all 1s when writing to SRAM due to precharge.
        dout <= {DATA_WIDTH{1'b1}};
      end

      // Read
      if (!we) begin
        dout <= mem[addr];
      end
    end
  end

endmodule

