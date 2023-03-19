---
title: 'Getting Started: Boost Converter'
---

* TOC
{:toc}

# Introduction
The first converter we'll build is a simple boost converter:

![](kicad.png)

This is a good circuit to start since it is easy to drive the mosfet (no high side driving) and it's fun to get a higher voltage than the input. 

# Individual Components
First let's discuss the individual components:

## Capacitor
A capacitor is an electronic component that stores electric charge. It is made up of two conductive plates separated by an insulating material, or dielectric. When a voltage is applied across the plates, electrons collect on one of the plates and are removed from the other, creating an electric field between the plates. 

The amount of current that flows for a given rate of voltage change (volts per second) is determined by its capacitance, which is measured in farads.

The capacitors act as a filter, smoothing out the input and output voltage by storing and releasing charge as needed. When the voltage rises, the capacitor charges, helping to maintain a steady voltage. Conversely, when the output voltage drops, the capacitor discharges, providing additional current to help maintain the voltage.

## Inductor
An inductor is a passive electrical component that stores energy in a magnetic field when a current flows through it. It consists of a coil of wire, typically wrapped around a core made of a magnetic material such as iron.

When a voltage is applied to an inductor, the current through the inductor begins to change (increase or decrease). The rate at which it changes (amperes per second) for a given voltage is determined by it's inductance, which is measured in henrys.

## Resistor
A resistor is a passive electrical component that is used to limit the flow of current. Resistors are made of materials that resist the flow of electrical current, typically by converting some of the electrical energy into heat.

Resistors come in a variety of shapes and sizes, with the most common type being a cylindrical component with wire leads. The resistance of a resistor is measured in ohms, with higher ohm values indicating a greater resistance to the flow of current.

When a voltage is applied to a resistor a current directly proportional to the voltage flows through the resistor.

## Diode
A diode is an electronic component that allows current to flow in only one direction, effectively acting as a one-way valve for electrical current. In other words, a diode allows current to flow freely in one direction (known as the forward direction), while blocking current from flowing in the opposite direction (known as the reverse direction).

The basic structure of a diode consists of a piece of semiconductor material (usually made of silicon or germanium) with two regions, known as the P-type and N-type regions.

## NPN Transistor
An NPN transistor is a three-terminal electronic component that is widely used in electronic circuits as an amplifier or a switch. It is made up of three layers of semiconductor material, with a layer of p-type material sandwiched between two layers of n-type material. The three terminals of an NPN transistor are called the emitter, the base, and the collector.

When a small current is applied to the base of the transistor, it allows current to flow from the collector to the emitter, effectively turning the switch on. Conversely, when no current is applied to the base, the transistor blocks current flow, turning the switch off.

# Operation of the Boost Converter
Now that the different components are introduced, we can understand how the converter works. To keep things simple, the first converter will operate in discontinuous conduction mode (DCM). You don't have to understand this yet, but be aware that there is also a continuous conduction mode (CCM), which is harder to control.

The converter goes through several different phases during each switching cycle. These phases are:

1. **Charge Phase:** During this phase, the transistor is turned on and the current through the inductor increases, charging it up with energy from the input. At the same time, the output capacitor discharges to provide current to the load.

1. **Discharge Phase:** Once the desired current has been reached, the switch is turned off and the inductor begins to discharge its energy through the diode and into the output capacitor and the load. During this phase, the inductor current decreases until it reaches zero.

1. **Idle Phase:** Once the inductor current reaches zero, all the load current is provided by the output capacitor. The phase ends when the next charging phase is started by the controller.

# Building the Converter
Now that we have a basic understanding of the converter, we can start designing and building one. Unlike other circuits, there is a myriad of tradeoffs involved in the design. But don't worry, this section will guide you to a working converter.

To make things easy, the following tool will do all the math for you. Read below on how to use it.

<div data-tool="first"></div>

First you'll need an inductor. The main property of an inductor is it's inductance. But many more properties are in play. The most relevant for our application:

* Core Saturation: If the current through an inductor is too high, the core saturates and does not resist a change in current anymore
* Wire Resistance: The copper wire of the inductor has a resistance which causes losses. This can be reduced by using thicker wire, but this makes 
the inductor bigger, more expensive and heavier
* Core Eddy Currents: If the core was made of a solid block of iron, a change in the magnetic field would cause currents to flow within the core. This is mitigated by using
laminated iron sheets for low frequency (mains power) or iron (ferrite) powder for higher frequencies.

This is why our first converter is designed for relatively low frequencies (most effects occur at high frequencies) and low current (core saturation, wire resistance).

