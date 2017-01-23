# This is intended to be used with nix-shell.
# It provides a dev environment, in which gradle and 'gradle runClient' work.

with import <nixpkgs> {};


with xlibs; stdenv.mkDerivation {
  name = "devenv";

  buildInputs = [ gradle jdk ];

  libraries = stdenv.lib.makeLibraryPath [
    stdenv.cc.cc libX11 libXext libXcursor libXrandr libXxf86vm mesa openal libpulseaudio
  ];

  shellHook = ''
    export LD_LIBRARY_PATH=$LD_LIBRARY_PATH:$libraries
  '';
}
