// See LICENSE for license details.

//**************************************************************************
// MMIO read and write test
//--------------------------------------------------------------------------
//

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

//--------------------------------------------------------------------------
// Main

int main( int argc, char* argv[] )
{
  
  pattern_table_t pattern_table = { { 0, 0xff, 0xcc, 0, 0, 0, 0, 0} };
  operation_t write_op = srambist_operation_init(OP_TYPE_WRITE, 0, 0, 0, 1, FLIP_TYPE_UNFLIPPED);
  operation_t read_op = srambist_operation_init(OP_TYPE_READ, 0, 0, 0, 1, FLIP_TYPE_UNFLIPPED);
  operation_t write_op_flipped = srambist_operation_init(OP_TYPE_WRITE, 0, 0, 0, 1, FLIP_TYPE_FLIPPED);
  operation_t read_op_flipped = srambist_operation_init(OP_TYPE_READ, 0, 0, 0, 1, FLIP_TYPE_FLIPPED);

  operation_t ops[4] = {write_op_flipped, read_op_flipped, write_op, read_op};
  element_t op_elem = srambist_operation_element_init(ops, 3, DIRECTION_UP, 0);

  element_t elems[2] = {op_elem, op_elem};

  bist_result_t result = srambist_run_bist(0, 1, 55, 15, 7, DIMENSION_COL, elems, 1, pattern_table, 0, 1);

  if (result.fail != 0) {
    printf("BIST unexpectedly failed\n");
    return 1;
  }
  printf("Hardware result %d is correct for SRAM read\n", result);
  return 0;
}
