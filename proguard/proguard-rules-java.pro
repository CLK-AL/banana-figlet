# ProGuard rules for banana-figlet legacy Java classes
# banana-figlet is a library — no main() entry points.
# Keep public API surface for consumers.

-keep class io.leego.banana.BananaUtils { *; }
-keep class io.leego.banana.Font { *; }
-keep class io.leego.banana.Ansi { *; }
-keep class io.leego.banana.Option { *; }
-keep class io.leego.banana.Layout { *; }
