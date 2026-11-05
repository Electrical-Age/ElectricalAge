{
  description = "Electrical Age (Minecraft 26.3 NeoForge port): build, tests and icon tools";

  inputs.nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";

  outputs = { self, nixpkgs }:
    let
      systems = [ "x86_64-linux" "aarch64-linux" "x86_64-darwin" "aarch64-darwin" ];
      forAll = f: nixpkgs.lib.genAttrs systems (system: f nixpkgs.legacyPackages.${system});
    in {
      # The computer's ROM images (rom/): `nix build .#roms`, then copy result/*.rom to
      # src/main/resources/eln/computer/rom/. Janet is pinned here, not taken from nixpkgs.
      packages = forAll (pkgs: {
        roms = pkgs.stdenv.mkDerivation {
          pname = "eln-computer-roms";
          version = "1";
          src = ./rom;
          janet = pkgs.fetchFromGitHub {
            owner = "janet-lang";
            repo = "janet";
            rev = "v1.41.2";
            hash = "sha256-sNRhcGG8JysmPHHXeRkYCt7qA75U6flptUEWWun+rDs=";
          };
          nativeBuildInputs = [ pkgs.pkgsCross.riscv32-embedded.buildPackages.gcc pkgs.mtools ];
          hardeningDisable = [ "all" ];
          buildPhase = "make JANET_SRC=$janet";
          installPhase = "mkdir -p $out && cp build/*.rom build/*.img $out/";
        };
      });

      devShells = forAll (pkgs:
        let
          # Gradle runs on JAVA_HOME (25, the mod's toolchain); NeoForge's data and game-test runs also
          # want a 21 toolchain. gradle.properties picks both up via org.gradle.java.installations.fromEnv.
          java = {
            JAVA_HOME = pkgs.jdk25.home;
            JDK25 = pkgs.jdk25.home;
            JDK21 = pkgs.jdk21.home;
          };
          # tools/icons: icon builds, contact sheets, manifest
          python = pkgs.python3.withPackages (ps: [ ps.pillow ps.numpy ]);
          # runClient on NixOS: the native libraries LWJGL loads at runtime
          clientLibs = pkgs.lib.optionals pkgs.stdenv.hostPlatform.isLinux (with pkgs; [
            libGL glfw openal alsa-lib libpulseaudio udev flite
            libX11 libXext libXcursor libXrandr libXxf86vm libXi libxkbcommon wayland
          ]);
          shell = extra: pkgs.mkShell (java // {
            packages = [ pkgs.jdk25 pkgs.jdk21 python pkgs.git pkgs.unzip ] ++ extra;
            LD_LIBRARY_PATH = pkgs.lib.makeLibraryPath clientLibs;
          });
        in {
          default = shell [ ];
          # tools/icons/tools/render_ref.py: model reference renders (Blender is large, so not in the default shell)
          refs = shell [ pkgs.blender ];
        });
    };
}
