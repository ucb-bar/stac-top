// See LICENSE for license details.

//**************************************************************************
// SRAM BIST (c version)
//--------------------------------------------------------------------------

#include "mmio.h"
#include "srambist.h"
#include <stdio.h>

const sram_params_t SRAMS[NUM_SRAMS] = {
    {8, 4, 64, 24},
    {8, 4, 64, 32},
    {8, 4, 128, 16},
    {8, 4, 128, 24},
    {8, 4, 128, 32},
    {1, 8, 256, 8},
    {8, 8, 256, 16},
    {8, 4, 256, 32},
    {8, 4, 256, 64},
    {8, 4, 256, 128},
    {1, 8, 512, 8},
    {8, 4, 512, 32},
    {8, 4, 512, 64},
    {8, 4, 512, 128},
    {1, 8, 1024, 8},
    {8, 8, 1024, 32},
    {8, 4, 1024, 64},
    {1, 8, 2048, 8},
    {8, 8, 2048, 32},
    {1, 8, 4096, 8},
    {8, 8, 4096, 32},
    {8, 8, 8192, 32},
};

uint128_t create_mask(uint8_t sram_id) {
    sram_params_t params = SRAMS[sram_id];
    uint128_t ret;
    if (params.data_width >= 64) {
        ret.hi = (1UL << (params.data_width - 64)) - 1;
        ret.lo = 0xffffffffffffffff;
    } else {
        ret.hi = 0x0;
        ret.lo = (1UL << params.data_width) - 1;
    }
    return ret;
}

uint128_t create_wmask_mask(uint8_t sram_id, uint128_t wmask) {
    sram_params_t params = SRAMS[sram_id];
    uint128_t mask = {0, 0};
    for (int i = 0; i < params.data_width; i++) {
        int wmask_idx = i / params.wmask_granularity;
        int wmask_bit = (wmask_idx < 64 ? (wmask.lo >> wmask_idx) : (wmask.hi >> (wmask_idx - 64))) & 1;
        if (i < 64) {
            mask.lo |= (wmask_bit << i);
        } else {
            mask.hi |= (wmask_bit << (i - 64));
        }
    }
    return mask;
}

// `num_bits` must be less than 32.
uint32_t read_at_bit_offset(void* x, int bit_offset, uint8_t num_bits) {
  uint8_t* byte_ptr = ((uint8_t*) x) + bit_offset/8;
  uint64_t val = *(uint64_t*) byte_ptr;
  return (uint32_t)(val >> (bit_offset % 8)) & ((1 << num_bits) - 1);
}

// `num_bits` must be less than 32.
void write_at_bit_offset(void* x, int bit_offset, void* val, uint8_t num_bits) {
  uint8_t* byte_ptr = ((uint8_t*) x) + bit_offset/8;
  uint64_t mask = ((1 << (num_bits)) - 1) << (bit_offset % 8);
  uint64_t old_val = *(uint64_t*) byte_ptr;
  uint64_t new_val = *(uint64_t*) val;
  *(uint64_t*) byte_ptr = (old_val & ~mask) | (((uint64_t) new_val << (bit_offset % 8)) & mask);
}

void srambist_execute() {
  reg_write8(SRAMBIST_EX, 1);

  while ((reg_read8(SRAMBIST_DONE) & 0x1) == 0);
}

void srambist_operation_init(
    operation_t* op,
    operation_type_t operation_type,
    int rand_data,
    int rand_mask,
    uint32_t data_pattern_idx,
    uint32_t mask_pattern_idx,
    flip_type_t flipped
  ) {
  op->operation_type = operation_type;
  op->rand_data = rand_data;
  op->rand_mask = rand_mask;
  op->data_pattern_idx = data_pattern_idx;
  op->mask_pattern_idx = mask_pattern_idx;
  op->flipped = flipped;
}

void srambist_operation_element_init(
  element_t* elem,
  operation_element_t* op_elem,
  operation_t** operations,
  uint32_t max_idx,
  direction_t dir,
  uint32_t num_addrs
) {
  op_elem->operations = operations;
  op_elem->max_idx = max_idx;
  op_elem->dir = dir;
  op_elem->num_addrs = num_addrs;
  elem->operation_element = op_elem;
  elem->element_type = ELEMENT_TYPE_RW;
}

void srambist_wait_element_init(
  element_t* elem,
  uint32_t rand_addr
) {
  elem->wait_element.rand_addr = rand_addr;
  elem->element_type = ELEMENT_TYPE_WAIT;
}

