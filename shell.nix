# This is intended to be used with nix-shell.
# It provides a dev environment, in which gradle and 'gradle runClient' work.

with import <nixpkgs> {};


with xlibs; stdenv.mkDerivation {
  name = "devenv";

  buildInputs = [ gradle jdk ];

  shellHook = ''
    export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:${stdenv.cc.cc}/lib:${libX11}/lib/:${libXext}/lib/:${libXcursor}/lib/:${libXrandr}/lib/:${libXxf86vm}/lib/:${mesa}/lib/:${openal}/lib/
  '';
}
