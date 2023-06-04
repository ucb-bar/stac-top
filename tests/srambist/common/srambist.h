#ifndef __SRAMBIST_H__
#define __SRAMBIST_H__
#include <stdint.h>

#define SRAMBIST_ADDR 0x1000
#define SRAMBIST_DIN 0x1008
#define SRAMBIST_MASK 0x1010
#define SRAMBIST_WE 0x1018
#define SRAMBIST_SRAM_ID 0x1020
#define SRAMBIST_SRAM_SEL 0x1028
#define SRAMBIST_SAE_CTL 0x1030
#define SRAMBIST_SAE_SEL 0x1038
#define SRAMBIST_DOUT 0x1040
#define SRAMBIST_TDC 0x1048
#define SRAMBIST_DONE 0x1068
#define SRAMBIST_BIST_RAND_SEED 0x1070
#define SRAMBIST_BIST_SIG_SEED 0x1080
#define SRAMBIST_BIST_MAX_ROW_ADDR 0x1088
#define SRAMBIST_BIST_MAX_COL_ADDR 0x1090
#define SRAMBIST_BIST_INNER_DIM 0x1098
#define SRAMBIST_BIST_ELEMENT_SEQUENCE 0x10A0
#define SRAMBIST_BIST_PATTERN_TABLE 0x1120
#define SRAMBIST_BIST_MAX_ELEMENT_IDX 0x1140
#define SRAMBIST_BIST_CYCLE_LIMIT 0x1148
#define SRAMBIST_BIST_STOP_ON_FAILURE 0x1150
#define SRAMBIST_BIST_FAIL 0x1158
#define SRAMBIST_BIST_FAIL_CYCLE 0x160
#define SRAMBIST_BIST_EXPECTED 0x1168
#define SRAMBIST_BIST_RECEIVED 0x1170
#define SRAMBIST_BIST_SIGNATURE 0x1178
#define SRAMBIST_EX 0x1180

#define SRAMBIST_PATTERN_TABLE_LENGTH_LOG2 3
#define SRAMBIST_OPERATIONS_PER_ELEMENT 8
#define SRAMBIST_OPERATIONS_PER_ELEMENT_LOG2 3
#define SRAMBIST_RAND_ADDR_WIDTH 14
#define SRAMBIST_ELEMENT_TABLE_LENGTH 14

typedef enum {
  SRAM_SEL_BIST = 0,
  SRAM_SEL_MMIO = 1,
} sram_sel_t;

typedef enum {
  OP_TYPE_READ = 0,
  OP_TYPE_WRITE = 1,
  OP_TYPE_RAND = 2,
} operation_type_t;

typedef enum {
  FLIP_TYPE_FLIPPED = 0,
  FLIP_TYPE_UNFLIPPED = 1,
} flip_type_t;

typedef enum {
  DIRECTION_UP = 0,
  DIRECTION_DOWN = 1,
  DIRECTION_RAND = 2,
} direction_t;

typedef enum {
  ELEMENT_TYPE_WAIT = 0,
  ELEMENT_TYPE_RW = 1,
} element_type_t;

typedef enum {
  DIMENSION_ROW = 0,
  DIMENSION_COL = 1,
} dimension_t;

typedef struct {
  operation_type_t operation_type;
  int rand_data;
  int rand_mask;
  uint8_t data_pattern_idx;
  uint8_t mask_pattern_idx;
  flip_type_t flipped;
} operation_t;

typedef struct {
  uint8_t inner[2];
} packed_operation_t;

typedef struct {
  operation_t operations[SRAMBIST_OPERATIONS_PER_ELEMENT];
  uint8_t max_idx;
  direction_t dir;
  uint16_t num_addrs;
} operation_element_t;

typedef struct {
  uint8_t inner[14];
} packed_operation_element_t;

typedef struct {
  uint16_t rand_addr;
} wait_element_t;

typedef struct {
  operation_element_t operation_element;
  wait_element_t wait_element;
  element_type_t element_type;
} element_t;

typedef struct {
  uint8_t inner[16];
} packed_element_t;

typedef struct {
  uint8_t inner[122];
} packed_element_vec_t;

typedef struct {
  uint32_t patterns[8];
} pattern_table_t;

typedef struct {
  int fail;
  uint32_t fail_cycle;
  uint32_t expected;
  uint32_t received;
  uint32_t signature;
} bist_result_t;

uint32_t read_at_bit_offset(void* x, int bit_offset, uint8_t num_bits);
void write_at_bit_offset(void* x, int bit_offset, void* val, uint8_t num_bits);

operation_t srambist_operation_init(
  operation_type_t operation_type,
  int rand_data,
  int rand_mask,
  uint32_t data_pattern_idx,
  uint32_t mask_pattern_idx,
  flip_type_t flipped
);

element_t srambist_operation_element_init(
  operation_t* operations,
  uint32_t max_idx,
  direction_t dir,
  uint32_t num_addrs
);

element_t srambist_wait_element_init(
  uint32_t rand_addr
);

packed_operation_t pack_operation(operation_t* op);
packed_operation_element_t pack_operation_element(operation_element_t* op_elem);
packed_element_t pack_element(element_t* elem);
packed_element_vec_t pack_element_vec(element_t* elems, uint8_t max_idx);

void srambist_write(uint32_t addr, uint32_t din, uint32_t mask, uint8_t sram_id);
uint32_t srambist_read(uint32_t addr, uint8_t sram_id);

bist_result_t srambist_run_bist(
    uint8_t sram_id,
    uint64_t rand_seed,
    uint32_t sig_seed,
    uint16_t max_row_addr,
    uint8_t max_col_addr,
    dimension_t inner_dim,
    element_t* elems,
    uint8_t max_elem_idx,
    pattern_table_t pattern_table, 
    uint32_t cycle_limit,
    int stop_on_failure
);

#endif
