#ifndef MESSAGE_RECEIVING_H
#define MESSAGE_RECEIVING_H

#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

    void receiveData(uint8_t *buf, uint32_t *len);

#ifdef __cplusplus
}
#endif
#endif
