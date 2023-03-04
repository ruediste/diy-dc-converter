# DIY Digital DC Converters
Welcome, electronic hobbyists! If you're interested in building electronic circuits or projects, you've probably heard the term "DC power converter" before. A DC power converter is an electronic circuit that takes an input DC voltage and converts it to a different output DC voltage. These converters are used in a wide range of applications, from powering small electronic devices to controlling the speed of electric motors. Understanding the basics of DC power converters can open up a world of possibilities for your electronic projects, so let's dive in!

Building DIY digitally controlled DC converters seems to be largely ignored by the community. That's a gap that needs to be filled!

The material available online is largely targeted at university students and might be a bit hard to approach for the average hobbyist. That's where this repository comes in: The goal is to show how to build increasingly complex converters, in a very hands-on way.

By controlling the converter digitally using standard hobbyist microcontroller boards from the arduino ecosystem, the circuits can be kept simple and the control algorithms can be quite sophisticated.

## State of the Project
I'm just getting started.

## What is a DC Converter?
A DC converter is an electronic circuit that takes an input DC voltage and converts it to a different output DC voltage. The input voltage can come from a variety of sources, such as a battery, a power supply, or a renewable energy source like a solar panel or wind turbine. The output voltage can be either higher or lower than the input voltage, depending on the specific application.

DC converters come in many different types and topologies, each with its own advantages and limitations. Some common types include:

* Buck converters: These convert a higher input voltage to a lower output voltage. They are widely used in battery-powered applications, such as portable electronics and electric vehicles.

* Boost converters: These convert a lower input voltage to a higher output voltage. They are commonly used in applications such as LED lighting and high-voltage power supplies.

* Buck-boost converters: These can convert an input voltage to either a higher or lower output voltage, depending on the application. They are used in a wide range of applications, from battery chargers to voltage regulators.

* Flyback converters: These use a transformer to provide isolation between the input and output, making them useful for applications where electrical isolation is important, such as medical devices and power supplies.

Overall, DC converters are essential components in many electronic circuits and projects. Understanding the different types and topologies of DC converters can help you choose the right one for your application, and ultimately make your projects more efficient and effective.

## Required Components and Tools
To get started you'll need:

* Black Pill Board STM32F401
* A few transistors, resistors, inductors and capacitors
* Breadboard
* Multimeter

Very handy: a component tester

For more advanced designs:

* Higher power transistors and inductors
* A soldering iron and some protoboards

Highly recommended: an entry level oscilloscope (two channel, 200$ range)

## The Converters
See [the converter list](https://ruediste.github.io/diy-dc-converter/)

## Contributing

### Updating schematics
* export as svg
* convert to png (for white background and cropping): `inkscape --batch-process --export-area-drawing --export-type=png -b "#ffffff" -y 1.0 -d 300 *.svg && mogrify -bordercolor white -border 50 *.png`