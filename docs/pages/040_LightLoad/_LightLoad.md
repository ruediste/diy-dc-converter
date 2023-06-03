---
title: 'Light Load Operation'
nav_order: 40
---


* TOC
{:toc}

# Introduction
Thus far we have been working with fixed switching frequencies. This has a big downside when the converter is operating at light load. Every time the transistor or mosfet is switched on or off some power is lost and dissipated as heat. For light loads the duty cycles become very small. In every cycle very little current flows through the inductor, but the switching losses are still present. 

This issue can be addressed by keeping the on-time of the switch constant and reducing the switching frequency instead (Constant On Time, COT). This reduces the duty cycle and the switching losses at the same time. To allow the converter to operate over a range of input voltages $$V_\text{in}$$, we can calculate the on time based on a desired peak inductor current $$\hat I$$:

$$ T=\frac{L\hat I}{V_\text{in}}; f=\frac{1}{T}=\frac{V_\text{in}}{L\hat I}$$

The power converted in DCM mode is proportional to the frequency. We therefore control the frequency via PID and convert it to a time period afterwards, instead of directly controlling the period.

If the load drops to a very low level, the frequency becomes also very low. It is not practical to use COT if one period lasts multiple seconds. Below a minimum frequency we therefore switch to a mode called cycle skipping. This is essentially a form of hysteric control: Whenever the voltage drops below the target voltage, the switching PWM signal is enabled. If it rises above the target voltage, the PWM signal is disabled.

# Mode Switching
Switching from COT to cycle skipping happens as soon as the switching frequency reaches the minimum switching frequency. In cycle skipping, the switching frequency is 10% larger than the minimum frequency. 

Whenever the PWM signal is enabled, the starting voltage is marked. If the voltage does not increase after three switching cycles, the controller switches back to COT mode. 