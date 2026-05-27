@echo off

SET SRC_DIR=src\main\java
SET OUT_DIR=out\classes
SET MAIN_CLASS=com.autoparts.Main
SET JAR_NAME=AutoPartsIMS.jar

REM -- Auto-detect the connector JAR in lib\ ------------------
SET CONNECTOR=
FOR %%F IN (lib\*.jar) DO SET CONNECTOR=%%F

IF "%CONNECTOR%"=="" (
    echo.
    echo ERROR: No .jar file found in the lib\ folder.
    echo Please place the MySQL Connector JAR inside the lib\ folder.
    echo.
    pause
    exit /b 1
)

echo Found connector: %CONNECTOR%
echo.

IF NOT EXIST %OUT_DIR% mkdir %OUT_DIR%

echo Compiling...
javac -encoding UTF-8 -cp "%CONNECTOR%" -d %OUT_DIR% ^
    %SRC_DIR%\com\autoparts\Main.java ^
    %SRC_DIR%\com\autoparts\db\DBConnection.java ^
    %SRC_DIR%\com\autoparts\db\InventoryDAO.java ^
    %SRC_DIR%\com\autoparts\auth\AuthService.java ^
    %SRC_DIR%\com\autoparts\model\User.java ^
    %SRC_DIR%\com\autoparts\gui\UIHelper.java ^
    %SRC_DIR%\com\autoparts\gui\DashboardPanel.java ^
    %SRC_DIR%\com\autoparts\gui\LoginFrame.java ^
    %SRC_DIR%\com\autoparts\gui\MainFrame.java ^
    %SRC_DIR%\com\autoparts\gui\InventoryPanel.java ^
    %SRC_DIR%\com\autoparts\gui\UsagePanel.java ^
    %SRC_DIR%\com\autoparts\gui\PartsPanel.java ^
    %SRC_DIR%\com\autoparts\gui\RequestsPanel.java ^
    %SRC_DIR%\com\autoparts\gui\SuppliersPanel.java ^
    %SRC_DIR%\com\autoparts\gui\OrderLogPanel.java

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo Compilation FAILED.
    pause
    exit /b 1
)

echo Compilation successful.
echo.

echo Main-Class: %MAIN_CLASS%> manifest.txt
echo Class-Path: %CONNECTOR%>> manifest.txt
echo.>> manifest.txt

echo Building JAR...
jar cfm %JAR_NAME% manifest.txt -C %OUT_DIR% .

IF %ERRORLEVEL% NEQ 0 (
    echo.
    echo JAR creation FAILED.
    pause
    exit /b 1
)

echo JAR created: %JAR_NAME%
echo.

echo Starting AutoParts IMS...
java -cp "%JAR_NAME%;%CONNECTOR%" %MAIN_CLASS%