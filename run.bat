@echo off
REM Compilar
javac -encoding UTF-8 -d bin -cp "lib\aedlib.jar;lib\tslib.jar;src" src\Procesador.java
if errorlevel 1 (
    echo Error al compilar
    pause
    exit /b 1 REM /b para salir del script sin cerrar la ventana
)

REM Ejecutar
java -cp "lib\aedlib.jar;lib\tslib.jar;bin" Procesador %*

pause
