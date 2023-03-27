#ifndef CONTROL_MAIN_H
#define CONTROL_MAIN_H

#include <stdint.h>

#ifdef __cplusplus
extern "C"
{
#endif

    void receiveData(uint8_t *buf, uint32_t *len);
    void onControlLoop();
    void controlMain();

#ifdef __cplusplus
}
#endif
#endif
