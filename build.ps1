$ErrorActionPreference = "Stop"
if (Test-Path ".\gradlew.bat") {
    .\gradlew.bat clean shadowJar
    exit $LASTEXITCODE
}
if (-not (Get-Command gradle -ErrorAction SilentlyContinue)) {
    throw "Gradle не найден в PATH. В IntelliJ IDEA открой папку как Gradle project или установи Gradle 8.5+."
}
gradle clean shadowJar
