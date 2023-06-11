---
title: 'Light Load Operation'
nav_order: 40
---


* TOC
{:toc}

# Introduction
Thus far we have been working with fixed switching frequencies. This has a big downside when the converter is operating at light load. Every time the transistor or mosfet is switched on or off, some power is lost and dissipated as heat. For light loads the duty cycles become very small. In every cycle very little current flows through the inductor, but the switching losses are still present. 

This issue can be addressed by keeping the on-time of the switch constant and reducing the switching frequency instead (Constant On Time, COT). This reduces the duty cycle and the switching losses at the same time. To allow the converter to operate over a range of input voltages $$V_\text{in}$$, we can calculate the on time based on a desired peak inductor current $$\hat I$$:

$$ t_\text{on}=\frac{L\hat I}{V_\text{in}}$$

The power converted in COT mode is proportional to the frequency. Thus, by changing the PWM frequency we can control the output voltage.

If the load drops to a very low level, the frequency becomes also very low. It is not practical to use a PID controller if one period lasts multiple seconds. Therefore, below a minimum load we switch to a mode called cycle skipping. This is essentially a form of hysteric control: The PWM frequency kept fixed. Whenever the voltage drops below the target voltage, the switching PWM signal is enabled. If it rises above the target voltage, the PWM signal is disabled.

# Modes Overview
This is the first converter with multiple control modes intended to operate over a wide range of output conditions. There are the following modes:

* **COT:** A PID controller is used to control the PWM frequency
* **Cycle Skipping:** For light loads, we use a fixed PWM frequency and switch the PWM output on if the output voltage exceeds ths target voltage 
* **Low Output Voltage (LOV):** If the output voltage is close to the input voltage, we try to raise the output voltage in short bursts

The following sections detail each mode and the conditions that lead to mode switches.

# COT
In the COT mode we use a PID controller to adjust the PWM frequency to reach the target voltage at the output. A PID controller works best for linear system. That means, at all operation points, the same change of the controller output should have the same effect on the converter. In our case, we choose to control the output current $$I_\text{out}$$. For a fixed target voltage, this is equivalent to controlling the target power, and for an ohmic load it is also equivalent to controlling the output voltage. Also, the output (and load) capacitance is typically fixed. Thus the response time to a current change is independent of the output voltage.

Given the output current $$I_\text{out}$$, we can derive the PWM period $$T$$. First, we can calculate the time $$t_\text{fall}$$ for the inductor current to reach zero when the switch is turned off:

$$ t_\text{fall}=\frac{L\hat I}{V_\text{out}-V_\text{in}}$$

The average current flowing to the output during this time is $$\hat{I}/2$$. The rest of the PWM period $$T$$ there is no current flowing to the output. The average current flowing is therefore

$$ I_\text{out}=\frac{\hat{I}}{2} * \frac{t_\text{fall}}{T} $$

Solving for $$T$$ leads to

$$ T=\frac{\hat{I}}{2} * \frac{t_\text{fall}}{I_\text{out}} $$

$$ T=\frac{L\hat{I}^2}{2I_\text{out}(V_\text{out}-V_\text{in})} $$


Switching from COT to cycle skipping happens as soon as the switching frequency reaches the minimum switching frequency. In cycle skipping, the switching frequency is 3 times higher than the minimum frequency. This avoids frequent switching between COT and cycles skipping mode.

# Cycle Skipping

Whenever the PWM signal is enabled, the starting voltage is marked. If the voltage does not increase after three switching cycles, the controller switches back to COT mode. 

# Low Output Voltage (LOV)



