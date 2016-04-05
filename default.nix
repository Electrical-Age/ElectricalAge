# This is intended to be used with nix-shell.
# It provides a dev environment, in which gradle and 'gradle runClient' work.

with import <nixpkgs> {};

let
  devenvSetup = with xlibs; stdenv.mkDerivation {
    name = "devenvSetup";

    phases = "installPhase";

    installPhase = ''
      mkdir -p $out/nix-support

      cat > $out/nix-support/setup-hook <<EOF
        export LD_LIBRARY_PATH=\$LD_LIBRARY_PATH:${libX11}/lib/:${libXext}/lib/:${libXcursor}/lib/:${libXrandr}/lib/:${libXxf86vm}/lib/:${mesa}/lib/:${openal}/lib/
      EOF
    '';
  };
in

{
  devenv = stdenv.mkDerivation {
    name = "devenv";

    buildInputs = [ gradle jdk devenvSetup ];
  };

}
