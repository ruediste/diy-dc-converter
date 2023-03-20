# DIY Digital DC Converters
See the [documentation](https://ruediste.github.io/diy-dc-converter/)

## Contributing

### Updating schematics
* export as svg
* convert to png (for white background and cropping): `inkscape --batch-process --export-area-drawing --export-type=png -b "#ffffff" -y 1.0 -d 300 *.svg && mogrify -bordercolor white -border 50 *.png`