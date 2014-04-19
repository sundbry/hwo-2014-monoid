@echo off
python ..\closure\closure\bin\build\depswriter.py --root_with_prefix=". ../../../visualizer" > deps.js
python ..\closure\closure\bin\build\closurebuilder.py --root=. --root=..\closure --namespace="monoid.main" --output_mode=compiled --compiler_jar=..\closure\compiler.jar > main-compiled.js