Choose or find an inductor made of iron powder (laminated metal sheets won't work) and measure it's inductance using your multimeter (if the function is available) or a component tester.

![Inductors](inductors.jpg)

The inductances are (left to right): 314uH, 25uH, 2mH, 1.3uH. If you plug these numbers in the calculator above, you'll see that the first and the third are a good choice. Of course, the big inductor will handle far larger currents.

Next get an adequate load resistor. The resistor will turn the output power of the converter into heat. Therefore it needs to be adequately sized. The following table shows typical sizes and their rating:

| **Power Rating <br/> [W]** | **Diameter<br/> [mm]** | **Length<br/>[mm]** | **Lead Diameter <br/> [mm]** |
|---|---|---|---|
| **1/8** | 1.8 | 3 | 0.45 |
| **1/4** | 2.5 | 6.5 | 0.6 |
| **1/2** | 3.2 | 8.5 | 0.6 |
| **1** | 5 | 11 | 0.8 |
| **2** | 5.5 | 15 | 1 |
| **3** | 6 | 17 | 1.1 |
| **5** | 7.5 | 24 | 1.2 |

The rest of the parts are less critical. For the capacitors, any larger value (up to a factor of perhaps 5x) will do. For the base resistor, I'd stay within 1/2 to 2x the value indicated.

![](fritzing_bb.svg)

# Explanation of the Calculations
If you are interested, this section explains the calculations performed by the tool above.

First some basic formulas:

## Inductor
The inductance determines how fast the current in the inductor will change if you apply a certain voltage. The formula is

$$ V = L \frac{\Delta I}{T} $$

With 

* $$V$$: The voltage applied to the inductor in volts
* $$L$$: The inductance of the inductor in henrys
* $$\Delta I$$: The change in current of over the given time frame in amperes
* $$T$$: The time the voltage is applied to the inductor in seconds

Usually, we don't need the voltage but are interested in the time for a given current change, or the current change over a given time:

$$ T = L \frac{\Delta I}{V}; \Delta I = \frac{VT}{L}  $$

## Capacitor
The capacitance determines much current flows through the capacitor for a certain rate of voltage change. The formula is

$$ I = C \frac{\Delta V}{T} $$

With 

* $$I$$: The current flowing through the inductor
* $$C$$: The capacitance of the capacitor in farads
* $$\Delta V$$: The change in voltage of over the given time frame in volts
* $$T$$: The time of the voltage change in seconds

The formula above solved for time, $$\Delta V$$ and $$C$$:

$$ T = C \frac{\Delta V}{I};\; \Delta V=\frac{IT}{C};\; C=\frac{IT}{\Delta V}$$

## Calculation
The calculation of the converter is not trivial, but it isn't rocket science either.

First, let's define the time: $$t_1$$ is the time of the charge phase and $$t_2$$ is the time of the discharge phase. $$T$$ is the total cycle time and $$d_\text{idle}$$ is the fraction of the cycle time of the idle phase.

$$ t_1+t_2+Td_\text{idle}=T $$

$$ t_1+t_2=T (1-d_\text{idle}) \tag{1}$$

Next we can look at the inductor current: During the charge phase the current raises linearly to $$I_\text{peak}$$:

$$ \frac{t_1V_\text{in}}{L} = I_\text{peak} \tag{2}$$

where $$L$$ is the inductance and $$V_\text{in}$$ is the input voltage. 

In a similar manner during the discharge phase the current drops to zero

$$ \frac{t_2(V_\text{out}-V_\text{in})}{L} = I_\text{peak} \tag{3}$$

with the output voltage $$V_\text{out}$$.

Ultimately, the average output current $$I_\text{out}$$ must equal the product of the average current during the discharge time, which is $$\frac{I_\text{peak}}{2}$$, and the fraction of time $$t_2$$ occupies during the entire cycle.

$$ I_\text{out} = \frac{I_\text{peak}}{2} \frac{t_2}{T} $$

$$ \frac{2TI_\text{out}}{t_2} =I_\text{peak} \tag{4}$$

Now we can derive the formula for $$I_\text{peak}$$:

$$\text{(2,3)}  \qquad  t_1V_\text{in}=t_2(V_\text{out}-V_\text{in})$$

$$ \frac{t_1}{t_2} = \frac{V_\text{out}-V_\text{in}}{V_\text{in}}=k \tag{5}$$

$$\text{(1,5)}  \qquad t_1=kt_2; (k+1)t_2=T (1-d_\text{idle}) $$

$$T=\frac{ (k+1)t_2}{1-d_\text{idle} } \tag{6}$$

$$\text{(4,6)} \qquad \frac{2(k+1)I_\text{out}}{1-d_\text{idle}} =I_\text{peak} $$

From this we can calculate the other quantities:

$$ t_1=\frac{LI_\text{peak}}{V_\text{in}} $$

$$ t_2= \frac{LI_\text{peak}}{V_\text{out}-V_\text{in}} $$

$$ T= \frac{t_1+t_2}{1-d_\text{idle}} $$

The output capacitance $$C_\text{out}$$ given the output ripple $$V_\text{rout}$$:

$$ C_\text{out}=\frac{I_\text{peak}t_2}{2V_\text{rout}} $$

And the input capacitance: 

$$ C_\text{in}=\frac{I_\text{peak}t_1}{2V_\text{rin}} $$

The load resistor is simply

$$ R_\text{load}=\frac{V_\text{out}}{I_\text{out}} $$

and the output power is

$$ P_{out}=V_\text{out}I_\text{out} $$ 