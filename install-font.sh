mkdir -p /usr/share/fonts/truetype
cp open-sans-condensed.light.ttf /usr/share/fonts/truetype
cd /usr/share/fonts/truetype
mkfontscale
mkfontdir
fc-cache
xset fp rehash

