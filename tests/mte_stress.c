// // #include "mte.h"
// // #include <stdint.h>
// // #include <unistd.h>
// // #include <stdlib.h>
// // #include <stdio.h>
// // #include <string.h>

// // static uintptr_t 
// // irt(uintptr_t t) {
// //     uint64_t irt_rd = 0, irt_rs1 = 0, irt_rs2 = 0;
// //     while (!(support_mte_tag_extract(irt_rd))) {
// //         irt_rs1 = t;
// //         irt_rs2 = 0;
// //         MTE_IRT(5, irt_rd, 6, irt_rs1, 0, irt_rs2);
// //     }
// //     return irt_rd;
// // }

// // static void stti(uintptr_t t) {
// //     uint64_t stti_rd;
// //     uint64_t stti_rs1 = t;
// //     /* 0x8000ABC0 := B */
// //     MTE_STTI(stti_rd, stti_rs1, /* rd */ 5, /* rs1 */ 6, 0b000000000000);
// // }

// // int main(void) {
// //     printf("hello world\n");
// //     size_t span = 64;
// //     uintptr_t *prev = malloc(sizeof(uintptr_t) * span);
// //     printf("prev=%p\n", prev);
// //     for (int i = 0; i < span; i++) {
// //         prev[i] = 0;
// //     }

// //     uintptr_t buf = (uintptr_t)malloc(4096 * 64);
// //     for (int q = 0; q < 64; q++) {
// //         for (uint64_t i = 0; i < 64; i++) {
// //             uint64_t prev_ptr = prev[(i - 1) & (span - 1)];
// //             printf("p %lx\n", prev_ptr);
// //             if (prev_ptr) {
// //                 if (support_mte_tag_extract(prev_ptr) & 0b1) {
// //                     *(volatile uint64_t *)prev_ptr;
// //                 } else {
// //                     *(volatile uint64_t *)prev_ptr = prev_ptr;
// //                 }
// //             }

// //             uintptr_t base = buf + 4096 * i;
// //             while (irt(0) & 0b11) {}
// //             base = irt(base);
// //             printf("b %lx\n", base);
// //             stti(base);

// //             if (support_mte_tag_extract(base) & 0b10) {
// //                 while (irt(0) & 0b11) {}
// //             }

// //             if (support_mte_tag_extract(base) & 0b100) {
// //                 *(volatile uint64_t *)base;
// //             } else {
// //                 *(volatile uint64_t *)base = base;
// //             }

// //             prev[i] = base;
// //         }
// //     }

// //     return 0;
// // }


#include "mte.h"
#include <stdint.h>
#include <unistd.h>
#include <stdlib.h>
#include <stdio.h>
#include <string.h>

// static uintptr_t 
// irt(uintptr_t t) {
//     uint64_t irt_rd = 0, irt_rs1 = 0, irt_rs2 = 0;
//     while (!(support_mte_tag_extract(irt_rd))) {
//         irt_rs1 = t;
//         irt_rs2 = 0;
//         MTE_IRT(5, irt_rd, 6, irt_rs1, 0, irt_rs2);
//     }
//     return irt_rd;
// }

static uintptr_t 
irt(uintptr_t t) {
    uint64_t irt_rd = 0, irt_rs1 = 0, irt_rs2 = 0;
    irt_rs1 = t;
    irt_rs2 = 0;
    MTE_IRT(5, irt_rd, 6, irt_rs1, 0, irt_rs2);

    /* avoid the permissive tag, biasing */
    uint64_t addti_rd = 0, addti_rs1 = irt_rd;
    MTE_ADDTI(5, addti_rd, 6, addti_rs1, /* addr_off */ 0, /* tag_off */ 0);

    return addti_rd;
}
static inline void stti(uintptr_t t) {
    MTE_STTI(/* rs1 */ t, /* rs1_n */ 6, /* addr off<<4 */ 0b00000000, /* tag off */ 0b0000)
}

int main(void) {
    printf("hello world\n");
    size_t span = 64;
    uintptr_t *prev = malloc(sizeof(uintptr_t) * span);
    printf("prev=%p\n", prev);
    for (int i = 0; i < span; i++) {
        prev[i] = 0;
    }

    #define PROBE_SPACE 128
    uintptr_t buf = (uintptr_t)malloc(4096 * PROBE_SPACE);
    for (int q = 0; q < 4096; q++) {
        uint64_t i = support_mte_tag_extract(irt(0)) << 4 | support_mte_tag_extract(irt(0));
        i &= (uint64_t)(PROBE_SPACE - 1);
        // printf("i=%lx\n", i);
        uint64_t prev_ptr = prev[(i - 1) & (span - 1)];
        if (prev_ptr) {
            if (support_mte_tag_extract(prev_ptr) & 0b1) {
                *(volatile uint64_t *)prev_ptr;
            } else {
                *(volatile uint64_t *)prev_ptr = prev_ptr;
            }
        }

        #define local_span 4
        uint64_t local_prev[local_span] = {0};
        uintptr_t base;
        for (uint64_t j = 0; j < local_span; j++) {
            base = buf + 4096 * i + 16 * j;
            while (support_mte_tag_extract(irt(0)) & 0b11) {}
            base = irt(base);
            stti(base);
            if (support_mte_tag_extract(irt(0)) & 0b1) {
                printf(".\n");
            }
            uint64_t l_prev = local_prev[(i - 1) & (local_span - 1)];
            if (l_prev) {
                if (support_mte_tag_extract(l_prev) & 0b1000) {
                    *(volatile uint64_t *)l_prev;
                } else {
                    *(volatile uint64_t *)l_prev = l_prev;
                }
            }
            local_prev[j] = base;

            if (support_mte_tag_extract(base) & 0b10) {
                while (irt(0) & 0b11) {}
            }

            if (support_mte_tag_extract(base) & 0b100) {
                *(volatile uint64_t *)base;
            } else {
                *(volatile uint64_t *)base = base;
            }

        }

        prev[i] = base;
    }

    return 0;
}