void pack_operation(packed_operation_t* packed_op, operation_t* op) {
  write_at_bit_offset(packed_op, 0, &op->operation_type, 2);
  write_at_bit_offset(packed_op, 2, &op->rand_data, 1);
  write_at_bit_offset(packed_op, 3, &op->rand_mask, 1);
  write_at_bit_offset(packed_op, 4, &op->data_pattern_idx, 3);
  write_at_bit_offset(packed_op, 7, &op->mask_pattern_idx, 3);
  write_at_bit_offset(packed_op, 10, &op->flipped, 1);
}

void pack_operation_element(packed_operation_element_t* packed_op_elem, operation_element_t* op_elem) {
  packed_operation_t packed_op;
  for (int i = 0; i <= op_elem->max_idx; i++) {
    pack_operation(&packed_op, op_elem->operations[i]);
    write_at_bit_offset(packed_op_elem, 11 * i, &packed_op, 11);
  }
  write_at_bit_offset(packed_op_elem, 88, &op_elem->max_idx, 3);
  write_at_bit_offset(packed_op_elem, 91, &op_elem->dir, 2);
  write_at_bit_offset(packed_op_elem, 93, &op_elem->num_addrs, 14);
}

void pack_element(packed_element_t* packed_elem, element_t* elem) {
  if (elem->element_type == ELEMENT_TYPE_RW) {
    pack_operation_element((packed_operation_element_t*) packed_elem, elem->operation_element);
  }
  write_at_bit_offset(packed_elem, 107, &elem->wait_element, 14);
  write_at_bit_offset(packed_elem, 121, &elem->element_type, 1);
}

void pack_element_vec(packed_element_vec_t* packed_elem_vec, element_t** elems, uint8_t max_idx) {
  int bit_offset = 0;
  packed_element_t packed_elem;
  for (int i = 0; i <= max_idx; i++) {
    pack_element(&packed_elem, elems[i]);
    while (bit_offset < 122 * (i + 1)) {
      printf("%d\n", bit_offset);
      int to_write = 122 * (i + 1) - bit_offset;
      to_write = to_write > 16 ? 16 : to_write;
      uint32_t val_to_write = read_at_bit_offset(&packed_elem, bit_offset - 122 * i, to_write);
      write_at_bit_offset(packed_elem_vec, bit_offset, &val_to_write, to_write);
      bit_offset += to_write;
    }
  }
}

void srambist_write(uint16_t addr, uint128_t din, uint128_t mask, uint8_t sram_id) {
  reg_write16(SRAMBIST_ADDR, addr);
  reg_write128(SRAMBIST_DIN, din);
  reg_write128(SRAMBIST_MASK, mask);
  reg_write8(SRAMBIST_WE, 1);
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_SEL_MMIO);

  srambist_execute();
}

uint128_t srambist_read(uint16_t addr, uint8_t sram_id) {
  reg_write16(SRAMBIST_ADDR, addr);
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_WE, 0);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_SEL_MMIO);

  srambist_execute();

  return reg_read128(SRAMBIST_DOUT);
}

bist_result_t srambist_run_bist(
    uint8_t sram_id,
    uint64_t rand_seed,
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    element_t** elems,
    uint8_t max_elem_idx,
    pattern_table_t* pattern_table, 
    uint64_t cycle_limit,
    int stop_on_failure
) {
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_SEL_BIST);
  reg_write64(SRAMBIST_BIST_RAND_SEED, rand_seed);
  for (int i = 1; i < 5; i++) {
    reg_write64(SRAMBIST_BIST_RAND_SEED + 8 * i, 0);
  }
  reg_write128(SRAMBIST_BIST_SIG_SEED, sig_seed);
  reg_write16(SRAMBIST_BIST_MAX_ROW_ADDR, max_row_addr);
  reg_write8(SRAMBIST_BIST_MAX_COL_ADDR, max_col_addr);
  reg_write8(SRAMBIST_BIST_INNER_DIM, inner_dim);

  packed_element_vec_t packed_elem_vec;
  pack_element_vec(&packed_elem_vec, elems, max_elem_idx);
  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_ELEMENT_SEQUENCE + 8 * i, *(((uint64_t*) &packed_elem_vec) + i));
  }
  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_PATTERN_TABLE + 8 * i, *(((uint64_t*) pattern_table) + i));
  }
  reg_write8(SRAMBIST_BIST_MAX_ELEMENT_IDX, max_elem_idx);

  reg_write64(SRAMBIST_BIST_CYCLE_LIMIT, cycle_limit);
  reg_write32(SRAMBIST_BIST_STOP_ON_FAILURE, stop_on_failure);

  srambist_execute();

  bist_result_t result;
  result.fail = reg_read8(SRAMBIST_BIST_FAIL) & 0x1;
  result.fail_cycle = reg_read64(SRAMBIST_BIST_FAIL_CYCLE);
  result.expected = reg_read128(SRAMBIST_BIST_EXPECTED);
  result.received = reg_read128(SRAMBIST_BIST_RECEIVED);
  result.signature = reg_read128(SRAMBIST_BIST_SIGNATURE);

  return result;
}

