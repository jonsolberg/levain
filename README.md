# Levain (Android)

A fully offline bread calculator based on baker's percentages.
Everything is bundled in `app/src/main/assets/index.html`. The only network use is an optional daily check for new versions on GitHub, which you can turn off at first start.

<p align="center">
  <img src="docs/screenshot-dough.png" width="250" alt="Levain dough settings">
  <img src="docs/screenshot-ingredients.png" width="250" alt="Levain ingredient list and baker's percentages">
  <img src="docs/screenshot-dark.png" width="250" alt="Levain in dark mode">
</p>

- Breads: sourdough, focaccia (olive oil), pizza (olive oil), enriched (butter, eggs, sugar)
- Start from dough weight or flour weight, any number of loaves
- Hydration, starter, starter hydration, salt and oil
- Flour blends for the dough and the starter (7 flour types)
- Ingredients to add, totals and baker's percentages
- Notes, method steps (with a template), saved recipes, share as text
- Baking mode with a tick-off list and built-in step timers: start them from each step, from the ⏱ button at the top (also quick timers) or from the "Next step" prompt that appears when you tick a step off. Several can run at once; they keep running when the app is closed and ring like an alarm clock (alarm volume, vibration, full-screen Stop button over the lock screen)
- Automatic, light or dark theme; keep-screen-on; English and Swedish (follows the phone language)

## Download

**[Download the latest APK](https://github.com/jonsolberg/levain/releases/latest/download/Levain.apk)** · [all releases](https://github.com/jonsolberg/levain/releases)

To install the APK, allow your browser or file manager to *install unknown apps* when Android asks.

## Building
Push to GitHub – the workflow builds the APK and publishes `Levain.apk` on the `apk` branch.
Or open the folder in Android Studio and choose *Build → Build APK(s)*.

## Support

Levain is free, open source and has no ads. If you find it useful, you can
[support me on Ko-fi](https://ko-fi.com/jonsolberg) ❤️

[![Support me on Ko-fi](https://img.shields.io/badge/Ko--fi-support%20me-FF5E5B?logo=ko-fi&logoColor=white)](https://ko-fi.com/jonsolberg)

## License

Copyright © 2026 Jon Solberg

This program is free software: you can redistribute it and/or modify it under the terms of the
GNU General Public License as published by the Free Software Foundation, either version 3 of the
License, or (at your option) any later version.

This program is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
[GNU General Public License](LICENSE) for more details.
