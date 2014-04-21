@echo off
python ..\closure\closure\bin\build\depswriter.py --root_with_prefix=". ../../../visualizer" > deps.js
python ..\closure\closure\bin\build\closurebuilder.py --root=. --root=..\closure --namespace="monoid.main" --output_mode=compiled --compiler_jar=..\closure\compiler.jar -f --warning_level=VERBOSE -f --externs=extern.js -f --js=..\closure\closure\goog\deps.js > main-compiled.js