bist_result_t srambist_run_bist_with_packed_elements(
    uint8_t sram_id,
    uint64_t rand_seed,
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    packed_element_vec_t* packed_elem_vec,
    uint8_t max_elem_idx,
    pattern_table_t* pattern_table, 
    uint32_t cycle_limit,
    int stop_on_failure
) {
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_SEL_BIST);
  reg_write64(SRAMBIST_BIST_RAND_SEED, rand_seed);
  for (int i = 1; i < 5; i++) {
    reg_write64(SRAMBIST_BIST_RAND_SEED + 8 * i, 0);
  }
  reg_write128(SRAMBIST_BIST_SIG_SEED, sig_seed);
  reg_write16(SRAMBIST_BIST_MAX_ROW_ADDR, max_row_addr);
  reg_write8(SRAMBIST_BIST_MAX_COL_ADDR, max_col_addr);
  reg_write8(SRAMBIST_BIST_INNER_DIM, inner_dim);

  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_ELEMENT_SEQUENCE + 8 * i, *(((uint64_t*) packed_elem_vec) + i));
  }
  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_PATTERN_TABLE + 8 * i, *(((uint64_t*) pattern_table) + i));
  }
  reg_write8(SRAMBIST_BIST_MAX_ELEMENT_IDX, max_elem_idx);

  reg_write32(SRAMBIST_BIST_CYCLE_LIMIT, cycle_limit);
  reg_write32(SRAMBIST_BIST_STOP_ON_FAILURE, stop_on_failure);

  srambist_execute();

  bist_result_t result;
  result.fail = reg_read8(SRAMBIST_BIST_FAIL) & 0x1;
  result.fail_cycle = reg_read64(SRAMBIST_BIST_FAIL_CYCLE);
  result.expected = reg_read128(SRAMBIST_BIST_EXPECTED);
  result.received = reg_read128(SRAMBIST_BIST_RECEIVED);
  result.signature = reg_read128(SRAMBIST_BIST_SIGNATURE);

  return result;
}

void srambist_write_packed_elements(
    packed_element_vec_t* packed_elem_vec,
    pattern_table_t* pattern_table
) {
  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_ELEMENT_SEQUENCE + 8 * i, *(((uint64_t*) packed_elem_vec) + i));
  }
  for (int i = 0; i < 16; i++) {
    reg_write64(SRAMBIST_BIST_PATTERN_TABLE + 8 * i, *(((uint64_t*) pattern_table) + i));
  }
}

bist_result_t srambist_run_bist_with_existing_patterns(
    uint8_t sram_id,
    uint64_t rand_seed,
    uint128_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    uint8_t max_elem_idx,
    uint32_t cycle_limit,
    int stop_on_failure
) {
  reg_write8(SRAMBIST_SRAM_ID, sram_id);
  reg_write8(SRAMBIST_SRAM_SEL, SRAM_SEL_BIST);
  reg_write64(SRAMBIST_BIST_RAND_SEED, rand_seed);
  for (int i = 1; i < 5; i++) {
    reg_write64(SRAMBIST_BIST_RAND_SEED + 8 * i, 0);
  }
  reg_write128(SRAMBIST_BIST_SIG_SEED, sig_seed);
  reg_write16(SRAMBIST_BIST_MAX_ROW_ADDR, max_row_addr);
  reg_write8(SRAMBIST_BIST_MAX_COL_ADDR, max_col_addr);
  reg_write8(SRAMBIST_BIST_INNER_DIM, inner_dim);

  reg_write8(SRAMBIST_BIST_MAX_ELEMENT_IDX, max_elem_idx);

  reg_write32(SRAMBIST_BIST_CYCLE_LIMIT, cycle_limit);
  reg_write32(SRAMBIST_BIST_STOP_ON_FAILURE, stop_on_failure);

  srambist_execute();

  bist_result_t result;
  result.fail = reg_read8(SRAMBIST_BIST_FAIL) & 0x1;
  result.fail_cycle = reg_read64(SRAMBIST_BIST_FAIL_CYCLE);
  result.expected = reg_read128(SRAMBIST_BIST_EXPECTED);
  result.received = reg_read128(SRAMBIST_BIST_RECEIVED);
  result.signature = reg_read128(SRAMBIST_BIST_SIGNATURE);

  return result;
}
