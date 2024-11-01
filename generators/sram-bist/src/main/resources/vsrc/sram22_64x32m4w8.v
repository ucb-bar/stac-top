// SRAM22 SRAM model
// Words: 64
// Word size: 32
// Write size: 8

module sram22_64x32m4w8(
`ifdef USE_POWER_PINS
    vdd,
    vss,
`endif
  clk,rstb,ce,we,wmask,addr,din,dout
  );

  localparam DATA_WIDTH = 32 ;
  localparam ADDR_WIDTH = 6 ;
  localparam WMASK_WIDTH = 4 ;
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
          if (wmask[2]) begin
            mem[addr][23:16] <= din[23:16];
          end
          if (wmask[3]) begin
            mem[addr][31:24] <= din[31:24];
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

