# Script para compilar el videojuego de JMonkey y empaquetarlo en un ejecutable .exe

$ErrorActionPreference = "Stop"

$jdkBinPath = "C:\Program Files\Java\jdk-21.0.10\bin"
$javacPath = Join-Path $jdkBinPath "javac.exe"
$jarPath = Join-Path $jdkBinPath "jar.exe"
$jpackagePath = Join-Path $jdkBinPath "jpackage.exe"

Write-Host "=== 1. Limpiando directorios anteriores ===" -ForegroundColor Cyan
Remove-Item -Recurse -Force build_temp, dist, output_exe -ErrorAction SilentlyContinue

Write-Host "=== 2. Creando directorios del build ===" -ForegroundColor Cyan
New-Item -ItemType Directory -Path build_temp | Out-Null
New-Item -ItemType Directory -Path dist | Out-Null
New-Item -ItemType Directory -Path dist/lib | Out-Null

Write-Host "=== 3. Compilando clases Java de la carpeta src ===" -ForegroundColor Cyan
& $javacPath -encoding UTF-8 -cp "C:\Program Files\jMonkeyEngine SDK\jmonkeyplatform\libs\*" -d build_temp src/mygame/*.java
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo en la compilación de las clases Java."
}

Write-Host "=== 4. Creando assets.jar a partir de los recursos ===" -ForegroundColor Cyan
& $jarPath cf dist/lib/assets.jar -C assets .
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo al empaquetar los assets."
}

Write-Host "=== 5. Copiando dependencias del SDK ===" -ForegroundColor Cyan
$sdkLibsPath = "C:\Program Files\jMonkeyEngine SDK\jmonkeyplatform\libs"
Get-ChildItem -Path $sdkLibsPath -Filter "*.jar" | Where-Object { 
    $_.Name -notmatch "sources" -and $_.Name -notmatch "javadoc" 
} | ForEach-Object {
    Copy-Item -Path $_.FullName -Destination "dist/lib"
}

Write-Host "=== 6. Generando archivo de Manifiesto para el JAR principal ===" -ForegroundColor Cyan
$libs = Get-ChildItem -Path "dist/lib" -Filter "*.jar" | ForEach-Object { "lib/$($_.Name)" }
$classPathList = $libs -join " "

# Formatear el Manifest con Class-Path de acuerdo con las especificaciones de longitud de línea (máx 72 chars)
$manifestLines = @(
    "Manifest-Version: 1.0",
    "Main-Class: mygame.Main"
)

$cpHeader = "Class-Path: "
$currentLine = $cpHeader
$parts = $classPathList -split " "

foreach ($part in $parts) {
    if (($currentLine + " " + $part).Length -gt 70) {
        $manifestLines += $currentLine
        $currentLine = "  " + $part # las líneas consecutivas llevan 2 espacios de sangría
    } else {
        if ($currentLine -eq $cpHeader) {
            $currentLine += $part
        } else {
            $currentLine += " " + $part
        }
    }
}
$manifestLines += $currentLine

# Escribir el manifest.txt usando ASCII para evitar la marca de orden de bytes (BOM) de UTF-8
[System.IO.File]::WriteAllLines("build_temp/manifest.txt", $manifestLines, [System.Text.Encoding]::ASCII)

Write-Host "=== 7. Creando MyGame.jar principal ===" -ForegroundColor Cyan
& $jarPath cfm dist/MyGame.jar build_temp/manifest.txt -C build_temp mygame
if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo al crear MyGame.jar."
}

Write-Host "=== 8. Generando el ejecutable .exe con jpackage ===" -ForegroundColor Cyan
& $jpackagePath --type app-image `
                --dest output_exe `
                --name "Andy-Descent-Into-Madness" `
                --input dist `
                --main-jar MyGame.jar `
                --main-class mygame.Main

if ($LASTEXITCODE -ne 0) {
    Write-Error "Fallo al generar el ejecutable .exe con jpackage."
}

Write-Host "==========================================" -ForegroundColor Green
Write-Host "¡Proceso finalizado con éxito!" -ForegroundColor Green
Write-Host "El ejecutable se encuentra en: output_exe\Andy-Descent-Into-Madness\Andy-Descent-Into-Madness.exe" -ForegroundColor Green
Write-Host "==========================================" -ForegroundColor Green
