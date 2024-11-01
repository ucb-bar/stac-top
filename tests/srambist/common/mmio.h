#ifndef __MMIO_H__
#define __MMIO_H__

#include <stdint.h>
#include <stdbool.h>

typedef struct {
    uint64_t hi;
    uint64_t lo;
} uint128_t;

static inline bool eq128(uint128_t a, uint128_t b) {
    return (a.hi == b.hi) && (a.lo == b.lo);
}

static inline void reg_write8(uintptr_t addr, uint8_t data)
{
	volatile uint8_t *ptr = (volatile uint8_t *) addr;
	*ptr = data;
}

static inline uint8_t reg_read8(uintptr_t addr)
{
	volatile uint8_t *ptr = (volatile uint8_t *) addr;
	return *ptr;
}

static inline void reg_write16(uintptr_t addr, uint16_t data)
{
	volatile uint16_t *ptr = (volatile uint16_t *) addr;
	*ptr = data;
}

static inline uint16_t reg_read16(uintptr_t addr)
{
	volatile uint16_t *ptr = (volatile uint16_t *) addr;
	return *ptr;
}

static inline void reg_write32(uintptr_t addr, uint32_t data)
{
	volatile uint32_t *ptr = (volatile uint32_t *) addr;
	*ptr = data;
}

static inline uint32_t reg_read32(uintptr_t addr)
{
	volatile uint32_t *ptr = (volatile uint32_t *) addr;
	return *ptr;
}

static inline void reg_write64(unsigned long addr, uint64_t data)
{
	volatile uint64_t *ptr = (volatile uint64_t *) addr;
	*ptr = data;
}

static inline uint64_t reg_read64(unsigned long addr)
{
	volatile uint64_t *ptr = (volatile uint64_t *) addr;
	return *ptr;
}

static inline void reg_write128(unsigned long addr, uint128_t data)
{
    reg_write64(addr, data.lo);
    reg_write64(addr + 8, data.hi);
}

static inline uint128_t reg_read128(unsigned long addr)
{
    uint128_t ret;
    ret.lo = reg_read64(addr);
    ret.hi = reg_read64(addr + 8);
	return ret;
}

#endif
