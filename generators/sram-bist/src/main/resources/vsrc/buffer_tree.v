module buffer_tree(
    X,
    A
);

    output X   ;
    input  A   ;
    wire buf1_X;	
    sky130_fd_sc_hd__bufbuf_16 buf1 (	
      .A (A),
      .X (buf1_X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2 (
      .A (buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_1 (
      .A (buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_2 (
      .A (buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_3 (
      .A (buf1_X),
      .X (X)
    );
endmodule
