// SRAM22 TDC model
// Output width: 252

module tdc_64(
`ifdef USE_POWER_PINS
    vdd,
    vss,
`endif
    a, b, reset_b, dout[DATA_WIDTH-1:0]
  );

  parameter DATA_WIDTH = 252 ;

`ifdef USE_POWER_PINS
    inout vdd; // power
    inout vss; // ground
`endif
  input  a; // start clock
  input  b; // stop clock
  input  reset_b; // active-low reset
  output [DATA_WIDTH-1:0] dout; // data out

  assign dout = 0;
  // wire [DATA_WIDTH-1:0] intermediate;

  // genvar i;

  // generate for (i = 0; i < DATA_WIDTH; i = i + 1) begin
  //   always @(*) begin
  //     if (i == 0) intermediate[i] <= #1 a;
  //     else intermediate[i] <= #1 intermediate[i-1];
  //   end
  // end endgenerate

  // always @(posedge b, negedge reset_b)
  // begin
  //   if (~reset_b) dout <=  {DATA_WIDTH{1'b0}};
  //   else dout <= intermediate;
  // end

endmodule

