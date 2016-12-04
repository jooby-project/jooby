## Troubleshooting

### Building sprites

#### If the sprites are generated are empty/blank

A. Make sure your config (`options.spriteElementPath`) is pointing to the folder containing your source svg files.

B. If you are using Adobe Illustrator to produce or save your source svg files - ensure the `Responsive` option in the "More Options"panel is **unticked**:

![airesponsiveoption](https://cloud.githubusercontent.com/assets/868834/3855442/28f33768-1ee5-11e4-89db-838b38425568.jpg)

#### If your sprite looks garbled

This is most likely due to one or more [plugins](https://github.com/svg/svgo/tree/master/plugins) in the svg minifier ([svgo](https://github.com/svg/svgo)) used in the sprite builder. If you run into those edge cases disabling plugins can be done via [`options.svgo`](https://github.com/drdk/dr-svg-sprites#optionssvgo). At default the following plugins are disabled: `moveGroupAttrsToElems`,  `collapseGroups` and `removeUselessStrokeAndFill`.
If you are unable to find the plugins causing the problem create an issue here and submit as much relevant data as possible.

### In browsers

#### If your sprite isn't displayed - or is very pixelated

Most devices have a maximum allowed buffer for images - if that buffer is exceeded you might experience your sprite either not getting displayed at all - or it getting downsampled.
It's hard to give precise numbers as the maximum buffer varies from device to device - but for iOS it's 3 megapixels (3 * 1024 * 1024 = 3,145,728 pixels) for devices with less than 256mb ram and 5 megapixels for devices with more.
As long as your sprite width times height is below the 3 megapixel range you should be good.

References:

* [Know iOS Resource Limits](https://developer.apple.com/library/safari/documentation/AppleApplications/Reference/SafariWebContent/CreatingContentforSafarioniPhone/CreatingContentforSafarioniPhone.html#//apple_ref/doc/uid/TP40006482-SW15)