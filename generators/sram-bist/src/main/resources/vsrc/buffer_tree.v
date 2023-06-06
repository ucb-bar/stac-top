module buffer_tree(
    X,
    A
);

    output X   ;
    input  A   ;
    wire _buf1_X;	
    sky130_fd_sc_hd__bufbuf_16 buf1 (	
      .A (io_A),
      .X (_buf1_X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2 (
      .A (_buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_1 (
      .A (_buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_2 (
      .A (_buf1_X),
      .X (X)
    );
    sky130_fd_sc_hd__bufbuf_16 buf2_3 (
      .A (_buf1_X),
      .X (X)
    );
endmodule
