{
  description = "Launcher3 QuickLaunch - LSPosed module dev environment";

  inputs = {
    nixpkgs.url = "github:NixOS/nixpkgs/nixos-unstable";
  };

  outputs = { self, nixpkgs }:
    let
      system = "x86_64-linux";
      pkgs = import nixpkgs {
        inherit system;
        config = {
          allowUnfree = true;
          android_sdk.accept_license = true;
        };
      };

      androidComposition = pkgs.androidenv.composeAndroidPackages {
        platformVersions = [ "35" "36" ];
        buildToolsVersions = [ "36.0.0" ];
        includeEmulator = false;
        includeSystemImages = false;
        includeSources = false;
        includeNDK = false;
      };

      androidSdk = androidComposition.androidsdk;
    in
    {
      devShells.${system}.default = pkgs.mkShell {
        buildInputs = with pkgs; [
          android-studio
          jdk21
          androidSdk
        ];

        ANDROID_HOME = "${androidSdk}/libexec/android-sdk";
        ANDROID_SDK_ROOT = "${androidSdk}/libexec/android-sdk";
        JAVA_HOME = "${pkgs.jdk21}";

        shellHook = ''
          echo "🚀 Launcher3 QuickLaunch dev environment"
          echo "   ANDROID_HOME=$ANDROID_HOME"
          echo "   JAVA_HOME=$JAVA_HOME"
          
          # Auto-configure local.properties for Android Gradle Plugin
          echo "sdk.dir=$ANDROID_HOME" > local.properties
          
          # Fix dynamically linked aapt2 error by pointing to Nix store's aapt2 in gradle.properties
          sed -i '/^android.aapt2FromMavenOverride=/d' gradle.properties || true
          echo "android.aapt2FromMavenOverride=$ANDROID_HOME/build-tools/36.0.0/aapt2" >> gradle.properties
        '';
      };
    };
}
