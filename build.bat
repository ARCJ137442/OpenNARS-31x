@echo off
REM Very simple build script

@REM 重置临时目录
rm -rf classes
mkdir classes

@REM * 2024-04-19 11:25:38 构建参考：https://www.baeldung.com/javac-compile-classes-directory/
@REM ↓使用 /A:-D 忽略目录 | 参考自 `dir --help`
dir /b /s /A:-D *.java > sources.txt
@REM ↑这是所有Java代码，可能编译顺序不能保证，但实际上无需顾虑顺序（编译器会自动做依赖分析）
javac -d classes @sources.txt -Xstdout compile.log

@REM ==后续其它构建代码==
@REM 1 构建NARS代码（classes）到jar文件（GUI）
echo Main-Class: nars.gui.main > manifest.txt
jar -cvfm opennars-312-gui.jar manifest.txt -C classes .
DEL manifest.txt

@REM 2 构建NARS代码（classes）到jar文件（Shell）
echo Main-Class: nars.main.Shell > manifest.txt
jar -cvfm opennars-312-shell.jar manifest.txt -C classes .
DEL manifest.txt

@REM 2 构建NARS代码（classes）到jar文件（Batch）
echo Main-Class: nars.main.NARSBatch > manifest.txt
jar -cvfm opennars-312-batch.jar manifest.txt -C classes .
DEL manifest.txt

@REM 后续信息
echo You can now launch:
echo java -jar opennars-312-gui.jar
echo or
echo java -jar opennars-312-gui.jar nars-dist/Examples/Example-NAL1-edited.txt --silence 90
echo or
echo java -jar opennars-312-shell.jar
echo or
echo java -jar opennars-312-batch.jar nars-dist/Examples/Example-NAL1-edited.txt
